package net.softcame.cavaapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Wine = Color(0xFF7B1E3A)
private val WineDark = Color(0xFF4E1025)
private val Cream = Color(0xFFFFF8F6)
private val Gold = Color(0xFFB78727)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CavaTheme { CavaApp(applicationContext) } }
    }
}

@Composable
fun CavaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Wine, onPrimary = Color.White,
            primaryContainer = Color(0xFFFFD9E2), onPrimaryContainer = WineDark,
            secondary = Gold, background = Cream, surface = Cream,
            surfaceVariant = Color(0xFFF6EDEE)
        ),
        content = content
    )
}

data class WineItem(
    val id:Int, val producer:String?, val name:String, val vintage:Int?, val country:String?,
    val region:String?, val grape:String?, val blend:String?, val alcohol:Double?, val oakMonths:Int?,
    val body:Double?, val tannin:Double?, val acidity:Double?, val sweetness:Double?,
    val intensity:Double?, val fruit:Double?, val oak:Double?, val profileType:String,
    val confidence:Int, val local:Boolean=false
)

private val seedWines = listOf(
WineItem(1,"Bodegas López Morenas","Aleia Lar de Oro Crianza",2019,"España","Extremadura","Tempranillo",null,14.0,6,null,3.0,null,null,4.0,4.0,3.0,"DERIVADO",75),
WineItem(2,"Bodegas Alceño","Será Garnacha Tintorera",2024,"España","Jumilla","Garnacha Tintorera",null,13.0,null,3.0,3.0,4.0,1.0,3.5,4.0,1.0,"DERIVADO",90),
WineItem(3,"Bodegas Alceño","Será Tempranillo",2024,"España","Jumilla","Tempranillo",null,13.0,null,3.0,2.0,3.5,1.0,3.0,4.0,1.0,"DERIVADO",90),
WineItem(4,"Viña del Nuevo Mundo","Casa La Carlina Carménère",2024,"Chile",null,"Carménère",null,13.5,null,2.5,2.0,3.0,1.0,3.0,4.0,1.0,"DERIVADO",95),
WineItem(5,"Viña del Nuevo Mundo","Casa La Carlina Cabernet Sauvignon",2024,"Chile",null,"Cabernet Sauvignon",null,13.5,null,3.0,4.0,3.5,1.0,4.0,4.0,1.0,"DERIVADO",95),
WineItem(6,"Ségur Estates","Sobrados Solo de Xisto",2024,"Portugal","Alentejo","Aragonez","40% Aragonez / 40% Alicante Bouschet / 20% Syrah",13.0,null,3.0,3.0,4.5,1.0,4.0,4.5,1.0,"DERIVADO",95),
WineItem(7,"Ségur Estates","Sobrados Solo de Argila",2023,"Portugal","Alentejo","Aragonez","40% Aragonez / 30% Trincadeira / 30% Castelão",14.0,null,3.0,2.0,3.5,1.0,3.5,4.0,1.0,"DERIVADO",95),
WineItem(8,"Cantine Ermes","Baglio Cumale Negroamaro",2024,"Italia","Puglia","Negroamaro",null,13.0,null,3.5,2.0,3.5,null,4.0,4.0,null,"DERIVADO",70),
WineItem(9,"Cantine Ermes","Baglio Cumale Merlot",2024,"Italia","Veneto","Merlot",null,13.0,null,3.0,2.0,null,null,3.5,4.0,null,"DERIVADO",70),
WineItem(10,"Union des Vignerons des Côtes du Rhône","Le Gourmandin Vin de France",null,"Francia",null,null,"Cabernet Sauvignon / Garnacha / Merlot / Syrah",13.0,6,3.5,null,null,null,3.5,4.0,1.0,"DERIVADO",75),
WineItem(11,"Union des Vignerons des Côtes du Rhône","Le Gourmandin IGP Méditerranée",null,"Francia","Méditerranée","Merlot","Merlot / Grenache / Syrah",13.5,6,3.0,2.0,3.5,null,3.5,4.0,1.0,"DERIVADO",80),
WineItem(12,"Tagua Tagua","Magé Pequeñas Producciones Cabernet Sauvignon",2023,"Chile","Valle del Rapel","Cabernet Sauvignon",null,13.5,6,3.0,4.0,3.5,null,4.0,3.5,3.0,"DERIVADO",80),
WineItem(13,"Vinos LT","Uzarovita Nebbiolo Cabernet",2017,"México","Baja California","Nebbiolo","Nebbiolo / Cabernet Sauvignon",13.0,12,4.0,4.0,3.5,1.0,4.0,3.5,4.0,"ESTIMADO",55),
WineItem(14,"Vinos LT","Piedra de Luna Selección Tinto",2017,"México","Baja California",null,"Cabernet Sauvignon / Merlot / Tempranillo / Sangiovese / Syrah",null,16,4.0,4.0,3.5,1.0,4.0,3.5,4.5,"ESTIMADO",60),
WineItem(15,"Vinos LT","Ojo de Halcón Nebbiolo Syrah",2016,"México","Valle de Guadalupe","Nebbiolo","50% Nebbiolo / 50% Syrah",13.0,13,3.0,3.0,null,null,4.0,4.0,4.5,"DERIVADO",90),
WineItem(16,"Montes Toscanini","Criado en Roble Tannat",2017,"Uruguay","Canelones","Tannat",null,14.0,15,5.0,4.0,3.0,1.0,4.5,4.0,4.0,"DERIVADO",95),
WineItem(17,"Beronia","Crianza Edición Limitada",2016,"España","Rioja","Tempranillo","100% Tempranillo",null,12,3.5,2.5,3.5,1.0,4.0,4.0,4.0,"DERIVADO",80),
WineItem(18,null,"Vinia Le Fruité",null,"Francia","Pays d'Oc","Carignan","Carignan / Syrah / Grenache",null,null,2.5,2.5,3.5,1.0,3.0,4.5,1.0,"ESTIMADO",55),
WineItem(19,"Montes Toscanini","Corte Supremo Premium",null,"Uruguay","Canelones","Cabernet Sauvignon","Cabernet Sauvignon / Tannat / Merlot",null,16,4.5,4.0,3.0,1.0,4.5,3.5,4.5,"DERIVADO",80),
WineItem(20,"Bodegas Protos","Protos Crianza",2016,"España","Ribera del Duero","Tempranillo","100% Tinta del País / Tempranillo",null,14,4.0,null,null,null,4.5,4.0,4.0,"DERIVADO",90),
WineItem(21,"Alximia","HIN Merlot",2018,"México","Valle de Guadalupe","Merlot","100% Merlot",13.7,18,4.0,3.0,3.0,1.0,4.0,3.5,4.5,"ESTIMADO",55),
WineItem(22,"Montes Toscanini","Cabernet Sauvignon Premium",null,"Uruguay","Canelones","Cabernet Sauvignon","100% Cabernet Sauvignon",null,16,4.0,4.0,3.5,1.0,4.5,3.5,4.5,"ESTIMADO",55)
)

enum class Screen { HOME, CAVA, ADD, DETAIL }
data class FoodProfile(val intensity:Double,val fat:Double,val acidity:Double,val sweetness:Double,val umami:Double,val toast:Double)

@Composable
fun CavaApp(context: Context) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var selected by remember { mutableStateOf<WineItem?>(null) }
    var added by remember { mutableStateOf(loadLocalWines(context)) }
    val wines = seedWines + added
    Scaffold(
        containerColor = Cream,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(screen==Screen.HOME,{screen=Screen.HOME},{Icon(Icons.Default.Restaurant,null)},label={Text("Maridar")})
                NavigationBarItem(screen==Screen.CAVA,{screen=Screen.CAVA},{Icon(Icons.Default.WineBar,null)},label={Text("Mi cava")})
                NavigationBarItem(screen==Screen.ADD,{screen=Screen.ADD},{Icon(Icons.Default.AddCircle,null)},label={Text("Etiqueta")})
            }
        }
    ) { p ->
        AnimatedContent(screen,modifier=Modifier.padding(p),label="screen") { s ->
            when(s){
                Screen.HOME -> HomeScreen(wines){selected=it;screen=Screen.DETAIL}
                Screen.CAVA -> CellarScreen(wines){selected=it;screen=Screen.DETAIL}
                Screen.ADD -> AddWineScreen {
                    val n=it.copy(id=1000+added.size+1,local=true);added=added+n;saveLocalWines(context,added);selected=n;screen=Screen.DETAIL
                }
                Screen.DETAIL -> selected?.let{ WineDetail(it){screen=Screen.CAVA} } ?: HomeScreen(wines){selected=it;screen=Screen.DETAIL}
            }
        }
    }
}

private fun pairingScore(w:WineItem,f:FoodProfile):Int{
    val i=w.intensity?:w.body?:3.0; val t=w.tannin?:2.5; val a=w.acidity?:2.5; val o=w.oak?:2.0
    val m=1.0-(kotlin.math.abs(i-f.intensity)/4.0).coerceIn(0.0,1.0)
    val cut=((t+a)/8.0).coerceIn(0.0,1.0)
    val toast=1.0-(kotlin.math.abs(o-f.toast)/4.0).coerceIn(0.0,1.0)
    return ((m*.38+cut*.37+toast*.15+(w.confidence/100.0)*.10)*100).toInt().coerceIn(0,100)
}

@Composable
private fun HomeScreen(wines:List<WineItem>,open:(WineItem)->Unit){
    val f=FoodProfile(4.5,5.0,1.0,1.0,4.0,4.0)
    val top=wines.map{it to pairingScore(it,f)}.sortedByDescending{it.second}.take(5)
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item{Hero()}
        item{Text("Picaña · ajo · romero",fontSize=30.sp,fontWeight=FontWeight.Black);Text("Grasa 5/5 · tostado 4/5 · intensidad 4.5/5",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        item{ElevatedCard(shape=RoundedCornerShape(30.dp)){Radar(listOf("Intens.","Grasa","Acidez","Dulzor","Umami","Tostado"),listOf(f.intensity,f.fat,f.acidity,f.sweetness,f.umami,f.toast),Modifier.fillMaxWidth().height(270.dp).padding(16.dp),Wine)}}
        item{Text("Mejores de tu cava",fontSize=22.sp,fontWeight=FontWeight.Bold)}
        items(top){x->PairCard(x.first,x.second,open)}
        item{ElevatedCard(shape=RoundedCornerShape(28.dp)){Column(Modifier.padding(20.dp)){Icon(Icons.Default.Lightbulb,null,tint=Gold);Text("Cómo lo calculamos",fontWeight=FontWeight.Bold,fontSize=18.sp);Text("Igualamos intensidad y premiamos acidez/tanino frente a grasa. Los campos sin dato pesan menos; nunca se convierten en cero.")}}}
    }
}

@Composable private fun Hero(){
    Card(colors=CardDefaults.cardColors(containerColor=WineDark),shape=RoundedCornerShape(36.dp)){
        Row(Modifier.fillMaxWidth().padding(24.dp),verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.size(72.dp).background(Color.White.copy(alpha=.12f),CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Default.WineBar,null,tint=Color.White,modifier=Modifier.size(38.dp))}
            Spacer(Modifier.width(16.dp));Column{Text("CAVA",color=Color.White,fontWeight=FontWeight.Black,fontSize=34.sp,letterSpacing=3.sp);Text("Maridaje inteligente, sin adivinar.",color=Color.White.copy(alpha=.85f))}
        }
    }
}

@Composable private fun PairCard(w:WineItem,score:Int,open:(WineItem)->Unit){
    ElevatedCard(Modifier.fillMaxWidth().clickable{open(w)},shape=RoundedCornerShape(28.dp)){
        Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.size(54.dp).background(MaterialTheme.colorScheme.primaryContainer,CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Default.WineBar,null,tint=Wine)}
            Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(w.name,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis);Text(listOfNotNull(w.producer,w.vintage?.toString()).joinToString(" · "),color=MaterialTheme.colorScheme.onSurfaceVariant)}
            Box(Modifier.size(54.dp).background(Wine,CircleShape),contentAlignment=Alignment.Center){Text(score.toString(),color=Color.White,fontWeight=FontWeight.Black)}
        }
    }
}

@Composable private fun CellarScreen(wines:List<WineItem>,open:(WineItem)->Unit){
    var q by remember{mutableStateOf("")}
    val filtered=wines.filter{q.isBlank()||listOfNotNull(it.name,it.producer,it.grape,it.country).any{s->s.contains(q,true)}}
    Column{
        Column(Modifier.padding(20.dp)){Text("Mi cava",fontSize=36.sp,fontWeight=FontWeight.Black);Text(wines.size.toString()+" etiquetas",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),placeholder={Text("Buscar vino, uva, país…")},leadingIcon={Icon(Icons.Default.Search,null)},shape=RoundedCornerShape(24.dp),singleLine=true)}
        LazyColumn(contentPadding=PaddingValues(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            items(filtered){w->ElevatedCard(Modifier.fillMaxWidth().clickable{open(w)},shape=RoundedCornerShape(24.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(48.dp).background(if(w.local)Color(0xFFFFE8A3)else MaterialTheme.colorScheme.primaryContainer,CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Default.WineBar,null,tint=Wine)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(w.name,fontWeight=FontWeight.Bold);Text(listOfNotNull(w.grape,w.country,w.vintage?.toString()).joinToString(" · "),maxLines=1,color=MaterialTheme.colorScheme.onSurfaceVariant)};AssistChip(onClick={open(w)},label={Text(w.profileType.take(3))})}}}
        }
    }
}

@Composable private fun WineDetail(w:WineItem,back:()->Unit){
    val vals=listOf(w.body,w.tannin,w.acidity,w.sweetness,w.intensity,w.fruit,w.oak)
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item{IconButton(onClick=back){Icon(Icons.Default.ArrowBack,null)};Text(w.name,fontSize=30.sp,fontWeight=FontWeight.Black);Text(listOfNotNull(w.producer,w.vintage?.toString(),w.region,w.country).joinToString(" · "),color=MaterialTheme.colorScheme.onSurfaceVariant)}
        item{Card(colors=CardDefaults.cardColors(containerColor=WineDark),shape=RoundedCornerShape(32.dp)){Column(Modifier.padding(20.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Verified,null,tint=Color(0xFFFFD17A));Spacer(Modifier.width(8.dp));Text(w.profileType+" · confianza "+w.confidence.toString()+"%",color=Color.White,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(10.dp));Text(w.grape?:w.blend?:"Mezcla no confirmada",color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold);w.oakMonths?.let{Text(it.toString()+" meses de crianza",color=Color.White.copy(alpha=.8f))};w.alcohol?.let{Text(it.toString()+"% Alc. Vol.",color=Color.White.copy(alpha=.8f))}}}}
        item{ElevatedCard(shape=RoundedCornerShape(30.dp)){Column(Modifier.padding(18.dp)){Text("Perfil sensorial",fontSize=22.sp,fontWeight=FontWeight.Bold);Radar(listOf("Cuerpo","Tanino","Acidez","Dulzor","Intens.","Fruta","Madera"),vals.map{it?:0.0},Modifier.fillMaxWidth().height(300.dp),Wine,vals.map{it==null})}}}
        item{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Metric("Cuerpo",w.body);Metric("Tanino",w.tannin);Metric("Acidez",w.acidity);Metric("Dulzor",w.sweetness);Metric("Intensidad",w.intensity);Metric("Fruta",w.fruit);Metric("Madera",w.oak)}}
    }
}

@Composable private fun Metric(label:String,v:Double?){
    Row(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(20.dp)).padding(14.dp),verticalAlignment=Alignment.CenterVertically){
        Text(label,Modifier.weight(1f),fontWeight=FontWeight.SemiBold)
        if(v==null)Text("Sin dato",color=MaterialTheme.colorScheme.onSurfaceVariant) else {LinearProgressIndicator(progress={ (v/5.0).toFloat() },modifier=Modifier.width(120.dp).height(8.dp),color=Wine,trackColor=Color(0xFFEADDE1));Spacer(Modifier.width(8.dp));Text(String.format("%.1f",v),fontWeight=FontWeight.Bold)}
    }
}

@Composable private fun Radar(labels:List<String>,values:List<Double>,modifier:Modifier,color:Color,missing:List<Boolean> = List(values.size){false}){
    Column(modifier){
        Canvas(Modifier.fillMaxWidth().weight(1f)){
            val n=values.size;val center=Offset(size.width/2,size.height/2);val radius=minOf(size.width,size.height)*.35f
            fun pt(i:Int,r:Float):Offset{val a=(-PI/2+2*PI*i/n).toFloat();return Offset(center.x+cos(a)*r,center.y+sin(a)*r)}
            for(level in 1..5){val p=Path();for(i in 0 until n){val x=pt(i,radius*level/5f);if(i==0)p.moveTo(x.x,x.y)else p.lineTo(x.x,x.y)};p.close();drawPath(p,Color(0xFFD9C8CD),style=Stroke(1f))}
            for(i in 0 until n)drawLine(Color(0xFFD9C8CD),center,pt(i,radius),1f)
            val p=Path();for(i in 0 until n){val x=pt(i,(values[i].coerceIn(0.0,5.0)/5*radius).toFloat());if(i==0)p.moveTo(x.x,x.y)else p.lineTo(x.x,x.y)};p.close();drawPath(p,color.copy(alpha=.20f));drawPath(p,color,style=Stroke(5f))
            for(i in 0 until n)if(!missing[i])drawCircle(color,6f,pt(i,(values[i]/5*radius).toFloat()))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){labels.take(4).forEach{Text(it,fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        if(missing.any{it})Text("• Los ejes sin dato confirmado no se trazan como valor real.",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun AddWineScreen(save:(WineItem)->Unit){
    var name by remember{mutableStateOf("")};var producer by remember{mutableStateOf("")};var vintage by remember{mutableStateOf("")};var grape by remember{mutableStateOf("")};var country by remember{mutableStateOf("")};var body by remember{mutableStateOf("")};var tannin by remember{mutableStateOf("")};var acidity by remember{mutableStateOf("")}
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("Nueva etiqueta",fontSize=36.sp,fontWeight=FontWeight.Black);Text("Captura manual para el MVP. El flujo final usará frente + reverso y validación por API.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        item{Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=RoundedCornerShape(28.dp)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.CameraAlt,null,tint=Wine,modifier=Modifier.size(34.dp));Spacer(Modifier.width(12.dp));Column{Text("Fotos de etiqueta",fontWeight=FontWeight.Bold);Text("Preparado para frente + reverso",fontSize=13.sp)}}}}
        item{Field("Nombre *",name){name=it}};item{Field("Productor",producer){producer=it}};item{Field("Añada",vintage,true){vintage=it}};item{Field("Uva / corte",grape){grape=it}};item{Field("País",country){country=it}}
        item{Text("Perfil opcional 1–5",fontWeight=FontWeight.Bold)};item{Field("Cuerpo",body,true){body=it}};item{Field("Tanino",tannin,true){tannin=it}};item{Field("Acidez",acidity,true){acidity=it}}
        item{Button(enabled=name.isNotBlank(),onClick={save(WineItem(0,producer.ifBlank{null},name,vintage.toIntOrNull(),country.ifBlank{null},null,grape.ifBlank{null},null,null,null,body.toDoubleOrNull()?.coerceIn(1.0,5.0),tannin.toDoubleOrNull()?.coerceIn(1.0,5.0),acidity.toDoubleOrNull()?.coerceIn(1.0,5.0),null,null,null,null,"USUARIO",100,true))},modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(22.dp)){Icon(Icons.Default.Save,null);Spacer(Modifier.width(8.dp));Text("Guardar en mi cava")}}
    }
}

@Composable private fun Field(label:String,value:String,numeric:Boolean=false,change:(String)->Unit){OutlinedTextField(value,change,Modifier.fillMaxWidth(),label={Text(label)},singleLine=true,shape=RoundedCornerShape(20.dp),keyboardOptions=KeyboardOptions(keyboardType=if(numeric)KeyboardType.Decimal else KeyboardType.Text))}

private fun saveLocalWines(c:Context,wines:List<WineItem>){
    val raw=wines.joinToString("\n"){w->listOf(w.name,w.producer?:"",w.vintage?.toString()?:"",w.country?:"",w.grape?:"",w.body?.toString()?:"",w.tannin?.toString()?:"",w.acidity?.toString()?:"").joinToString("|"){it.replace("|","/")}}
    c.getSharedPreferences("cava",Context.MODE_PRIVATE).edit().putString("local_wines",raw).apply()
}
private fun loadLocalWines(c:Context):List<WineItem>{
    val raw=c.getSharedPreferences("cava",Context.MODE_PRIVATE).getString("local_wines","")?:"";if(raw.isBlank())return emptyList()
    return raw.lines().mapIndexedNotNull{i,line->val p=line.split("|");if(p.size<8)null else WineItem(1001+i,p[1].ifBlank{null},p[0],p[2].toIntOrNull(),p[3].ifBlank{null},null,p[4].ifBlank{null},null,null,null,p[5].toDoubleOrNull(),p[6].toDoubleOrNull(),p[7].toDoubleOrNull(),null,null,null,null,"USUARIO",100,true)}
}
