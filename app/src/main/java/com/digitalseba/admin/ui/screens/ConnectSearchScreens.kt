package com.digitalseba.admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.digitalseba.admin.data.Api
import com.digitalseba.admin.data.Prefs
import com.digitalseba.admin.data.SearchResp
import com.digitalseba.admin.ui.Intents
import com.digitalseba.admin.ui.Nav
import com.digitalseba.admin.ui.components.*
import com.digitalseba.admin.ui.rememberActions
import com.digitalseba.admin.ui.rememberLive
import com.digitalseba.admin.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ConnectScreen(nav: Nav) {
    val loader = rememberLive { Api.connectInfo() }
    val actions = rememberActions()
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var filled by remember { mutableStateOf(false) }
    val d = loader.data
    LaunchedEffect(d) {
        if (d != null && !filled) {
            name = d.store?.name.orEmpty()
            url = d.store?.url.orEmpty()
            filled = true
        }
    }
    val hasExisting = d?.store != null
    val colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = LocalExtra.current.cardBorder, focusedBorderColor = Brand.Violet)

    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            ScreenHeader(
                title = "Connect Store",
                subtitle = "WooCommerce setup wizard",
                onBack = { nav.back() },
                brush = Brush.linearGradient(listOf(Color(0xFF7C83FF), Color(0xFF4F46E5), Color(0xFF19A7FF)))
            )
        }
        item {
            SectionCard("Setup checklist", null, Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), accent = Brand.Blue) {
                HelperStep(1, "Store URL", "আসল WooCommerce domain দিন। Dashboard URL নয়।", Tone.Blue)
                HelperStep(2, "API Key", "Permission অবশ্যই Read/Write হবে।", Tone.Purple)
                HelperStep(3, "Test & Save", "Save করার আগে WooCommerce API verify হবে।", Tone.Pink)
                HelperStep(4, "First Sync", "Connect হওয়ার পরে orders/products sync করুন।", Tone.Green)
            }
        }
        item {
            SectionCard("Connect / Reconnect WooCommerce", null, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), accent = Brand.Violet) {
                InfoBanner("সঠিক: https://digitalseba.shop  ·  ভুল: http://admin.digitalseba.shop", Brand.Amber)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Store Name") }, placeholder = { Text("DigitalSeba") }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = colors, leadingIcon = { Icon(Icons.Rounded.Storefront, null, tint = Brand.Violet) })
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("WooCommerce Store URL") }, placeholder = { Text("https://yourstore.com") }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = colors, leadingIcon = { Icon(Icons.Rounded.Language, null, tint = Brand.Violet) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    key, { key = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Consumer Key") },
                    placeholder = { Text(d?.consumerKeyHint?.ifBlank { null } ?: "ck_xxxxx") },
                    supportingText = { if (hasExisting) Text("ফাঁকা রাখলে আগের key থাকবে") },
                    singleLine = true, shape = RoundedCornerShape(16.dp), colors = colors, leadingIcon = { Icon(Icons.Rounded.Key, null, tint = Brand.Violet) }
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    secret, { secret = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Consumer Secret") },
                    placeholder = { Text(d?.consumerSecretHint?.ifBlank { null } ?: "cs_xxxxx") },
                    supportingText = { if (hasExisting) Text("ফাঁকা রাখলে আগের secret থাকবে") },
                    singleLine = true, shape = RoundedCornerShape(16.dp), colors = colors, leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = Brand.Violet) }
                )
                Spacer(Modifier.height(12.dp))
                GradientButton("Test & Connect Store", Modifier.fillMaxWidth(), icon = Icons.Rounded.Link, loading = actions.isBusy("c")) {
                    actions.run("c", onDone = { Prefs.storeName = name; nav.back() }) { Api.connect(name.trim(), url.trim(), key, secret).message }
                }
            }
        }
        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
fun SearchScreen(nav: Nav) {
    var text by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<SearchResp?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableStateOf(0) }
    val focus = remember { FocusRequester() }
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { delay(200); runCatching { focus.requestFocus() } }
    LaunchedEffect(text) {
        delay(400)
        query = text.trim()
    }
    LaunchedEffect(query, retry) {
        if (query.isEmpty()) { result = null; return@LaunchedEffect }
        loading = true
        try { result = Api.search(query); error = null } catch (e: Exception) { error = e.message } finally { loading = false }
    }

    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            ScreenHeader(
                title = "Search",
                subtitle = "Order, product, customer — সব এক জায়গায়",
                onBack = { nav.back() },
                bottom = { SearchField(text, { text = it }, "অর্ডার নং, নাম, ফোন, প্রোডাক্ট…", Modifier.focusRequester(focus)) }
            )
        }
        val r = result
        when {
            loading && r == null -> item { SkeletonList(3) }
            error != null && r == null -> item { ErrorState(error!!) { retry++ } }
            r == null -> item { EmptyState(Icons.Rounded.Search, "কী খুঁজছেন?", "অর্ডার নম্বর, কাস্টমারের নাম/ফোন বা প্রোডাক্টের নাম লিখুন।", Tone.Pink) }
            r.orders.isEmpty() && r.products.isEmpty() && r.customers.isEmpty() -> item { EmptyState(Icons.Rounded.SearchOff, "কিছু পাওয়া যায়নি", "\"$query\" এর জন্য কোনো ফলাফল নেই।", Tone.Amber) }
            else -> {
                if (r.orders.isNotEmpty()) {
                    item { GroupTitle("Orders", r.orders.size, Brand.Blue) }
                    items(r.orders, key = { "o" + it.id }) { o ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                            OrderCard(o, onClick = { nav.order(o.id) }, onWhatsApp = { Intents.open(ctx, o.whatsappUrl) })
                        }
                    }
                }
                if (r.products.isNotEmpty()) {
                    item { GroupTitle("Products", r.products.size, Brand.Teal) }
                    item {
                        PlainCard(Modifier.padding(horizontal = 16.dp)) {
                            r.products.forEach { p -> ProductRow(p, onClick = { nav.products() }) }
                        }
                    }
                }
                if (r.customers.isNotEmpty()) {
                    item { GroupTitle("Customers", r.customers.size, Brand.Pink) }
                    item {
                        PlainCard(Modifier.padding(horizontal = 16.dp)) {
                            r.customers.forEachIndexed { i, c ->
                                if (i > 0) LineDivider()
                                androidx.compose.foundation.layout.Row(
                                    Modifier.fillMaxWidth().padding(vertical = 9.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Avatar(c.name.ifBlank { "Guest" }, 40.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(c.name.ifBlank { "Guest" }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                        Text("${c.phone.ifBlank { "-" }} · ${c.ordersCount} orders", style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted)
                                    }
                                    SmallLinkButton("Orders", Brand.Pink) { nav.customerOrders(c.name, c.phone, c.email) }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun GroupTitle(title: String, count: Int, color: Color) {
    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(8.dp))
        Pill(count.toString(), color, filled = true)
    }
}
