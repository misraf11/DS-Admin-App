package com.digitalseba.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalseba.admin.data.Api
import com.digitalseba.admin.data.Order
import com.digitalseba.admin.data.OrderDetailResp
import com.digitalseba.admin.ui.Fmt
import com.digitalseba.admin.ui.Intents
import com.digitalseba.admin.ui.Nav
import com.digitalseba.admin.ui.components.*
import com.digitalseba.admin.ui.rememberActions
import com.digitalseba.admin.ui.rememberLive
import com.digitalseba.admin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(nav: Nav, id: Int) {
    val loader = rememberLive(id) { Api.order(id) }
    val actions = rememberActions()
    val ctx = LocalContext.current
    var showStatusSheet by remember { mutableStateOf(false) }
    var confirmStatus by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }

    val d = loader.data
    val o = d?.order

    fun setStatus(s: String) {
        actions.run("st") {
            val r = Api.setOrderStatus(id, s)
            loader.load(silent = true)
            r.message
        }
    }

    PullToRefreshBox(isRefreshing = loader.refreshing, onRefresh = { loader.refresh() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ScreenHeader(
                    title = if (o != null) "Order #${o.number}" else "Order Details",
                    subtitle = if (o != null) "Woo #${o.wcOrderId} · ${Fmt.ago(o.createdTs)}" else "Order fulfillment workspace",
                    onBack = { nav.back() },
                    brush = if (o != null) statusTone(o.status).brush else BrandGradient,
                    actions = {
                        if (o != null && d != null) {
                            if (o.customerPhone.isNotBlank()) HeaderIcon(Icons.AutoMirrored.Rounded.Chat) { Intents.open(ctx, o.whatsappUrl) }
                            Box {
                                HeaderIcon(Icons.Rounded.MoreVert) { menu = true }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(text = { Text("Change status") }, leadingIcon = { Icon(Icons.Rounded.SwapHoriz, null) }, onClick = { menu = false; showStatusSheet = true })
                                    if (o.customerPhone.isNotBlank()) {
                                        DropdownMenuItem(text = { Text("Message on WhatsApp") }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Chat, null, tint = Brand.WhatsApp) }, onClick = { menu = false; Intents.open(ctx, o.whatsappUrl) })
                                        DropdownMenuItem(text = { Text("Call customer") }, leadingIcon = { Icon(Icons.Rounded.Call, null) }, onClick = { menu = false; Intents.dial(ctx, o.customerPhone) })
                                    }
                                    DropdownMenuItem(text = { Text("Copy order info") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = { menu = false; Intents.copy(ctx, orderSummary(o, d.items.map { "${it.name} ×${it.quantity}" })) })
                                    DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = { menu = false; Intents.share(ctx, orderSummary(o, d.items.map { "${it.name} ×${it.quantity}" })) })
                                    if (d.canCancel) {
                                        DropdownMenuItem(text = { Text("Cancel order", color = Brand.Rose) }, leadingIcon = { Icon(Icons.Rounded.Cancel, null, tint = Brand.Rose) }, onClick = { menu = false; confirmStatus = "cancelled" })
                                    }
                                }
                            }
                        }
                    },
                    bottom = {
                        if (o != null && d != null) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Column(Modifier.weight(1f)) {
                                    Text("Total", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                    Text(Fmt.money(o.total), color = Color.White, style = NumberStyle, fontSize = 30.sp)
                                }
                                Text(
                                    statusLabel(o.status) + statusBangla(o.status).let { if (it.isNotEmpty()) " · $it" else "" },
                                    color = statusColor(o.status), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val next = d.nextStatus
                                if (next != null) {
                                    Box(
                                        Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp)).background(Color.White)
                                            .clickable(enabled = actions.busy == null) { setStatus(next) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (actions.isBusy("st")) CircularProgressIndicator(Modifier.size(20.dp), color = statusColor(next), strokeWidth = 2.dp)
                                        else Text("Mark as ${statusLabel(next)}", color = statusColor(next), fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                                HeaderChipButton("Status", Icons.Rounded.SwapHoriz, modifier = Modifier.height(46.dp).then(if (next == null) Modifier.weight(1f) else Modifier)) { showStatusSheet = true }
                            }
                        }
                    }
                )
            }

            when {
                d == null && loader.error != null -> item { ErrorState(loader.error!!) { loader.load() } }
                d == null -> item { SkeletonList(3) }
                else -> detailBody(d, onWhatsApp = { Intents.open(ctx, d.order.whatsappUrl) }, actions = actions, reload = { loader.load(silent = true) })
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    if (showStatusSheet && d != null) {
        ModalBottomSheet(onDismissRequest = { showStatusSheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text("Order status বদলান", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("WooCommerce store-এ সাথে সাথে আপডেট হবে।", style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.muted)
                Spacer(Modifier.height(12.dp))
                d.statuses.ifEmpty { listOf("pending", "processing", "on-hold", "completed", "cancelled", "refunded", "failed") }.forEach { s ->
                    val current = s == d.order.status
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(if (current) statusColor(s).copy(alpha = 0.12f) else Color.Transparent)
                            .clickable(enabled = !current) {
                                showStatusSheet = false
                                if (s in listOf("cancelled", "refunded", "failed")) confirmStatus = s else setStatus(s)
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(statusColor(s)))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(statusLabel(s), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            if (statusBangla(s).isNotEmpty()) Text(statusBangla(s), style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted)
                        }
                        if (current) Icon(Icons.Rounded.CheckCircle, null, tint = statusColor(s))
                    }
                }
            }
        }
    }

    val cs = confirmStatus
    if (cs != null && o != null) {
        AlertDialog(
            onDismissRequest = { confirmStatus = null },
            icon = { Icon(Icons.Rounded.WarningAmber, null, tint = Brand.Rose) },
            title = { Text("${statusLabel(cs)} order #${o.number}?") },
            text = { Text("এই অর্ডারের status ${statusLabel(cs)} করা হবে এবং WooCommerce-এ আপডেট হবে।") },
            confirmButton = {
                TextButton(onClick = { confirmStatus = null; setStatus(cs) }) { Text("হ্যাঁ, ${statusLabel(cs)}", color = Brand.Rose, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmStatus = null }) { Text("না") } }
        )
    }
}

private fun orderSummary(o: Order, items: List<String>): String = buildString {
    appendLine("Order #${o.number} · ${statusLabel(o.status)}")
    appendLine("Total: ${Fmt.money(o.total)}")
    items.forEach { appendLine("• $it") }
    appendLine("Customer: ${o.customerName.ifBlank { "Guest" }} ${o.customerPhone}")
    val addr = o.billingAddress.ifBlank { o.shippingAddress }
    if (addr.isNotBlank()) appendLine("Address: $addr")
    append("Date: ${Fmt.dateLong(o.createdTs)}")
}

private fun androidx.compose.foundation.lazy.LazyListScope.detailBody(
    d: OrderDetailResp,
    onWhatsApp: () -> Unit,
    actions: com.digitalseba.admin.ui.ActionRunner,
    reload: () -> Unit,
) {
    val o = d.order
    item {
        SectionCard("Products", "${d.items.size} ${if (d.items.size == 1) "item" else "items"}", Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), accent = Brand.Violet) {
            if (d.items.isEmpty()) Text("No items synced for this order.", color = LocalExtra.current.muted)
            d.items.forEachIndexed { i, it ->
                if (i > 0) LineDivider()
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProductImage(it.imageUrl, it.name, 50.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(it.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                        Text(it.sku.ifBlank { "No SKU" }, style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted)
                    }
                    Pill("×${it.quantity}", Brand.Blue)
                    Spacer(Modifier.width(10.dp))
                    Text(Fmt.money(it.total), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
    item {
        SectionCard("Payment details", null, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), accent = Brand.Green) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KeyValue("Method", o.paymentTitle.ifBlank { o.paymentMethod.ifBlank { "Payment n/a" } }, Modifier.weight(1f))
                KeyValue("Total", Fmt.money(o.total), Modifier.weight(1f), valueColor = Brand.Green)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KeyValue("Shipping", Fmt.money(o.shippingTotal), Modifier.weight(1f))
                KeyValue("Discount", Fmt.money(o.discountTotal), Modifier.weight(1f), valueColor = Brand.Pink)
            }
            if (o.isCod) {
                Spacer(Modifier.height(10.dp))
                InfoBanner("Cash on Delivery অর্ডার — কাস্টমারের সাথে কনফার্ম করে নিন।", Brand.Pink, icon = Icons.Rounded.Payments)
            }
        }
    }
    item {
        val ctx = LocalContext.current
        SectionCard("Customer", null, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), accent = Brand.Blue) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(o.customerName.ifBlank { "Guest" }, 50.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(o.customerName.ifBlank { "Guest customer" }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(o.customerPhone.ifBlank { "No phone" }, style = MaterialTheme.typography.bodyMedium, color = LocalExtra.current.muted)
                    Text(o.customerEmail.ifBlank { "No email" }, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.muted)
                }
            }
            if (o.customerPhone.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftButton("Call", Brand.Blue, Modifier.weight(1f), icon = Icons.Rounded.Call) { Intents.dial(ctx, o.customerPhone) }
                    SoftButton("WhatsApp", Brand.WhatsAppDark, Modifier.weight(1f), icon = Icons.AutoMirrored.Rounded.Chat) { onWhatsApp() }
                    SoftButton("Copy", Brand.Violet, Modifier.weight(1f), icon = Icons.Rounded.ContentCopy) { Intents.copy(ctx, o.customerPhone) }
                }
            }
            val addr = o.billingAddress.ifBlank { o.shippingAddress }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(LocalExtra.current.subtle)
                    .clickable(enabled = addr.isNotBlank()) { Intents.copy(ctx, addr) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.LocationOn, null, tint = Brand.Rose, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(addr.ifBlank { "No address" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            }
        }
    }
    item {
        SectionCard("Customer note", null, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), accent = Brand.Amber) {
            Text(o.customerNote.ifBlank { "No customer note." }, style = MaterialTheme.typography.bodyMedium, color = if (o.customerNote.isBlank()) LocalExtra.current.muted else MaterialTheme.colorScheme.onSurface)
        }
    }
    item {
        SectionCard(
            "WhatsApp message", "Settings-এর template থেকে তৈরি",
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp), accent = Brand.WhatsApp,
            action = { WhatsAppMark(o.whatsappSentTs, actions.isBusy("wm")) { actions.run("wm") { val r = Api.markWhatsApp(o.id, o.whatsappSentTs == null); reload(); r.message } } }
        ) {
            Text(
                o.whatsappMessage.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brand.WhatsApp.copy(alpha = 0.08f)).padding(12.dp)
            )
            if (o.customerPhone.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                GradientButton("Open WhatsApp", Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.Chat, brush = Tone.Green.brush, height = 46.dp) { onWhatsApp() }
            }
        }
    }
    item {
        SectionCard("Timeline", null, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), accent = Brand.Purple) {
            TimelineRow("Created", Fmt.dateLong(o.createdTs), Brand.Blue, first = true)
            if (o.whatsappSentTs != null) TimelineRow("WhatsApp sent", Fmt.dateLong(o.whatsappSentTs), Brand.WhatsApp)
            TimelineRow("Last updated", Fmt.dateLong(o.updatedTs), Brand.Purple)
            TimelineRow("Status", statusLabel(o.status), statusColor(o.status), last = true)
        }
    }
}

@Composable
private fun TimelineRow(title: String, value: String, color: Color, first: Boolean = false, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(20.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(2.dp).height(8.dp).background(if (first) Color.Transparent else LocalExtra.current.cardBorder))
            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
            Box(Modifier.width(2.dp).weight(1f).background(if (last) Color.Transparent else LocalExtra.current.cardBorder))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.padding(bottom = 12.dp, top = 4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(value, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.muted)
        }
    }
}
