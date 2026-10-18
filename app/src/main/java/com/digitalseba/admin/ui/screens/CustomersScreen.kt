package com.digitalseba.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalseba.admin.data.Api
import com.digitalseba.admin.data.Customer
import com.digitalseba.admin.ui.Fmt
import com.digitalseba.admin.ui.Intents
import com.digitalseba.admin.ui.Nav
import com.digitalseba.admin.ui.components.*
import com.digitalseba.admin.ui.rememberLive
import com.digitalseba.admin.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(nav: Nav) {
    var searchText by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var riskFilter by remember { mutableStateOf("") }
    LaunchedEffect(searchText) {
        delay(400)
        query = searchText.trim()
    }
    val loader = rememberLive(query) { Api.customers(query) }
    val d = loader.data
    val list = d?.customers.orEmpty().filter { riskFilter.isEmpty() || it.risk == riskFilter }

    PullToRefreshBox(isRefreshing = loader.refreshing, onRefresh = { loader.refresh() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ScreenHeader(
                    title = "Customers",
                    subtitle = "Repeat customer, cancellation risk এবং WhatsApp follow-up",
                    brush = Brush.linearGradient(listOf(Color(0xFFFF5F9E), Color(0xFF9B4DFF), Color(0xFF4F5BFF))),
                    bottom = { SearchField(searchText, { searchText = it }, "Search name, phone, email…") }
                )
            }
            item {
                val all = d?.customers.orEmpty()
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToneChip("All", riskFilter == "", Brand.Violet, count = all.size) { riskFilter = "" }
                    ToneChip("Low", riskFilter == "low", Brand.Green, count = all.count { it.risk == "low" }) { riskFilter = "low" }
                    ToneChip("Medium", riskFilter == "medium", Brand.Amber, count = all.count { it.risk == "medium" }) { riskFilter = "medium" }
                    ToneChip("High", riskFilter == "high", Brand.Rose, count = all.count { it.risk == "high" }) { riskFilter = "high" }
                }
            }
            when {
                d == null && loader.error != null -> item { ErrorState(loader.error!!) { loader.load() } }
                d == null -> item { SkeletonList(4, PaddingValues(horizontal = 16.dp)) }
                list.isEmpty() -> item { EmptyState(Icons.Rounded.People, "No customers found", "Search বদলান অথবা আগে orders sync করুন।", Tone.Pink) }
                else -> items(list, key = { it.name + "|" + it.phone + "|" + it.email }) { c ->
                    CustomerCard(c, Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { nav.customerOrders(c.name, c.phone, c.email) }
                }
            }
        }
    }
}

@Composable
private fun CustomerCard(c: Customer, modifier: Modifier, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    val riskColor = when (c.risk) { "high" -> Brand.Rose; "medium" -> Brand.Amber; else -> Brand.Green }
    PlainCard(modifier, onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(c.name.ifBlank { "Guest" }, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name.ifBlank { "Guest" }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(c.phone.ifBlank { "-" }, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.muted)
                Text(c.email.ifBlank { "No email" }, style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Pill(c.risk.replaceFirstChar { it.uppercase() } + " risk", riskColor)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyValue("Orders", c.ordersCount.toString(), Modifier.weight(1f), valueColor = Brand.Blue)
            KeyValue("Total Spent", Fmt.money(c.totalSpent), Modifier.weight(1.4f), valueColor = Brand.Green)
            KeyValue("Last order", Fmt.ago(c.lastOrderTs), Modifier.weight(1.1f))
        }
        if (c.phone.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftButton("WhatsApp", Brand.WhatsAppDark, Modifier.weight(1f), icon = Icons.AutoMirrored.Rounded.Chat) { Intents.open(ctx, c.whatsappUrl) }
                SoftButton("Call", Brand.Blue, Modifier.weight(1f), icon = Icons.Rounded.Call) { Intents.dial(ctx, c.phone) }
                SoftButton("Orders", Brand.Violet, Modifier.weight(1f), icon = Icons.Rounded.ReceiptLong) { onOpen() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOrdersScreen(nav: Nav, name: String, phone: String, email: String) {
    val loader = rememberLive(name, phone, email) { Api.customerOrders(name, phone, email) }
    val ctx = LocalContext.current
    val d = loader.data
    PullToRefreshBox(isRefreshing = loader.refreshing, onRefresh = { loader.refresh() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ScreenHeader(
                    title = name.ifBlank { "Guest customer" },
                    subtitle = listOf(phone, email).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Customer orders" },
                    onBack = { nav.back() },
                    brush = Brush.linearGradient(listOf(Color(0xFFFF5F9E), Color(0xFF9B4DFF), Color(0xFF4F5BFF))),
                    actions = {
                        if (phone.isNotBlank()) {
                            HeaderIcon(Icons.Rounded.Call) { Intents.dial(ctx, phone) }
                            HeaderIcon(Icons.AutoMirrored.Rounded.Chat) { Intents.whatsapp(ctx, phone) }
                        }
                    },
                    bottom = {
                        val orders = d?.orders.orEmpty()
                        val spent = orders.filter { it.status !in listOf("cancelled", "failed", "refunded") }.sumOf { it.total }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HeaderStat("Orders", orders.size.toString(), Modifier.weight(1f))
                            HeaderStat("Spent", Fmt.money(spent), Modifier.weight(1.5f))
                            HeaderStat("Cancelled", orders.count { it.status == "cancelled" }.toString(), Modifier.weight(1f))
                        }
                    }
                )
            }
            when {
                d == null && loader.error != null -> item { ErrorState(loader.error!!) { loader.load() } }
                d == null -> item { SkeletonList(3) }
                d.orders.isEmpty() -> item { EmptyState(Icons.Rounded.ShoppingBag, "No orders", "এই কাস্টমারের কোনো অর্ডার পাওয়া যায়নি।") }
                else -> items(d.orders, key = { it.id }) { o ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        OrderCard(o, onClick = { nav.order(o.id) }, onWhatsApp = { Intents.open(ctx, o.whatsappUrl) })
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
fun HeaderStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
        AutoText(value, Color.White, 17.sp)
    }
}
