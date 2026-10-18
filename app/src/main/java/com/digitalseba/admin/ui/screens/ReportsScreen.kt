package com.digitalseba.admin.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalseba.admin.data.Api
import com.digitalseba.admin.data.DaySales
import com.digitalseba.admin.ui.Fmt
import com.digitalseba.admin.ui.Nav
import com.digitalseba.admin.ui.components.*
import com.digitalseba.admin.ui.rememberLive
import com.digitalseba.admin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(nav: Nav) {
    val loader = rememberLive { Api.reports() }
    val d = loader.data
    PullToRefreshBox(isRefreshing = loader.refreshing, onRefresh = { loader.refresh() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ScreenHeader(
                    title = "Reports",
                    subtitle = "Colorful business insights · BD time",
                    onBack = { nav.back() },
                    brush = Brush.linearGradient(listOf(Color(0xFFA05CFF), Color(0xFF6A3BEA), Color(0xFFE8457E))),
                    actions = { HeaderIcon(Icons.Rounded.History) { nav.go("synclogs") } }
                )
            }
            when {
                d == null && loader.error != null -> item { ErrorState(loader.error!!) { loader.load() } }
                d == null -> item { SkeletonList(4) }
                else -> {
                    val s = d.stats
                    item {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MetricTile("Total Sales", Fmt.money(s.totalSales), "Active synced orders", Tone.Blue, Icons.Rounded.AccountBalanceWallet, Modifier.weight(1f))
                                MetricTile("Product Revenue", Fmt.money(s.productRevenue), "Order item revenue", Tone.Purple, Icons.Rounded.TrendingUp, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MetricTile(
                                    "Visitors Today", Fmt.int(s.visitorsToday),
                                    if (s.trafficConnected > 0) "${Fmt.int(s.pageviewsToday)} pageviews" else "Traffic not connected",
                                    Tone.Amber, Icons.Rounded.Visibility, Modifier.weight(1f)
                                )
                                MetricTile("Cancelled", Fmt.int(s.cancelledOrders), "Need recovery", Tone.Red, Icons.Rounded.Cancel, Modifier.weight(1f)) { nav.orders("cancelled") }
                            }
                        }
                    }
                    item {
                        SectionCard("Last 7 days sales", "প্রতিদিনের বিক্রি (৳) ও অর্ডার", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), accent = Brand.Blue) {
                            val total = d.daily.sumOf { it.sales }
                            val orders = d.daily.sumOf { it.orders }
                            Row {
                                KeyValue("7-day sales", Fmt.money(total), Modifier.weight(1f), valueColor = Brand.Blue)
                                Spacer(Modifier.width(10.dp))
                                KeyValue("7-day orders", Fmt.int(orders), Modifier.weight(1f), valueColor = Brand.Violet)
                            }
                            Spacer(Modifier.height(14.dp))
                            SalesChart(d.daily)
                        }
                    }
                    item {
                        SectionCard("Orders by status", null, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), accent = Brand.Pink) {
                            val max = (d.byStatus.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
                            if (d.byStatus.isEmpty()) Text("No report data yet.", color = LocalExtra.current.muted)
                            d.byStatus.forEach { r ->
                                BarRow(statusLabel(r.status), "${r.count} · ${Fmt.money(r.total)}", r.count.toFloat() / max, statusColor(r.status))
                            }
                        }
                    }
                    item {
                        SectionCard("Payment methods", null, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), accent = Brand.Green) {
                            if (d.payments.isEmpty()) Text("No data yet.", color = LocalExtra.current.muted)
                            val tones = listOf(Tone.Green, Tone.Blue, Tone.Purple, Tone.Amber, Tone.Pink, Tone.Teal, Tone.Indigo, Tone.Red)
                            val maxP = (d.payments.maxOfOrNull { it.total } ?: 1.0).coerceAtLeast(1.0)
                            d.payments.forEachIndexed { i, p ->
                                BarRow(p.method, "${p.count} orders · ${Fmt.money(p.total)}", (p.total / maxP).toFloat(), tones[i % tones.size].end)
                            }
                        }
                    }
                    item {
                        SectionCard("Top product revenue", "সবচেয়ে বেশি আয় করা প্রোডাক্ট", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), accent = Brand.Violet) {
                            if (d.topProducts.isEmpty()) Text("No product revenue yet.", color = LocalExtra.current.muted)
                            d.topProducts.forEachIndexed { i, p ->
                                if (i > 0) LineDivider()
                                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val medal = when (i) { 0 -> Tone.Amber; 1 -> Tone.Indigo; 2 -> Tone.Pink; else -> Tone.Blue }
                                    Box(Modifier.size(30.dp).clip(CircleShape).background(medal.brush), contentAlignment = Alignment.Center) {
                                        Text("${i + 1}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(p.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text("${p.units} units", style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted)
                                    }
                                    Text(Fmt.money(p.revenue), style = MaterialTheme.typography.titleSmall, color = Brand.Green)
                                }
                            }
                        }
                    }
                    item {
                        SectionCard("Visitor activity", "WordPress companion plugin থেকে · BD time", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), accent = Brand.Amber) {
                            if (d.traffic.isEmpty()) Text("Waiting for visitor data from the WordPress companion plugin.", color = LocalExtra.current.muted)
                            val maxV = (d.traffic.maxOfOrNull { it.visitors } ?: 1).coerceAtLeast(1)
                            d.traffic.forEach { t ->
                                BarRow(Fmt.dayLabel(t.date), "${t.visitors} visitors · ${t.pageviews} views", t.visitors.toFloat() / maxV, Brand.Amber)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun SalesChart(days: List<DaySales>) {
    if (days.isEmpty()) {
        Text("No data yet.", color = LocalExtra.current.muted)
        return
    }
    val max = (days.maxOf { it.sales }).coerceAtLeast(1.0)
    val grid = LocalExtra.current.cardBorder
    Column {
        Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            days.forEachIndexed { i, d ->
                val isToday = i == days.lastIndex
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text(
                        if (d.sales > 0) Fmt.moneyCompact(d.sales).removePrefix("৳") else "",
                        fontSize = 9.sp, color = if (isToday) Brand.Violet else LocalExtra.current.muted, maxLines = 1, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(3.dp))
                    val frac = (d.sales / max).toFloat().coerceIn(0.03f, 1f)
                    Canvas(Modifier.fillMaxWidth().fillMaxHeight(frac * 0.85f)) {
                        drawRoundRect(
                            brush = if (isToday) Brush.verticalGradient(listOf(Color(0xFFFF6FA8), Color(0xFF7B3BFF)))
                            else Brush.verticalGradient(listOf(Color(0xFF6F9BFF), Color(0xFF4F5BFF))),
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(grid))
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEachIndexed { i, d ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (i == days.lastIndex) "আজ" else Fmt.weekday(d.date), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (i == days.lastIndex) Brand.Violet else MaterialTheme.colorScheme.onSurface)
                    Text("${d.orders}", fontSize = 9.sp, color = LocalExtra.current.muted)
                }
            }
        }
    }
}
