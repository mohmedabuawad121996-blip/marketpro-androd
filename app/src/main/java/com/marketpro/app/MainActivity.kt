package com.marketpro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // الواجهة من اليمين إلى اليسار كما في البرنامج
                androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppRoot()
                    }
                }
            }
        }
    }
}

/** يقرر ما يظهر: تسجيل الدخول أولاً ثم الشاشة الرئيسية */
@Composable
fun AppRoot() {
    var api by remember { mutableStateOf<Api?>(null) }
    if (api == null) {
        LoginScreen(onLoggedIn = { api = it })
    } else {
        MainScreen(api = api!!, onLogout = { api = null })
    }
}

/** شاشة الدخول: عنوان الكمبيوتر وكلمة المرور */
@Composable
fun LoginScreen(onLoggedIn: (Api) -> Unit) {
    var server by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("MarketPro", style = MaterialTheme.typography.headlineMedium)
        Text("سجّل الدخول إلى نسخة الكمبيوتر", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = server,
            onValueChange = { server = it },
            label = { Text("عنوان الكمبيوتر (مثال: 192.168.1.10:8080)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("كلمة مرور الشبكة") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(
            enabled = !busy,
            onClick = {
                if (server.isBlank() || pin.isBlank()) {
                    message = "أدخل عنوان الكمبيوتر وكلمة المرور"
                } else {
                    message = ""
                    busy = true
                    scope.launch {
                        val api = Api(server.trim())
                        val err = withContext(Dispatchers.IO) {
                            try { api.login(pin) } catch (e: Exception) { "تعذّر الاتصال بالكمبيوتر: " + (e.message ?: "") }
                        }
                        busy = false
                        if (err == null) onLoggedIn(api) else message = err
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "جارٍ الاتصال..." else "دخول") }
        if (message.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
        }
    }
}

/** الشاشة الرئيسية: تبويبات الأصناف والعملاء والموردين */
@Composable
fun MainScreen(api: Api, onLogout: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("الأصناف", "العملاء", "الموردون")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MarketPro", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onLogout) { Text("خروج") }
        }
        TabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        when (tab) {
            0 -> ProductsTab(api)
            1 -> PartiesTab(api, kind = 0)
            else -> PartiesTab(api, kind = 1)
        }
    }
}

@Composable
fun ProductsTab(api: Api) {
    var productList by remember { mutableStateOf<List<ProductItem>?>(null) }
    var error by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedLoad {
        scope.launch {
            try { productList = withContext(Dispatchers.IO) { api.products().toProducts() } }
            catch (e: Exception) { error = "تعذّر تحميل الأصناف: " + (e.message ?: "") }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            label = { Text("ابحث عن صنف أو باركود") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        when {
            error.isNotEmpty() -> Text(error, color = MaterialTheme.colorScheme.error)
            productList == null -> CircularProgressIndicator()
            else -> {
                val q = query.trim().lowercase()
                val shown = productList!!.filter { q.isEmpty() || it.name.lowercase().contains(q) || it.barcode.contains(q) }
                Text("عدد الأصناف: ${shown.size}")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(shown) { p ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium)
                                Text("الباركود: ${p.barcode}   السعر: ${"%.3f".format(p.price)}")
                                Text("الكمية: ${"%.3f".format(p.qty)} ${p.unit}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PartiesTab(api: Api, kind: Int) {
    var partyList by remember(kind) { mutableStateOf<List<PartyItem>?>(null) }
    var error by remember(kind) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedLoad(key = kind) {
        scope.launch {
            try { partyList = withContext(Dispatchers.IO) { api.parties(kind).toParties() } }
            catch (e: Exception) { error = "تعذّر التحميل: " + (e.message ?: "") }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        when {
            error.isNotEmpty() -> Text(error, color = MaterialTheme.colorScheme.error)
            partyList == null -> CircularProgressIndicator()
            else -> {
                Text("العدد: ${partyList!!.size}")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(partyList!!) { p ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium)
                                if (p.phone.isNotBlank()) Text("الهاتف: ${p.phone}")
                                Text("الرصيد الافتتاحي: ${"%.3f".format(p.opening)}")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** تشغيل تحميل البيانات مرة واحدة عند ظهور الشاشة */
@Composable
fun LaunchedLoad(key: Any? = Unit, block: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(key) { block() }
}
