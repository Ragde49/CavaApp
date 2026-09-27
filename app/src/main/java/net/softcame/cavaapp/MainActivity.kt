package net.softcame.cavaapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Wine = Color(0xFF7B1E3A)
private val WineDark = Color(0xFF4E1025)
private val Cream = Color(0xFFFFF8F6)

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
            primary = Wine,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFD9E2),
            onPrimaryContainer = WineDark,
            background = Cream,
            surface = Cream,
            surfaceVariant = Color(0xFFF7ECEF)
        ),
        content = content
    )
}

data class WineItem(
    val id:Int,
    val producer:String?,
    val name:String,
    val vintage:Int?,
    val country:String?,
    val region:String?,
    val grape:String?,
    val blend:String?,
    val alcohol:Double?,
    val oakMonths:Int?,
    val body:Double?,
    val tannin:Double?,
    val acidity:Double?,
    val sweetness:Double?,
    val intensity:Double?,
    val fruit:Double?,
    val oak:Double?,
    val profileType:String?,
    val confidence:Int?
)

data class FoodProfile(
    var dishName:String = "",
    var intensity:Int = 3,
    var fat:Int = 3,
    var acidity:Int = 2,
    var sweetness:Int = 1,
    var spice:Int = 1,
    var umami:Int = 3,
    var toasted:Int = 2,
    var salt:Int = 3
)

data class PairingResult(val wine:WineItem,val score:Int,val reason:String)

class ApiClient(private val baseUrl:String) {
    private fun endpoint(path:String)=baseUrl.trimEnd('/')+path

    suspend fun getWines():List<WineItem> = withContext(Dispatchers.IO) {
        val conn=(URL(endpoint("/api/vinos")).openConnection() as HttpURLConnection).apply {
            requestMethod="GET";connectTimeout=12000;readTimeout=12000
        }
        require(conn.responseCode in 200..299) { "API respondió HTTP "+conn.responseCode }
        parseWineArray(JSONArray(conn.inputStream.bufferedReader().use{it.readText()}))
    }

    suspend fun pair(profile:FoodProfile):List<PairingResult> = withContext(Dispatchers.IO) {
        val conn=(URL(endpoint("/api/maridajes/calcular")).openConnection() as HttpURLConnection).apply {
            requestMethod="POST";connectTimeout=12000;readTimeout=12000;doOutput=true
            setRequestProperty("Content-Type","application/json; charset=utf-8")
        }
        val payload=JSONObject()
            .put("nombrePlato",profile.dishName)
            .put("intensidad",profile.intensity)
            .put("grasa",profile.fat)
            .put("acidez",profile.acidity)
            .put("dulzor",profile.sweetness)
            .put("picante",profile.spice)
            .put("umami",profile.umami)
            .put("tostado",profile.toasted)
            .put("salado",profile.salt)
            .put("top",5)
            .toString()
        conn.outputStream.use{it.write(payload.toByteArray(Charsets.UTF_8))}
        require(conn.responseCode in 200..299) { "API respondió HTTP "+conn.responseCode }
        val arr=JSONArray(conn.inputStream.bufferedReader().use{it.readText()})
        buildList {
            for(i in 0 until arr.length()) {
                val o=arr.getJSONObject(i)
                add(PairingResult(parseWine(o.getJSONObject("wine")),o.getInt("score"),o.optString("reason","")))
            }
        }
    }

    private fun parseWineArray(arr:JSONArray)=buildList {
        for(i in 0 until arr.length()) add(parseWine(arr.getJSONObject(i)))
    }

    private fun parseWine(o:JSONObject)=WineItem(
        id=o.getInt("vinoID"),
        producer=o.optNullableString("productor"),
        name=o.getString("nombre"),
        vintage=o.optNullableInt("anada"),
        country=o.optNullableString("pais"),
        region=o.optNullableString("region"),
        grape=o.optNullableString("uvaPrincipal"),
        blend=o.optNullableString("mezcla"),
        alcohol=o.optNullableDouble("alcoholPorcentaje"),
        oakMonths=o.optNullableInt("crianzaMeses"),
        body=o.optNullableDouble("cuerpo"),
        tannin=o.optNullableDouble("tanino"),
        acidity=o.optNullableDouble("acidez"),
        sweetness=o.optNullableDouble("dulzor"),
        intensity=o.optNullableDouble("intensidad"),
        fruit=o.optNullableDouble("fruta"),
        oak=o.optNullableDouble("madera"),
        profileType=o.optNullableString("tipoPerfil"),
        confidence=o.optNullableInt("confianzaPerfil")
    )
}

private fun JSONObject.optNullableString(key:String):String? =
    if(!has(key)||isNull(key)) null else optString(key).takeIf{it.isNotBlank()}
private fun JSONObject.optNullableInt(key:String):Int? =
    if(!has(key)||isNull(key)) null else optInt(key)
private fun JSONObject.optNullableDouble(key:String):Double? =
    if(!has(key)||isNull(key)) null else optDouble(key)

enum class AppScreen { MENU, PAIRING, CAVA, SETTINGS }

@Composable
fun CavaApp(context:Context) {
    val prefs=remember{context.getSharedPreferences("cava",Context.MODE_PRIVATE)}
    var apiUrl by remember{mutableStateOf(prefs.getString("api_url","")?:"")}
    var screen by remember{mutableStateOf(AppScreen.MENU)}
    var wines by remember{mutableStateOf<List<WineItem>>(emptyList())}
    var selected by remember{mutableStateOf<WineItem?>(null)}
    var detailReturn by remember{mutableStateOf(AppScreen.CAVA)}
    var loading by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()

    fun refreshWines() {
        if(apiUrl.isBlank()) return
        scope.launch {
            loading=true;error=null
            try { wines=ApiClient(apiUrl).getWines() }
            catch(e:Exception){error=e.message?:"Error de conexión"}
            finally{loading=false}
        }
    }

    fun openWine(w:WineItem, from:AppScreen) {
        detailReturn=from
        selected=w
    }

    LaunchedEffect(apiUrl){ if(apiUrl.isNotBlank()) refreshWines() }

    Scaffold(
        containerColor=Cream,
        bottomBar={
            if(selected==null && screen!=AppScreen.SETTINGS) {
                NavigationBar(containerColor=Color.White) {
                    NavigationBarItem(screen==AppScreen.MENU,{screen=AppScreen.MENU},{Icon(Icons.Default.Home,null)},label={Text("Inicio")})
                    NavigationBarItem(screen==AppScreen.CAVA,{screen=AppScreen.CAVA},{Icon(Icons.Default.WineBar,null)},label={Text("Mi cava")})
                    NavigationBarItem(screen==AppScreen.PAIRING,{screen=AppScreen.PAIRING},{Icon(Icons.Default.Restaurant,null)},label={Text("Maridaje")})
                }
            }
        }
    ){p->
        Box(Modifier.padding(p)) {
            when {
                selected!=null -> WineDetail(selected!!){
                    selected=null
                    screen=detailReturn
                }
                screen==AppScreen.MENU -> MenuScreen(
                    wineCount=wines.size,
                    apiConfigured=apiUrl.isNotBlank(),
                    onPairing={screen=AppScreen.PAIRING},
                    onCellar={screen=AppScreen.CAVA},
                    onSettings={screen=AppScreen.SETTINGS}
                )
                screen==AppScreen.PAIRING -> PairingWizard(apiUrl){openWine(it,AppScreen.PAIRING)}
                screen==AppScreen.CAVA -> WinesScreen(wines,loading,error,{openWine(it,AppScreen.CAVA)},{refreshWines()})
                screen==AppScreen.SETTINGS -> ApiSettings(
                    current=apiUrl,
                    onSave={url->
                        apiUrl=url.trim()
                        prefs.edit().putString("api_url",apiUrl).apply()
                        refreshWines()
                        screen=AppScreen.MENU
                    },
                    onRefresh={refreshWines()},
                    onBack={screen=AppScreen.MENU}
                )
            }
        }
    }
}

@Composable
private fun MenuScreen(
    wineCount:Int,
    apiConfigured:Boolean,
    onPairing:()->Unit,
    onCellar:()->Unit,
    onSettings:()->Unit
) {
    LazyColumn(
        contentPadding=PaddingValues(20.dp),
        verticalArrangement=Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CAVA",fontSize=38.sp,fontWeight=FontWeight.Black,letterSpacing=3.sp,color=WineDark)
                    Text("Tu asistente de maridaje",fontSize=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick=onSettings) {
                    Icon(Icons.Default.Settings,"Configurar API",tint=Wine)
                }
            }
        }

        item {
            Card(
                modifier=Modifier.fillMaxWidth().clickable{onPairing()},
                colors=CardDefaults.cardColors(containerColor=WineDark),
                shape=RoundedCornerShape(34.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Box(Modifier.size(62.dp).background(Color.White.copy(alpha=.12f),CircleShape),contentAlignment=Alignment.Center) {
                        Icon(Icons.Default.Restaurant,null,tint=Color.White,modifier=Modifier.size(34.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Maridaje",color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Black)
                    Text("Dime qué vas a comer. Te hago unas preguntas y cruzamos el plato contra los vinos que tienes.",color=Color.White.copy(alpha=.84f),fontSize=16.sp)
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text("Encontrar el mejor vino",color=Color.White,fontWeight=FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowForward,null,tint=Color.White)
                    }
                }
            }
        }

        item {
            ElevatedCard(
                modifier=Modifier.fillMaxWidth().clickable{onCellar()},
                shape=RoundedCornerShape(34.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Box(Modifier.size(62.dp).background(MaterialTheme.colorScheme.primaryContainer,CircleShape),contentAlignment=Alignment.Center) {
                        Icon(Icons.Default.WineBar,null,tint=Wine,modifier=Modifier.size(34.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Mi cava",fontSize=30.sp,fontWeight=FontWeight.Black)
                    Text("Entra directo a tus botellas, busca etiquetas y revisa el perfil y las gráficas de cada vino.",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=16.sp)
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text(if(apiConfigured) "$wineCount vinos disponibles" else "Configura primero la API",fontWeight=FontWeight.Bold,color=Wine)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowForward,null,tint=Wine)
                    }
                }
            }
        }

        if(!apiConfigured) {
            item {
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFFFE8C2)),shape=RoundedCornerShape(24.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning,null,tint=Wine)
                        Spacer(Modifier.width(10.dp))
                        Text("Falta configurar la dirección HTTPS de la API para cargar tu cava y calcular maridajes.",Modifier.weight(1f))
                        TextButton(onClick=onSettings){Text("Configurar")}
                    }
                }
            }
        }
    }
}

private data class Question(val title:String,val help:String,val get:(FoodProfile)->Int,val set:(FoodProfile,Int)->Unit)

private val questions=listOf(
    Question("¿Qué tan intenso es el plato?","Piensa en cuánto domina el sabor general: delicado vs. muy potente.",{it.intensity},{p,v->p.intensity=v}),
    Question("¿Cuánta grasa tiene?","Aceite, mantequilla, crema, piel, cortes grasos o quesos aumentan este valor.",{it.fat},{p,v->p.fat=v}),
    Question("¿Qué tan ácido es?","Limón, vinagre, jitomate, encurtidos y salsas ácidas.",{it.acidity},{p,v->p.acidity=v}),
    Question("¿Qué tan dulce es?","Incluye glaseados, miel, fruta, BBQ dulce o postres.",{it.sweetness},{p,v->p.sweetness=v}),
    Question("¿Qué tan picante es?","Chile, pimienta fuerte o salsas que dejan calor en boca.",{it.spice},{p,v->p.spice=v}),
    Question("¿Qué tanto sabor profundo tiene?","Umami: carne dorada, hongos, quesos curados, soya, fondos y salsas concentradas.",{it.umami},{p,v->p.umami=v}),
    Question("¿Qué tan tostado o ahumado está?","Parrilla, costra, carbón, horno fuerte o ahumado.",{it.toasted},{p,v->p.toasted=v}),
    Question("¿Qué tan salado es?","Desde poco sazonado hasta muy salado o curado.",{it.salt},{p,v->p.salt=v})
)

@Composable
private fun PairingWizard(apiUrl:String,openWine:(WineItem)->Unit) {
    var profile by remember{mutableStateOf(FoodProfile())}
    var step by remember{mutableStateOf(-1)}
    var results by remember{mutableStateOf<List<PairingResult>>(emptyList())}
    var loading by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()

    fun calculate() {
        if(apiUrl.isBlank()) { error="Primero configura la URL de la API.";return }
        scope.launch {
            loading=true;error=null
            try { results=ApiClient(apiUrl).pair(profile);step=questions.size }
            catch(e:Exception){error=e.message?:"No fue posible calcular el maridaje"}
            finally{loading=false}
        }
    }

    if(step==questions.size) {
        PairingResults(profile,results,loading,error,openWine) {
            profile=FoodProfile();results=emptyList();step=-1;error=null
        }
        return
    }

    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {
            Card(colors=CardDefaults.cardColors(containerColor=WineDark),shape=RoundedCornerShape(34.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Icon(Icons.Default.AutoAwesome,null,tint=Color(0xFFFFD48A),modifier=Modifier.size(34.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("¿Qué vas a comer?",color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Black)
                    Text("Te hago unas preguntas y cruzo el plato contra las botellas que tienes.",color=Color.White.copy(alpha=.82f),fontSize=16.sp)
                }
            }
        }

        if(step==-1) {
            item {
                OutlinedTextField(
                    profile.dishName,
                    {profile=profile.copy(dishName=it)},
                    Modifier.fillMaxWidth(),
                    label={Text("Nombre del platillo")},
                    placeholder={Text("Ej. picaña con ajo y romero")},
                    shape=RoundedCornerShape(24.dp),
                    singleLine=true
                )
            }
            item {
                Button(
                    onClick={step=0},
                    enabled=profile.dishName.isNotBlank(),
                    modifier=Modifier.fillMaxWidth().height(58.dp),
                    shape=RoundedCornerShape(22.dp)
                ){Text("Empezar maridaje",fontWeight=FontWeight.Bold);Spacer(Modifier.width(8.dp));Icon(Icons.Default.ArrowForward,null)}
            }
            item {
                ElevatedCard(shape=RoundedCornerShape(26.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("No necesitas saber de vino",fontWeight=FontWeight.Bold,fontSize=18.sp)
                        Text("Solo describe la comida. La API compara intensidad, grasa, acidez, dulzor, picante, umami, tostado y sal con el perfil de cada etiqueta de tu cava.")
                    }
                }
            }
        } else {
            val q=questions[step]
            item {
                Text("Pregunta "+(step+1)+" de "+questions.size,fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress={(step+1f)/questions.size},modifier=Modifier.fillMaxWidth())
            }
            item {
                Text(q.title,fontSize=30.sp,fontWeight=FontWeight.Black)
                Text(q.help,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=16.sp)
            }
            item { ScaleSelector(q.get(profile)){v->val p=profile.copy();q.set(p,v);profile=p} }
            error?.let{item{ErrorCard(it){error=null}}}
            if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
            item {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick={step--},modifier=Modifier.weight(1f).height(54.dp),shape=RoundedCornerShape(20.dp)){Icon(Icons.Default.ArrowBack,null);Spacer(Modifier.width(6.dp));Text("Atrás")}
                    Button(
                        onClick={ if(step==questions.lastIndex) calculate() else step++ },
                        modifier=Modifier.weight(1f).height(54.dp),
                        shape=RoundedCornerShape(20.dp),
                        enabled=!loading
                    ){
                        Text(if(step==questions.lastIndex)"Recomendar" else "Siguiente")
                        Spacer(Modifier.width(6.dp))
                        Icon(if(step==questions.lastIndex)Icons.Default.WineBar else Icons.Default.ArrowForward,null)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScaleSelector(value:Int,onValue:(Int)->Unit) {
    val labels=listOf("Muy bajo","Bajo","Medio","Alto","Muy alto")
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        labels.forEachIndexed{i,label->
            val v=i+1
            val selected=value==v
            Card(
                modifier=Modifier.fillMaxWidth().clickable{onValue(v)},
                colors=CardDefaults.cardColors(containerColor=if(selected)MaterialTheme.colorScheme.primaryContainer else Color.White),
                shape=RoundedCornerShape(22.dp)
            ) {
                Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(if(selected)Wine else Color(0xFFE8DEE1),CircleShape),contentAlignment=Alignment.Center){
                        Text(v.toString(),color=if(selected)Color.White else Wine,fontWeight=FontWeight.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(label,fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium,fontSize=17.sp)
                    Spacer(Modifier.weight(1f))
                    if(selected)Icon(Icons.Default.CheckCircle,null,tint=Wine)
                }
            }
        }
    }
}

@Composable
private fun PairingResults(
    profile:FoodProfile,
    rows:List<PairingResult>,
    loading:Boolean,
    error:String?,
    openWine:(WineItem)->Unit,
    restart:()->Unit
) {
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Text("Para "+profile.dishName,fontSize=30.sp,fontWeight=FontWeight.Black)
            Text("Estas son las botellas de tu cava que mejor encajan con el plato.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            AssistChip(onClick={},label={Text("Intensidad "+profile.intensity+" · Grasa "+profile.fat+" · Acidez "+profile.acidity+" · Dulzor "+profile.sweetness)})
        }
        if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
        error?.let{item{ErrorCard(it){}}}
        items(rows){x->
            ElevatedCard(Modifier.fillMaxWidth().clickable{openWine(x.wine)},shape=RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).background(Wine,CircleShape),contentAlignment=Alignment.Center){
                            Text(x.score.toString(),color=Color.White,fontWeight=FontWeight.Black)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(x.wine.name,fontWeight=FontWeight.Black,fontSize=18.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                            Text(listOfNotNull(x.wine.producer,x.wine.vintage?.toString()).joinToString(" · "),color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(x.reason)
                }
            }
        }
        item {
            OutlinedButton(onClick=restart,modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(20.dp)){
                Icon(Icons.Default.RestartAlt,null);Spacer(Modifier.width(8.dp));Text("Otro platillo")
            }
        }
    }
}

@Composable
private fun WinesScreen(wines:List<WineItem>,loading:Boolean,error:String?,open:(WineItem)->Unit,refresh:()->Unit) {
    var q by remember{mutableStateOf("")}
    val filtered=wines.filter{q.isBlank()||listOfNotNull(it.name,it.producer,it.grape,it.country).any{s->s.contains(q,true)}}
    Column {
        Column(Modifier.padding(20.dp)) {
            Text("Mi cava",fontSize=36.sp,fontWeight=FontWeight.Black)
            Text(wines.size.toString()+" etiquetas disponibles para maridar",color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Buscar vino, uva, país…")},shape=RoundedCornerShape(22.dp),singleLine=true)
            if(loading){Spacer(Modifier.height(8.dp));LinearProgressIndicator(Modifier.fillMaxWidth())}
            error?.let{Spacer(Modifier.height(8.dp));ErrorCard(it,refresh)}
        }
        LazyColumn(contentPadding=PaddingValues(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            items(filtered){w->
                ElevatedCard(Modifier.fillMaxWidth().clickable{open(w)},shape=RoundedCornerShape(24.dp)) {
                    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.WineBar,null,tint=Wine,modifier=Modifier.size(38.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.name,fontWeight=FontWeight.Bold)
                            Text(listOfNotNull(w.grape,w.country,w.vintage?.toString()).joinToString(" · "),maxLines=1,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(w.profileType?:"SIN DATO",fontSize=11.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiSettings(current:String,onSave:(String)->Unit,onRefresh:()->Unit,onBack:()->Unit) {
    var value by remember(current){mutableStateOf(current)}
    Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null)}
        Text("Conexión API",fontSize=36.sp,fontWeight=FontWeight.Black)
        Text("Los vinos y el motor de maridaje viven en tu API de IIS.")
        OutlinedTextField(value,{value=it},Modifier.fillMaxWidth(),label={Text("URL HTTPS de Cava API")},placeholder={Text("https://cavaapi.midominio.com")},shape=RoundedCornerShape(22.dp),singleLine=true)
        Button(onClick={onSave(value)},enabled=value.startsWith("https://"),modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(22.dp)){
            Icon(Icons.Default.Save,null);Spacer(Modifier.width(8.dp));Text("Guardar y sincronizar")
        }
        OutlinedButton(onClick=onRefresh,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(22.dp)){
            Icon(Icons.Default.Refresh,null);Spacer(Modifier.width(8.dp));Text("Probar otra vez")
        }
    }
}

@Composable
private fun WineDetail(w:WineItem,back:()->Unit) {
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item{IconButton(onClick=back){Icon(Icons.Default.ArrowBack,null)}}
        item{Text(w.name,fontSize=30.sp,fontWeight=FontWeight.Black);Text(listOfNotNull(w.producer,w.vintage?.toString(),w.region,w.country).joinToString(" · "))}
        item {
            Card(colors=CardDefaults.cardColors(containerColor=WineDark),shape=RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text((w.profileType?:"SIN DATO")+" · confianza "+(w.confidence?.toString()?:"N/D")+"%",color=Color.White,fontWeight=FontWeight.Bold)
                    Text(w.grape?:w.blend?:"Uva sin confirmar",color=Color.White,fontSize=21.sp)
                    w.alcohol?.let{Text(it.toString()+"% Alc. Vol.",color=Color.White.copy(alpha=.8f))}
                    w.oakMonths?.let{Text(it.toString()+" meses de crianza",color=Color.White.copy(alpha=.8f))}
                }
            }
        }
        item {
            ElevatedCard(shape=RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Gráfica sensorial",fontSize=21.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    WineRadar(
                        values=listOf(w.body,w.tannin,w.acidity,w.sweetness,w.intensity,w.fruit,w.oak),
                        modifier=Modifier.fillMaxWidth().height(280.dp)
                    )
                    Text("Cuerpo · Tanino · Acidez · Dulzor · Intensidad · Fruta · Madera",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item{Metric("Cuerpo",w.body)};item{Metric("Tanino",w.tannin)};item{Metric("Acidez",w.acidity)}
        item{Metric("Dulzor",w.sweetness)};item{Metric("Intensidad",w.intensity)};item{Metric("Fruta",w.fruit)};item{Metric("Madera",w.oak)}
    }
}

@Composable
private fun WineRadar(values:List<Double?>,modifier:Modifier=Modifier) {
    Canvas(modifier) {
        val count=values.size
        val center=Offset(size.width/2f,size.height/2f)
        val radius=minOf(size.width,size.height)*0.36f
        fun point(index:Int,r:Float):Offset {
            val angle=(-PI/2.0+2.0*PI*index/count).toFloat()
            return Offset(center.x+cos(angle)*r,center.y+sin(angle)*r)
        }

        for(level in 1..5) {
            val p=Path()
            for(i in 0 until count) {
                val pt=point(i,radius*level/5f)
                if(i==0)p.moveTo(pt.x,pt.y) else p.lineTo(pt.x,pt.y)
            }
            p.close()
            drawPath(p,Color(0xFFD7C8CC),style=Stroke(1.5f))
        }
        for(i in 0 until count) drawLine(Color(0xFFD7C8CC),center,point(i,radius),1.5f)

        val polygon=Path()
        for(i in 0 until count) {
            val v=(values[i]?:0.0).coerceIn(0.0,5.0)
            val pt=point(i,(radius*(v/5.0)).toFloat())
            if(i==0)polygon.moveTo(pt.x,pt.y) else polygon.lineTo(pt.x,pt.y)
        }
        polygon.close()
        drawPath(polygon,Wine.copy(alpha=.18f))
        drawPath(polygon,Wine,style=Stroke(5f))

        values.forEachIndexed{i,v->
            if(v!=null) drawCircle(Wine,6f,point(i,(radius*(v.coerceIn(0.0,5.0)/5.0)).toFloat()))
        }
    }
}

@Composable
private fun Metric(label:String,v:Double?) {
    Row(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(18.dp)).padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f),fontWeight=FontWeight.SemiBold)
        Text(v?.let{String.format("%.1f",it)}?:"Sin dato",fontWeight=FontWeight.Bold)
    }
}

@Composable
private fun ErrorCard(message:String,retry:()->Unit) {
    Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFFFE2E2)),shape=RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.Warning,null);Spacer(Modifier.width(8.dp))
            Text(message,Modifier.weight(1f),maxLines=3)
            IconButton(onClick=retry){Icon(Icons.Default.Refresh,null)}
        }
    }
}
