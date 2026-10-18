package com.digitalseba.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalseba.admin.data.Api
import com.digitalseba.admin.data.AppNotification
import com.digitalseba.admin.ui.Fmt
import com.digitalseba.admin.ui.Intents
import com.digitalseba.admin.ui.Nav
import com.digitalseba.admin.ui.components.*
import com.digitalseba.admin.ui.rememberActions
import com.digitalseba.admin.ui.rememberLive
import com.digitalseba.admin.ui.theme.*

private val WhatsAppGradient = Brush.linearGradient(listOf(Color(0xFF25D366), Color(0xFF0FA67A), Color(0xFF128C7E)))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppScreen(nav: Nav) {
    val loader = rememberLive { Api.whatsapp() }
    val actions = rememberActions()
    val ctx = LocalContext.current
    val d = loader.data
    PullToRefreshBox(isRefreshing = loader.refreshing, onRefresh = { loader.refresh() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ScreenHeader(
                    title = "WhatsApp Center",
                    subtitle = "COD follow-up ও message templates",
                    onBack = { nav.back() },
                    brush = WhatsAppGradient,
                    actions = { HeaderIcon(Icons.Rounded.Edit) { nav.go("settings") } },
                    bottom = {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HeaderStat("Pending follow-ups", (d?.pendingCount ?: 0).toString(), Modifier.weight(1f))
                            HeaderStat("Templates", (d?.templates?.size ?: 4).toString(), Modifier.weight(0.8f))
                            HeaderStat("COD focus", (d?.codCount ?: 0).toString(), Modifier.weight(0.9f))
                        }
                    }
                )
            }
            when {
                d == null && loader.error != null -> item { ErrorState(loader.error!!) { loader.load() } }
                d == null -> item { SkeletonList(3) }
                else -> {
                    item {
                        SectionCard("Message templates", "কোন পরিস্থিতিতে কোন মেসেজ", Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), accent = Brand.WhatsApp) {
                            val tones = listOf(Tone.Green, Tone.Pink, Tone.Blue, Tone.Red)
                            d.templates.forEachIndexed { i, t ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(32.dp).clip(CircleShape).background(tones[i % tones.size].brush), contentAlignment = Alignment.Center) {
                                        Text("${i + 1}", color = Color.White, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(t.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                        Text(t.description, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.muted)
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            SoftButton("Order template edit করুন", Brand.WhatsAppDark, Modifier.fillMaxWidth(), icon = Icons.Rounded.Edit) { nav.go("settings") }
                        }
                    }
                    item {
                        Text(
                            "Pending follow-up queue",
                            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                    if (d.queue.isEmpty()) item { EmptyState(Icons.Rounded.TaskAlt, "সব follow-up শেষ!", "কোনো pending অর্ডারে follow-up দরকার নেই।", Tone.Green) }
                    items(d.queue, key = { it.id }) { o ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            OrderCard(
                                o,
                                onClick = { nav.order(o.id) },
                                onWhatsApp = { Intents.open(ctx, o.whatsappUrl) },
                                onToggleMark = { actions.run("m${o.id}", onDone = { loader.load(silent = true) }) { Api.markWhatsApp(o.id, o.whatsappSentTs == null).message } },
                                markBusy = actions.isBusy("m${o.id}")
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(nav: Nav) {
    val loader = rememberLive { Api.notifications() }
    val actions = rememberActions()
    val d = loader.data
    PullToRefreshBox(isRefreshing = loader.refreshing, onRefresh = { loader.refresh() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ScreenHeader(
                    title = "Notifications",
                    subtitle = "New order, sync, stock ও store events · ${d?.badges?.unreadNotifications ?: 0} unread",
                    onBack = { nav.back() },
                    brush = Brush.linearGradient(listOf(Color(0xFFFFB547), Color(0xFFF5821F), Color(0xFFE8457E))),
                    actions = {
                        HeaderIcon(Icons.Rounded.DoneAll, loading = actions.isBusy("read")) {
                            actions.run("read", onDone = { loader.load(silent = true) }) { Api.markNotificationsRead().message }
                        }
                        HeaderIcon(Icons.Rounded.Healing) { nav.go("health") }
                    }
                )
            }
            when {
                d == null && loader.error != null -> item { ErrorState(loader.error!!) { loader.load() } }
                d == null -> item { SkeletonList(4) }
                d.notifications.isEmpty() -> item { EmptyState(Icons.Rounded.NotificationsNone, "No notifications yet", "New orders, sync events এবং stock alerts এখানে দেখাবে।", Tone.Amber) }
                else -> {
                    item { Spacer(Modifier.height(10.dp)) }
                    items(d.notifications, key = { it.id }) { n -> NotificationRow(n, Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun NotificationRow(n: AppNotification, modifier: Modifier) {
    val (color, icon) = when (n.type.lowercase()) {
        "error" -> Brand.Rose to Icons.Rounded.ErrorOutline
        "warning" -> Brand.Amber to Icons.Rounded.WarningAmber
        "success" -> Brand.Green to Icons.Rounded.CheckCircle
        else -> when {
            n.type.contains("order") -> Brand.Blue to Icons.Rounded.ShoppingBag
            n.type.contains("product") || n.type.contains("stock") -> Brand.Teal to Icons.Rounded.Inventory2
            else -> Brand.Violet to Icons.Rounded.Info
        }
    }
    PlainCard(modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(n.type.ifBlank { "info" }, color)
                    Spacer(Modifier.weight(1f))
                    Text(Fmt.ago(n.createdTs), style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted)
                    if (n.unread) {
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(9.dp).clip(CircleShape).background(Brand.Rose))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(n.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                if (n.message.isNotBlank()) Text(n.message, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.muted)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncLogsScreen(nav: Nav) {
    val loader = rememberLive { Api.syncLogs() }
    val actions = rememberActions()
    val ctx = LocalContext.current
    val d = loader.data
    var expanded by remember { mutableStateOf(setOf<Int>()) }
    PullToRefreshBox(isRefreshing = loader.refreshing, onRefresh = { loader.refresh() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ScreenHeader(
                    title = "Sync Logs",
                    subtitle = "প্রতিটি sync attempt, records এবং errors",
                    onBack = { nav.back() },
                    brush = Brush.linearGradient(listOf(Color(0xFF2DD4BF), Color(0xFF0E9F9A), Color(0xFF2F6BFF))),
                    bottom = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HeaderChipButton("Sync Orders", Icons.Rounded.Sync, actions.isBusy("so"), Modifier.weight(1f)) { actions.run("so") { Api.run("sync_orders").message } }
                            HeaderChipButton("Repair Database", Icons.Rounded.Build, actions.isBusy("rd"), Modifier.weight(1f)) { actions.run("rd") { Api.run("repair_database").message } }
                        }
                    }
                )
            }
            when {
                d == null && loader.error != null -> item { ErrorState(loader.error!!) { loader.load() } }
                d == null -> item { SkeletonList(4) }
                d.logs.isEmpty() -> item { EmptyState(Icons.Rounded.History, "No sync log yet", "Sync Orders চাপলে এখানে log দেখাবে।", Tone.Teal) }
                else -> {
                    item { Spacer(Modifier.height(10.dp)) }
                    items(d.logs, key = { it.id }) { l ->
                        val c = when (l.status) { "completed" -> Brand.Green; "failed" -> Brand.Rose; else -> Brand.Blue }
                        PlainCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(c.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                                    Icon(if (l.type.startsWith("product")) Icons.Rounded.Inventory2 else Icons.Rounded.ShoppingBag, null, tint = c)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(l.type, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                    Text(Fmt.date(l.startedTs) + " · " + Fmt.ago(l.startedTs), style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted)
                                }
                                Pill(l.status, c)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                KeyValue("Processed", l.processed.toString(), Modifier.weight(1f), valueColor = Brand.Blue)
                                KeyValue("Created", l.created.toString(), Modifier.weight(1f), valueColor = Brand.Green)
                                KeyValue("Updated", l.updated.toString(), Modifier.weight(1f), valueColor = Brand.Purple)
                            }
                            if (l.error.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                val open = l.id in expanded
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SmallLinkButton(if (open) "Hide error" else "Show error", Brand.Rose) { expanded = if (open) expanded - l.id else expanded + l.id }
                                    Spacer(Modifier.width(8.dp))
                                    SmallLinkButton("Copy", Brand.Violet) { Intents.copy(ctx, l.error) }
                                }
                                if (open) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(l.error, style = MaterialTheme.typography.bodySmall, color = Brand.Rose, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brand.Rose.copy(alpha = 0.08f)).padding(10.dp))
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(nav: Nav) {
    val loader = rememberLive { Api.health() }
    val actions = rememberActions()
    val d = loader.data
    PullToRefreshBox(isRefreshing = loader.refreshing, onRefresh = { loader.refresh() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ScreenHeader(
                    title = "Store Health",
                    subtitle = "API, database, sync এবং connection diagnostics",
                    onBack = { nav.back() },
                    brush = Brush.linearGradient(listOf(Color(0xFFFF7A7A), Color(0xFFE8457E), Color(0xFF8B5CF6))),
                    bottom = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HeaderChipButton("API Test", Icons.Rounded.NetworkCheck, actions.isBusy("at"), Modifier.weight(1f)) { actions.run("at") { Api.run("api_test").message } }
                            HeaderChipButton("Repair Database", Icons.Rounded.Build, actions.isBusy("rd"), Modifier.weight(1f)) { actions.run("rd") { Api.run("repair_database").message } }
                        }
                    }
                )
            }
            when {
                d == null && loader.error != null -> item { ErrorState(loader.error!!) { loader.load() } }
                d == null -> item { SkeletonList(3) }
                else -> {
                    val rt = d.store?.realtimeStatus == "active"
                    val cards = listOf(
                        HealthCard("Store Connection", "Connected", d.store?.url.orEmpty(), Tone.Green, Icons.Rounded.Link),
                        HealthCard("Database Schema", if (d.missingColumns.isEmpty()) "Healthy" else "Needs Repair", if (d.missingColumns.isEmpty()) "All required columns available." else d.missingColumns.joinToString(", "), if (d.missingColumns.isEmpty()) Tone.Teal else Tone.Red, Icons.Rounded.Storage),
                        HealthCard("Last Sync", Fmt.date(d.store?.lastSyncTs), d.lastLog?.let { "${it.type} · ${it.status}" } ?: "No sync log yet.", Tone.Blue, Icons.Rounded.Sync),
                        HealthCard("Realtime", if (rt) "Active" else (d.store?.realtimeStatus ?: "pending").replaceFirstChar { it.uppercase() }, "Last webhook " + Fmt.date(d.store?.lastWebhookTs), if (rt) Tone.Green else Tone.Amber, Icons.Rounded.Bolt),
                        HealthCard("Orders Endpoint", "${d.totalOrders} synced", "Test by sync", Tone.Purple, Icons.Rounded.ShoppingBag, "so"),
                        HealthCard("Products Endpoint", "${d.totalProducts} synced", "Test by sync", Tone.Pink, Icons.Rounded.Inventory2, "sp"),
                        HealthCard("Security", "API Keys Encrypted", "Consumer secret encrypted in database.", Tone.Indigo, Icons.Rounded.Lock),
                        HealthCard("App Token", "Secure", "90 দিনের রোলিং টোকেন", Tone.Teal, Icons.Rounded.VerifiedUser),
                    )
                    item { Spacer(Modifier.height(10.dp)) }
                    cards.chunked(2).forEach { pair ->
                        item {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                pair.forEach { c ->
                                    HealthTile(c, actions.isBusy(c.action ?: "-"), Modifier.weight(1f)) {
                                        when (c.action) {
                                            "so" -> actions.run("so") { Api.run("sync_orders").message }
                                            "sp" -> actions.run("sp") { Api.run("sync_products").message }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        SectionCard("Problem helper", null, Modifier.padding(16.dp), accent = Brand.Amber) {
                            HelperStep(1, "No orders?", "Sync Orders চাপুন। Error এলে Sync Logs দেখুন।", Tone.Blue)
                            HelperStep(2, "API 401/403?", "WooCommerce key Read/Write কিনা check করুন।", Tone.Amber)
                            HelperStep(3, "Database column error?", "Repair Database চাপুন।", Tone.Red)
                        }
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

private data class HealthCard(val title: String, val value: String, val sub: String, val tone: Tone, val icon: androidx.compose.ui.graphics.vector.ImageVector, val action: String? = null)

@Composable
private fun HealthTile(c: HealthCard, busy: Boolean, modifier: Modifier, onAction: () -> Unit) {
    PlainCard(modifier.height(170.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(c.tone.brush), contentAlignment = Alignment.Center) {
            Icon(c.icon, null, tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(c.title, style = MaterialTheme.typography.labelMedium, color = LocalExtra.current.muted)
        Text(c.value, style = MaterialTheme.typography.titleSmall, color = c.tone.end, maxLines = 1)
        Text(c.sub, style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted, maxLines = 2)
        if (c.action != null) {
            Spacer(Modifier.weight(1f))
            SmallLinkButton(if (busy) "Syncing…" else "Test by Sync", c.tone.end, onAction)
        }
    }
}

@Composable
fun HelperStep(n: Int, title: String, text: String, tone: Tone) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(tone.brush), contentAlignment = Alignment.Center) {
            Text("$n", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(text, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.muted)
        }
    }
}
