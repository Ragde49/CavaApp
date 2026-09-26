package net.softcame.cavaapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            surface = Cream
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

data class PairingResult(val wine: WineItem, val score:Int, val reason:String)

class ApiClient(private val baseUrl:String) {
    private fun endpoint(path:String) = baseUrl.trimEnd('/') + path

    suspend fun getWines():List<WineItem> = withContext(Dispatchers.IO) {
        val conn = (URL(endpoint("/api/vinos")).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        parseWineArray(JSONArray(body))
    }

    suspend fun pairPicanha():List<PairingResult> = withContext(Dispatchers.IO) {
        val conn = (URL(endpoint("/api/maridajes/calcular")).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 12000
            doOutput = true
            setRequestProperty("Content-Type","application/json; charset=utf-8")
        }
        val payload = JSONObject()
            .put("intensidad",4.5)
            .put("grasa",5)
            .put("acidez",1)
            .put("dulzor",1)
            .put("umami",4)
            .put("tostado",4)
            .put("top",5)
            .toString()
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val arr = JSONArray(body)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(PairingResult(parseWine(o.getJSONObject("wine")), o.getInt("score"), o.optString("reason","")))
            }
        }
    }

    private fun parseWineArray(arr:JSONArray):List<WineItem> = buildList {
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
    if (isNull(key) || !has(key)) null else optString(key).takeIf { it.isNotBlank() }

private fun JSONObject.optNullableInt(key:String):Int? =
    if (isNull(key) || !has(key)) null else optInt(key)

private fun JSONObject.optNullableDouble(key:String):Double? =
    if (isNull(key) || !has(key)) null else optDouble(key)

@Composable
fun CavaApp(context:Context) {
    val prefs = remember { context.getSharedPreferences("cava", Context.MODE_PRIVATE) }
    var apiUrl by remember { mutableStateOf(prefs.getString("api_url","") ?: "") }
    var tab by remember { mutableStateOf(0) }
    var wines by remember { mutableStateOf<List<WineItem>>(emptyList()) }
    var pairings by remember { mutableStateOf<List<PairingResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<WineItem?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        if (apiUrl.isBlank()) return
        scope.launch {
            loading = true; error = null
            try {
                val api = ApiClient(apiUrl)
                wines = api.getWines()
                pairings = api.pairPicanha()
            } catch (e:Exception) {
                error = e.message ?: "Error de conexión"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(apiUrl) { if (apiUrl.isNotBlank()) refresh() }

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(tab==0,{tab=0},{Icon(Icons.Default.Restaurant,null)},label={Text("Maridar")})
                NavigationBarItem(tab==1,{tab=1},{Icon(Icons.Default.WineBar,null)},label={Text("Mi cava")})
                NavigationBarItem(tab==2,{tab=2},{Icon(Icons.Default.Settings,null)},label={Text("API")})
            }
        }
    ) { p ->
        Box(Modifier.padding(p)) {
            when {
                selected != null -> WineDetail(selected!!){ selected=null }
                tab==0 -> PairingScreen(pairings,loading,error,{selected=it},{refresh()})
                tab==1 -> WinesScreen(wines,loading,error,{selected=it},{refresh()})
                else -> ApiSettings(apiUrl,
                    onSave = {
                        apiUrl=it.trim()
                        prefs.edit().putString("api_url",apiUrl).apply()
                        refresh()
                    },
                    onRefresh={refresh()}
                )
            }
        }
    }
}

@Composable
private fun PairingScreen(
    rows:List<PairingResult>, loading:Boolean, error:String?,
    open:(WineItem)->Unit, refresh:()->Unit
) {
    LazyColumn(contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Card(colors=CardDefaults.cardColors(containerColor=WineDark),shape=RoundedCornerShape(34.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("CAVA",color=Color.White,fontSize=34.sp,fontWeight=FontWeight.Black,letterSpacing=3.sp)
                    Text("Picaña · ajo · romero",color=Color.White.copy(alpha=.85f),fontSize=18.sp)
                    Text("Todo calculado por la API",color=Color.White.copy(alpha=.7f))
                }
            }
        }
        item {
            Text("Mejores de tu cava",fontSize=26.sp,fontWeight=FontWeight.Black)
            Text("Grasa 5/5 · tostado 4/5 · intensidad 4.5/5")
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { item { ErrorCard(it,refresh) } }
        items(rows) { x ->
            ElevatedCard(Modifier.fillMaxWidth().clickable{open(x.wine)},shape=RoundedCornerShape(26.dp)) {
                Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(54.dp).background(MaterialTheme.colorScheme.primaryContainer,CircleShape),contentAlignment=Alignment.Center) {
                        Icon(Icons.Default.WineBar,null,tint=Wine)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(x.wine.name,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text(x.reason,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
                    }
                    Box(Modifier.size(54.dp).background(Wine,CircleShape),contentAlignment=Alignment.Center) {
                        Text(x.score.toString(),color=Color.White,fontWeight=FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun WinesScreen(wines:List<WineItem>,loading:Boolean,error:String?,open:(WineItem)->Unit,refresh:()->Unit) {
    var q by remember { mutableStateOf("") }
    val filtered = wines.filter { q.isBlank() || listOfNotNull(it.name,it.producer,it.grape,it.country).any { s -> s.contains(q,true) } }
    Column {
        Column(Modifier.padding(20.dp)) {
            Text("Mi cava",fontSize=36.sp,fontWeight=FontWeight.Black)
            Text(wines.size.toString()+" etiquetas desde SQL Server",color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Buscar vino, uva, país…")},shape=RoundedCornerShape(22.dp),singleLine=true)
            if (loading) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
            error?.let { Spacer(Modifier.height(8.dp)); ErrorCard(it,refresh) }
        }
        LazyColumn(contentPadding=PaddingValues(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            items(filtered) { w ->
                ElevatedCard(Modifier.fillMaxWidth().clickable{open(w)},shape=RoundedCornerShape(24.dp)) {
                    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.WineBar,null,tint=Wine,modifier=Modifier.size(38.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.name,fontWeight=FontWeight.Bold)
                            Text(listOfNotNull(w.grape,w.country,w.vintage?.toString()).joinToString(" · "),color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
                        }
                        Text(w.profileType ?: "SIN DATO",fontSize=11.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiSettings(current:String,onSave:(String)->Unit,onRefresh:()->Unit) {
    var value by remember(current) { mutableStateOf(current) }
    Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Conexión API",fontSize=36.sp,fontWeight=FontWeight.Black)
        Text("La app ya no trae vinos embebidos. Todo viene de tu API publicada en IIS.")
        OutlinedTextField(value,{value=it},Modifier.fillMaxWidth(),label={Text("URL HTTPS de Cava API")},placeholder={Text("https://cavaapi.midominio.com")},shape=RoundedCornerShape(22.dp),singleLine=true)
        Button(onClick={onSave(value)},enabled=value.startsWith("https://"),modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(22.dp)) {
            Icon(Icons.Default.Save,null);Spacer(Modifier.width(8.dp));Text("Guardar y sincronizar")
        }
        OutlinedButton(onClick=onRefresh,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(22.dp)) {
            Icon(Icons.Default.Refresh,null);Spacer(Modifier.width(8.dp));Text("Probar otra vez")
        }
        Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=RoundedCornerShape(26.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Alta de nuevas etiquetas",fontWeight=FontWeight.Bold)
                Text("Por ahora no se capturan desde la app. Me mandas foto frontal y trasera, investigamos la botella y te doy el INSERT/UPDATE SQL revisado. Al ejecutarlo, aparecerá en la app en la siguiente sincronización.")
            }
        }
    }
}

@Composable
private fun WineDetail(w:WineItem,back:()->Unit) {
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { IconButton(onClick=back){Icon(Icons.Default.ArrowBack,null)} }
        item { Text(w.name,fontSize=30.sp,fontWeight=FontWeight.Black);Text(listOfNotNull(w.producer,w.vintage?.toString(),w.region,w.country).joinToString(" · ")) }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=WineDark),shape=RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text((w.profileType ?: "SIN DATO")+" · confianza "+(w.confidence?.toString() ?: "N/D")+"%",color=Color.White,fontWeight=FontWeight.Bold)
                    Text(w.grape ?: w.blend ?: "Uva sin confirmar",color=Color.White,fontSize=21.sp)
                    w.alcohol?.let { Text(it.toString()+"% Alc. Vol.",color=Color.White.copy(alpha=.8f)) }
                    w.oakMonths?.let { Text(it.toString()+" meses de crianza",color=Color.White.copy(alpha=.8f)) }
                }
            }
        }
        item { Metric("Cuerpo",w.body) }
        item { Metric("Tanino",w.tannin) }
        item { Metric("Acidez",w.acidity) }
        item { Metric("Dulzor",w.sweetness) }
        item { Metric("Intensidad",w.intensity) }
        item { Metric("Fruta",w.fruit) }
        item { Metric("Madera",w.oak) }
    }
}

@Composable
private fun Metric(label:String,v:Double?) {
    Row(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(18.dp)).padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f),fontWeight=FontWeight.SemiBold)
        Text(v?.let { String.format("%.1f",it) } ?: "Sin dato",fontWeight=FontWeight.Bold)
    }
}

@Composable
private fun ErrorCard(message:String,retry:()->Unit) {
    Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFFFE2E2)),shape=RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.Warning,null)
            Spacer(Modifier.width(8.dp))
            Text(message,Modifier.weight(1f),maxLines=3)
            IconButton(onClick=retry){Icon(Icons.Default.Refresh,null)}
        }
    }
}
