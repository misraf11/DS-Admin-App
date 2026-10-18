package com.digitalseba.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalseba.admin.BuildConfig
import com.digitalseba.admin.data.LiveHub
import com.digitalseba.admin.data.Prefs
import com.digitalseba.admin.ui.Nav
import com.digitalseba.admin.ui.components.*
import com.digitalseba.admin.ui.theme.*

private data class MoreItem(val title: String, val sub: String, val icon: ImageVector, val tone: Tone, val route: String)

@Composable
fun MoreScreen(nav: Nav, onLogout: () -> Unit) {
    val live by LiveHub.live.collectAsState()
    var confirmLogout by remember { mutableStateOf(false) }
    val items = listOf(
        MoreItem("WhatsApp Center", "COD follow-up queue", Icons.AutoMirrored.Rounded.Chat, Tone.Green, "whatsapp"),
        MoreItem("Notifications", "Activity feed", Icons.Rounded.Notifications, Tone.Amber, "notifications"),
        MoreItem("Reports", "Business insights", Icons.Rounded.BarChart, Tone.Purple, "reports"),
        MoreItem("Sync Logs", "Sync history", Icons.Rounded.History, Tone.Teal, "synclogs"),
        MoreItem("Store Health", "Diagnostics", Icons.Rounded.Healing, Tone.Red, "health"),
        MoreItem("Settings", "Template, realtime", Icons.Rounded.Settings, Tone.Blue, "settings"),
        MoreItem("Connect Store", "WooCommerce API", Icons.Rounded.Link, Tone.Indigo, "connect"),
        MoreItem("Search", "Order, product, customer", Icons.Rounded.Search, Tone.Pink, "search"),
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            ScreenHeader(
                title = "আরও",
                subtitle = "সব টুলস এক জায়গায়",
                bottom = {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.16f)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                            Text(Prefs.userName.firstOrNull()?.uppercase() ?: "A", color = Brand.Violet, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(Prefs.userName.ifBlank { "Admin" }, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(Prefs.userEmail, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text((live?.store?.name ?: Prefs.storeName).ifBlank { "Store" } + " · " + (live?.store?.url ?: "").removePrefix("https://"), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
        items.chunked(2).forEach { pair ->
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { m ->
                        val badge = if (m.route == "notifications") live?.badges?.unreadNotifications ?: 0 else if (m.route == "whatsapp") live?.badges?.pendingOrders ?: 0 else 0
                        MoreTile(m, badge, Modifier.weight(1f)) { nav.go(m.route) }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(10.dp))
            SoftButton("Logout", Brand.Rose, Modifier.fillMaxWidth().padding(horizontal = 16.dp), icon = Icons.AutoMirrored.Rounded.Logout) { confirmLogout = true }
            Spacer(Modifier.height(12.dp))
            Text(
                "DS Admin v${BuildConfig.VERSION_NAME} · WooControl",
                style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted,
                modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Logout করবেন?") },
            text = { Text("এই ডিভাইস থেকে লগআউট হবে এবং অর্ডার অ্যালার্ট বন্ধ হবে।") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Logout", color = Brand.Rose, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("না") } }
        )
    }
}

@Composable
private fun MoreTile(m: MoreItem, badge: Int, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(128.dp),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, LocalExtra.current.cardBorder),
        shadowElevation = 1.dp,
    ) {
        Box {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Box(
                    Modifier.size(46.dp).shadow(8.dp, RoundedCornerShape(15.dp), spotColor = m.tone.end).clip(RoundedCornerShape(15.dp)).background(m.tone.brush),
                    contentAlignment = Alignment.Center
                ) { Icon(m.icon, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.weight(1f))
                Text(m.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(m.sub, style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted, maxLines = 1)
            }
            if (badge > 0) {
                Text(
                    if (badge > 99) "99+" else badge.toString(),
                    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(50)).background(Brand.Rose).padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
