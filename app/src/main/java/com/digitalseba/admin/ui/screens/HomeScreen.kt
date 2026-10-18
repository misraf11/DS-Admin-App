package com.digitalseba.admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalseba.admin.data.Api
import com.digitalseba.admin.data.LiveHub
import com.digitalseba.admin.data.Prefs
import com.digitalseba.admin.ui.Fmt
import com.digitalseba.admin.ui.Intents
import com.digitalseba.admin.ui.Nav
import com.digitalseba.admin.ui.components.*
import com.digitalseba.admin.ui.rememberActions
import com.digitalseba.admin.ui.rememberLive
import com.digitalseba.admin.ui.theme.Brand
import com.digitalseba.admin.ui.theme.LocalExtra
import com.digitalseba.admin.ui.theme.Tone
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: Nav) {
    val loader = rememberLive { Api.dashboard() }
    val live by LiveHub.live.collectAsState()
    val online by LiveHub.online.collectAsState()
    val actions = rememberActions()
    val ctx = LocalContext.current
    var clock by remember { mutableStateOf(Fmt.clock()) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = Fmt.clock()
            delay(1000)
        }
    }

    val d = loader.data
    val stats = live?.stats ?: d?.stats
    val store = live?.store ?: d?.store
    val unread = live?.badges?.unreadNotifications ?: d?.badges?.unreadNotifications ?: 0

    PullToRefreshBox(isRefreshing = loader.refreshing, onRefresh = { loader.refresh() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ScreenHeader(
                    title = Fmt.greeting() + ", " + Prefs.userName.ifBlank { "Admin" }.substringBefore(" "),
                    subtitle = (store?.name?.ifBlank { null } ?: Prefs.storeName.ifBlank { "DigitalSeba" }) + " · আজকের অপারেশন এক নজরে",
                    actions = {
                        HeaderIcon(Icons.Rounded.Search) { nav.go("search") }
                        HeaderIcon(Icons.Rounded.Notifications, badge = unread) { nav.go("notifications") }
                    },
                    bottom = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LivePill(live?.liveChannel?.ifBlank { null } ?: "Connecting", online)
                            Spacer(Modifier.width(8.dp))
                            Text("🕒 $clock · BD", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Last sync: " + Fmt.date(store?.lastSyncTs),
                            color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HeaderChipButton("Sync Orders", Icons.Rounded.Sync, actions.isBusy("so"), Modifier.weight(1f)) {
                                actions.run("so") { Api.run("sync_orders").message }
                            }
                            HeaderChipButton("Products", Icons.Rounded.Inventory2, actions.isBusy("sp"), Modifier.weight(1f)) {
                                actions.run("sp") { Api.run("sync_products").message }
                            }
                            HeaderChipButton("API Test", Icons.Rounded.NetworkCheck, actions.isBusy("at"), Modifier.weight(1f)) {
                                actions.run("at") { Api.run("api_test").message }
                            }
                        }
                    }
                )
            }

            when {
                d == null && loader.error != null -> item { ErrorState(loader.error!!) { loader.load() } }
                d == null -> item { SkeletonList() }
                else -> {
                    if (d.missingColumns.isNotEmpty()) {
                        item {
                            InfoBanner(
                                "Database needs repair: " + d.missingColumns.joinToString(", "),
                                Brand.Rose,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                            ) {
                                SoftButton("Repair", Brand.Rose, loading = actions.isBusy("rd")) {
                                    actions.run("rd") { Api.run("repair_database").message }
                                }
                            }
                        }
                    }
                    if (stats != null) metricGrid(stats, nav)
                    item { Spacer(Modifier.height(4.dp)) }

                    item {
                        SectionCard(
                            "Orders needing action",
                            "Pending / On-hold অর্ডার আগে handle করুন",
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            accent = Brand.Amber,
                            action = { SmallLinkButton("সব দেখুন", Brand.Amber) { nav.orders("pending") } }
                        ) {
                            if (d.urgentOrders.isEmpty()) {
                                EmptyState(Icons.Rounded.TaskAlt, "No urgent orders", "এখন কোনো pending বা on-hold অর্ডার নেই।", Tone.Green)
                            }
                            d.urgentOrders.forEachIndexed { i, o ->
                                if (i > 0) Spacer(Modifier.height(10.dp))
                                OrderCard(
                                    o,
                                    onClick = { nav.order(o.id) },
                                    onWhatsApp = { Intents.open(ctx, o.whatsappUrl) },
                                    trailingAction = {
                                        SoftButton("Processing", Brand.Blue, loading = actions.isBusy("p${o.id}")) {
                                            actions.run("p${o.id}") { Api.setOrderStatus(o.id, "processing").message }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val rt = (store?.realtimeStatus ?: "") == "active"
                            MiniStat("WooCommerce API", if (rt) "Realtime" else "Connecting", if (rt) Tone.Green else Tone.Amber, Icons.Rounded.Bolt, Modifier.weight(1f)) { nav.go("health") }
                            MiniStat("Database", if (d.missingColumns.isEmpty()) "Healthy" else "Repair", if (d.missingColumns.isEmpty()) Tone.Teal else Tone.Red, Icons.Rounded.Storage, Modifier.weight(1f)) { nav.go("health") }
                        }
                    }

                    item {
                        SectionCard(
                            "Inventory alerts",
                            "Low stock এবং out of stock items",
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            accent = Brand.Rose,
                            action = { SmallLinkButton("Products", Brand.Rose) { nav.products("low") } }
                        ) {
                            if (d.lowStock.isEmpty()) Text("কোনো low stock alert নেই। 🎉", color = LocalExtra.current.muted, style = MaterialTheme.typography.bodyMedium)
                            d.lowStock.forEach { p -> ProductRow(p, onClick = { nav.products("low") }) }
                        }
                    }

                    item {
                        SectionCard(
                            "Recent orders",
                            "Latest WooCommerce order feed",
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            accent = Brand.Blue,
                            action = { SmallLinkButton("All orders", Brand.Blue) { nav.tab("orders") } }
                        ) {
                            if (d.recentOrders.isEmpty()) Text("এখনো কোনো অর্ডার নেই। Sync Orders করুন।", color = LocalExtra.current.muted)
                            d.recentOrders.forEachIndexed { i, o ->
                                if (i > 0) LineDivider()
                                RecentOrderRow(o) { nav.order(o.id) }
                            }
                        }
                    }

                    item {
                        SectionCard(
                            "Recent sync logs",
                            "Last background/manual sync attempts",
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            accent = Brand.Teal,
                            action = { SmallLinkButton("Logs", Brand.Teal) { nav.go("synclogs") } }
                        ) {
                            if (d.syncLogs.isEmpty()) Text("No sync logs yet.", color = LocalExtra.current.muted)
                            d.syncLogs.forEach { SyncLogRow(it) }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.metricGrid(s: com.digitalseba.admin.data.Stats, nav: Nav) {
    val tiles = listOf<@Composable (Modifier) -> Unit>(
        { m -> MetricTile("Today Sales", Fmt.money(s.todaySales), "${Fmt.int(s.todayOrders)} orders today · BD", Tone.Blue, Icons.Rounded.Payments, m) { nav.orders(period = "today") } },
        { m -> MetricTile("Yesterday Sales", Fmt.money(s.yesterdaySales), "${Fmt.int(s.yesterdayOrders)} orders yesterday", Tone.Purple, Icons.Rounded.History, m) { nav.orders(period = "yesterday") } },
        { m -> MetricTile("Total Orders", Fmt.int(s.totalOrders), "All synced orders", Tone.Pink, Icons.Rounded.ShoppingBag, m) { nav.tab("orders") } },
        { m -> MetricTile("Pending Orders", Fmt.int(s.pendingOrders), "Need confirmation", Tone.Amber, Icons.Rounded.PendingActions, m) { nav.orders("pending") } },
        { m -> MetricTile("Low Stock", Fmt.int(s.lowstock), "Items need attention", Tone.Red, Icons.Rounded.WarningAmber, m) { nav.products("low") } },
        { m -> MetricTile("Product Revenue", Fmt.money(s.productRevenue), "Active order items", Tone.Teal, Icons.Rounded.TrendingUp, m) { nav.go("reports") } },
        { m ->
            MetricTile(
                "Visitors Today", Fmt.int(s.visitorsToday),
                if (s.trafficConnected > 0) "${Fmt.int(s.pageviewsToday)} pageviews · live" else "Traffic not connected",
                Tone.Indigo, Icons.Rounded.Visibility, m
            ) { nav.go("reports") }
        },
        { m -> MetricTile("Total Sales", Fmt.money(s.totalSales), "${Fmt.int(s.completedOrders)} completed", Tone.Green, Icons.Rounded.AccountBalanceWallet, m) { nav.go("reports") } },
    )
    tiles.chunked(2).forEach { pair ->
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { it(Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RecentOrderRow(o: com.digitalseba.admin.data.Order, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(o.customerName.ifBlank { "Guest" }, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("#${o.number} · " + o.customerName.ifBlank { "Guest" }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(Fmt.ago(o.createdTs) + " · " + o.customerPhone.ifBlank { "No phone" }, style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Fmt.money(o.total), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(3.dp))
            StatusChip(o.status)
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.ChevronRight, "Open", tint = LocalExtra.current.muted)
        }
    }
}
