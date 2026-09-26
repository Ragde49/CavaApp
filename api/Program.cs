using Dapper;
using Microsoft.Data.SqlClient;

var builder = WebApplication.CreateBuilder(args);
builder.Services.AddProblemDetails();
var app = builder.Build();

const string wineSql = """
SELECT V.VinoID,V.Productor,V.Nombre,V.Anada,V.Pais,V.Region,V.Denominacion,V.UvaPrincipal,V.Mezcla,
V.AlcoholPorcentaje,V.CrianzaMeses,V.CrianzaRecipiente,V.TemperaturaServicioC,V.TieneRadarEtiqueta,
V.NotasTecnicas,V.NivelDatosTecnicos,V.FuenteTecnica,
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
        return Results.Problem("No se pudo conectar a Cava",detail:ex.Message,statusCode:503);
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
    if(new[]{r.Intensidad,r.Grasa,r.Acidez,r.Dulzor,r.Umami,r.Tostado}.Any(x=>x<1||x>5))
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
    var wi=w.Intensidad is null?(w.Cuerpo is null?null:(double?)w.Cuerpo):(double?)w.Intensidad;
    if(wi is not null){sum+=(1-Math.Clamp(Math.Abs(wi.Value-f.Intensidad)/4,0,1))*.4;weight+=.4;}
    var vals=new[]{w.Tanino,w.Acidez}.Where(x=>x is not null).Select(x=>(double)x!.Value).ToList();
    if(vals.Count>0){var s=vals.Average()/5;var need=f.Grasa/5;sum+=(1-Math.Clamp(Math.Abs(s-need),0,1))*.35;weight+=.35;}
    if(w.Madera is not null){sum+=(1-Math.Clamp(Math.Abs((double)w.Madera.Value-f.Tostado)/4,0,1))*.15;weight+=.15;}
    sum+=((w.ConfianzaPerfil??50)/100.0)*.10;weight+=.10;
    return (int)Math.Round(Math.Clamp(sum/weight*100,0,100));
}
static string Explain(WineDto w,PairingRequest f){
    var p=new List<string>();
    var i=w.Intensidad is null?(w.Cuerpo is null?null:(double?)w.Cuerpo):(double?)w.Intensidad;
    if(i is not null)p.Add(Math.Abs(i.Value-f.Intensidad)<=.75?"intensidad muy compatible":"intensidad compatible");
    if(f.Grasa>=4 && ((w.Tanino??0)+(w.Acidez??0))>=6)p.Add("tanino/acidez ayudan con la grasa");
    if(f.Tostado>=3.5 && (w.Madera??0)>=3)p.Add("la crianza acompaña el tostado");
    if(p.Count==0)p.Add("perfil incompleto; recomendación con menor evidencia");
    return string.Join("; ",p)+".";
}

public sealed record WineDto(int VinoID,string? Productor,string Nombre,short? Anada,string? Pais,string? Region,string? Denominacion,string? UvaPrincipal,string? Mezcla,decimal? AlcoholPorcentaje,short? CrianzaMeses,string? CrianzaRecipiente,decimal? TemperaturaServicioC,bool TieneRadarEtiqueta,string? NotasTecnicas,string? NivelDatosTecnicos,string? FuenteTecnica,decimal? Cuerpo,decimal? Tanino,decimal? Acidez,decimal? Dulzor,decimal? Intensidad,decimal? Fruta,decimal? Madera,string? TipoPerfil,byte? ConfianzaPerfil,string? FuentePerfil,string? NotasPerfil);
public sealed record PairingRequest(double Intensidad,double Grasa,double Acidez,double Dulzor,double Umami,double Tostado,int Top=5);
public sealed record PairingResult(WineDto Wine,int Score,string Reason);
