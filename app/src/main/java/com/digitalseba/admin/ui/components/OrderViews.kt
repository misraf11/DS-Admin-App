package com.digitalseba.admin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalseba.admin.data.Order
import com.digitalseba.admin.data.Product
import com.digitalseba.admin.data.SyncLog
import com.digitalseba.admin.ui.Fmt
import com.digitalseba.admin.ui.theme.Brand
import com.digitalseba.admin.ui.theme.LocalExtra
import com.digitalseba.admin.ui.theme.NumberStyle
import com.digitalseba.admin.ui.theme.classColor
import com.digitalseba.admin.ui.theme.statusColor

/** Colorful order card with WhatsApp shortcuts — the main building block of order lists. */
@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit,
    onWhatsApp: (() -> Unit)? = null,
    onToggleMark: (() -> Unit)? = null,
    markBusy: Boolean = false,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val color = statusColor(order.status)
    Surface(
        onClick = onClick,
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, LocalExtra.current.cardBorder),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(color))
            Column(Modifier.weight(1f).padding(start = 13.dp, end = 14.dp, top = 13.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${order.number}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(8.dp))
                    StatusChip(order.status)
                    if (order.isCod) {
                        Spacer(Modifier.width(6.dp))
                        Pill("COD", Brand.Pink)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(Fmt.money(order.total), style = NumberStyle, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Inventory2, null, tint = Brand.Violet, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        order.itemsPrimary.ifBlank { "Product details unavailable" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (order.itemsSecondary.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(order.itemsSecondary, style = MaterialTheme.typography.labelSmall, color = Brand.Violet, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Person, null, tint = LocalExtra.current.muted, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        order.customerName.ifBlank { "Guest customer" } + " · " + order.customerPhone.ifBlank { "No phone" },
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalExtra.current.muted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, tint = LocalExtra.current.muted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(Fmt.ago(order.createdTs), style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted)
                    Spacer(Modifier.weight(1f))
                    if (onToggleMark != null) {
                        WhatsAppMark(sentTs = order.whatsappSentTs, busy = markBusy, onClick = onToggleMark)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (onWhatsApp != null) {
                        WhatsAppButton(enabled = order.customerPhone.isNotBlank(), onClick = onWhatsApp)
                    }
                    if (trailingAction != null) {
                        Spacer(Modifier.width(8.dp))
                        trailingAction()
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsAppButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (enabled) Brand.WhatsApp else LocalExtra.current.subtle)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.AutoMirrored.Rounded.Chat, "WhatsApp", tint = if (enabled) Color.White else LocalExtra.current.muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun WhatsAppMark(sentTs: Long?, busy: Boolean, onClick: () -> Unit) {
    val sent = sentTs != null
    val c = if (sent) Brand.WhatsAppDark else LocalExtra.current.muted
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(50))
            .background(if (sent) Brand.WhatsApp.copy(alpha = 0.16f) else LocalExtra.current.subtle)
            .clickable(enabled = !busy, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(14.dp), color = c, strokeWidth = 2.dp)
        else Icon(if (sent) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = c, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            if (sent) "Sent · ${Fmt.ago(sentTs)}" else "Mark sent",
            color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1
        )
    }
}

@Composable
fun ProductRow(p: Product, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductImage(p.imageUrl, p.name, 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("SKU: " + p.sku.ifBlank { "-" }, style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted)
        }
        Spacer(Modifier.width(8.dp))
        if (trailing != null) trailing() else StockBadge(p)
    }
}

@Composable
fun StockBadge(p: Product) {
    val c = classColor(p.stockClass)
    val label = when {
        p.stockQuantity != null -> "${p.stockQuantity}"
        p.stockStatus == "instock" -> "In stock"
        p.stockStatus == "outofstock" -> "Out"
        else -> p.stockStatus.ifBlank { "n/a" }
    }
    Pill(label, c)
}

@Composable
fun SyncLogRow(l: SyncLog) {
    val c = when (l.status) {
        "completed" -> Brand.Green
        "failed" -> Brand.Rose
        else -> Brand.Blue
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(c))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(l.type, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text("${l.processed} records · " + Fmt.date(l.startedTs), style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted)
        }
        Pill(l.status, c)
    }
}

@Composable
fun LineDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(LocalExtra.current.cardBorder))
}

@Composable
fun StatusArrangedRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { content() }
}
