using Dapper;
using Microsoft.Data.SqlClient;

var builder = WebApplication.CreateBuilder(args);
builder.Services.AddProblemDetails();
var app = builder.Build();
app.UseStaticFiles();

const string wineSql = """
SELECT V.VinoID,V.Productor,V.Nombre,V.Anada,V.Pais,V.Region,V.Denominacion,V.UvaPrincipal,V.Mezcla,
V.AlcoholPorcentaje,V.CrianzaMeses,V.CrianzaRecipiente,V.TemperaturaServicioC,V.TieneRadarEtiqueta,
V.NotasTecnicas,V.NivelDatosTecnicos,V.FuenteTecnica,V.ImagenUrl,
P.Cuerpo,P.Tanino,P.Acidez,P.Dulzor,P.Intensidad,P.Fruta,P.Madera,P.TipoPerfil,P.ConfianzaPerfil,P.FuentePerfil,P.NotasPerfil
FROM dbo.Vinos V
LEFT JOIN dbo.VinoPerfilSensorial P ON P.VinoID=V.VinoID
WHERE V.Activo=1
""";

SqlConnection Db() {
    var cs=builder.Configuration.GetConnectionString("Cava");
    if(string.IsNullOrWhiteSpace(cs)) throw new InvalidOperationException("Falta ConnectionStrings:Cava.");
    return new SqlConnection(cs);
}

app.MapGet("/",()=>Results.Ok(new{app="Cava API",version="1.0.0"}));

app.MapGet("/api/health",async()=>{
    try {
        await using var db=Db(); await db.OpenAsync();
        var ok=await db.ExecuteScalarAsync<int>("SELECT 1");
        return Results.Ok(new{status=ok==1?"ok":"error",database="Cava"});
    } catch(Exception ex) {
        return Results.Problem(title:"No se pudo conectar a Cava",detail:ex.Message,statusCode:503);
    }
});

app.MapGet("/api/vinos",async()=>{
    await using var db=Db();
    return Results.Ok(await db.QueryAsync<WineDto>(wineSql+" ORDER BY V.Productor,V.Nombre,V.Anada;"));
});

app.MapGet("/api/vinos/{id:int}",async(int id)=>{
    await using var db=Db();
    var row=await db.QuerySingleOrDefaultAsync<WineDto>(wineSql+" AND V.VinoID=@id;",new{id});
    return row is null?Results.NotFound():Results.Ok(row);
});

app.MapPost("/api/maridajes/calcular",async(PairingRequest r)=>{
    if(new[]{r.Intensidad,r.Grasa,r.Acidez,r.Dulzor,r.Picante,r.Umami,r.Tostado,r.Salado}.Any(x=>x<1||x>5))
        return Results.BadRequest(new{error="Todos los valores deben estar entre 1 y 5."});
    await using var db=Db();
    var wines=(await db.QueryAsync<WineDto>(wineSql)).ToList();
    var result=wines.Select(w=>new PairingResult(w,Score(w,r),Explain(w,r)))
                    .OrderByDescending(x=>x.Score).ThenByDescending(x=>x.Wine.ConfianzaPerfil??0)
                    .Take(Math.Clamp(r.Top,1,20));
    return Results.Ok(result);
});

app.Run();

static int Score(WineDto w,PairingRequest f){
    double sum=0,weight=0;

    // 1) Intensidad del plato vs intensidad/cuerpo del vino
    var wi=w.Intensidad is null?(w.Cuerpo is null?null:(double?)w.Cuerpo):(double?)w.Intensidad;
    if(wi is not null){
        var match=1-Math.Clamp(Math.Abs(wi.Value-f.Intensidad)/4,0,1);
        sum+=match*.25; weight+=.25;
    }

    // 2) Grasa: premia tanino y acidez que limpian/estructuran
    var structureVals=new[]{w.Tanino,w.Acidez}.Where(x=>x is not null).Select(x=>(double)x!.Value).ToList();
    if(structureVals.Count>0){
        var structure=structureVals.Average()/5.0;
        var need=f.Grasa/5.0;
        var match=1-Math.Clamp(Math.Abs(structure-need),0,1);
        sum+=match*.20; weight+=.20;
    }

    // 3) Acidez del plato: el vino debe tener acidez suficiente
    if(w.Acidez is not null){
        var wineAcidity=(double)w.Acidez.Value;
        var deficit=Math.Max(0,f.Acidez-wineAcidity);
        var match=1-Math.Clamp(deficit/4.0,0,1);
        sum+=match*.12; weight+=.12;
    }

    // 4) Dulzor: si el plato es dulce, un vino demasiado seco pierde compatibilidad
    if(w.Dulzor is not null){
        var wineSweet=(double)w.Dulzor.Value;
        var deficit=Math.Max(0,f.Dulzor-wineSweet);
        var match=1-Math.Clamp(deficit/4.0,0,1);
        sum+=match*.10; weight+=.10;
    }

    // 5) Picante: alcohol y tanino altos pueden amplificar el calor
    if(w.AlcoholPorcentaje is not null || w.Tanino is not null){
        var alcohol=(double)(w.AlcoholPorcentaje??13m);
        var tannin=(double)(w.Tanino??2.5m);
        var heat=((Math.Max(0,alcohol-12)/4.0)+(tannin/5.0))/2.0;
        var spiceNeed=f.Picante/5.0;
        var match=1-Math.Clamp(spiceNeed*heat,0,1);
        sum+=match*.10; weight+=.10;
    }

    // 6) Umami: tanino muy alto puede sentirse más seco/amargo
    if(w.Tanino is not null){
        var tannin=(double)w.Tanino.Value/5.0;
        var umami=f.Umami/5.0;
        var match=1-Math.Clamp(umami*tannin*.65,0,1);
        sum+=match*.08; weight+=.08;
    }

    // 7) Tostado/ahumado: premia madera/crianza compatible
    if(w.Madera is not null){
        var match=1-Math.Clamp(Math.Abs((double)w.Madera.Value-f.Tostado)/4,0,1);
        sum+=match*.08; weight+=.08;
    }

    // 8) Sal: la sal suele llevarse bien con acidez y puede suavizar tanino
    if(w.Acidez is not null || w.Tanino is not null){
        var acid=(double)(w.Acidez??2.5m)/5.0;
        var tannin=(double)(w.Tanino??2.5m)/5.0;
        var salt=f.Salado/5.0;
        var benefit=Math.Clamp((acid*.6+tannin*.4)*salt,0,1);
        sum+=(0.65+0.35*benefit)*.05; weight+=.05;
    }

    // Confianza del perfil del vino
    sum+=((w.ConfianzaPerfil??50)/100.0)*.02; weight+=.02;

    return weight<=0?0:(int)Math.Round(Math.Clamp(sum/weight*100,0,100));
}
static string Explain(WineDto w,PairingRequest f){
    var p=new List<string>();
    var i=w.Intensidad is null?(w.Cuerpo is null?null:(double?)w.Cuerpo):(double?)w.Intensidad;
    if(i is not null)p.Add(Math.Abs(i.Value-f.Intensidad)<=.75?"intensidad muy compatible":"intensidad compatible");
    if(f.Grasa>=4 && ((w.Tanino??0)+(w.Acidez??0))>=6)p.Add("tanino/acidez ayudan con la grasa");
    if(f.Tostado>=3.5 && (w.Madera??0)>=3)p.Add("la crianza acompaña el tostado");
    if(f.Picante>=4 && (w.AlcoholPorcentaje??0)>=14)p.Add("el alcohol puede sentirse más intenso con picante");
    if(f.Salado>=4 && (w.Tanino??0)>=3)p.Add("la sal puede suavizar la percepción del tanino");
    if(p.Count==0)p.Add("perfil incompleto; recomendación con menor evidencia");
    return string.Join("; ",p)+".";
}

public sealed record WineDto(int VinoID,string? Productor,string Nombre,short? Anada,string? Pais,string? Region,string? Denominacion,string? UvaPrincipal,string? Mezcla,decimal? AlcoholPorcentaje,short? CrianzaMeses,string? CrianzaRecipiente,decimal? TemperaturaServicioC,bool TieneRadarEtiqueta,string? NotasTecnicas,string? NivelDatosTecnicos,string? FuenteTecnica,string? ImagenUrl,decimal? Cuerpo,decimal? Tanino,decimal? Acidez,decimal? Dulzor,decimal? Intensidad,decimal? Fruta,decimal? Madera,string? TipoPerfil,byte? ConfianzaPerfil,string? FuentePerfil,string? NotasPerfil);
public sealed record PairingRequest(string? NombrePlato,double Intensidad,double Grasa,double Acidez,double Dulzor,double Picante,double Umami,double Tostado,double Salado,int Top=5);
public sealed record PairingResult(WineDto Wine,int Score,string Reason);
