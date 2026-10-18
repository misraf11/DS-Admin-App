package com.digitalseba.admin.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.digitalseba.admin.BuildConfig
import com.digitalseba.admin.data.Api
import com.digitalseba.admin.data.Prefs
import com.digitalseba.admin.service.LiveAlertService
import com.digitalseba.admin.ui.Fmt
import com.digitalseba.admin.ui.Intents
import com.digitalseba.admin.ui.LocalNotifier
import com.digitalseba.admin.ui.Nav
import com.digitalseba.admin.ui.components.*
import com.digitalseba.admin.ui.rememberActions
import com.digitalseba.admin.ui.rememberLive
import com.digitalseba.admin.ui.theme.*

@SuppressLint("BatteryLife")
@Composable
fun SettingsScreen(nav: Nav, onLogout: () -> Unit) {
    val loader = rememberLive { Api.settings() }
    val actions = rememberActions()
    val notifier = LocalNotifier.current
    val ctx = LocalContext.current
    val theme by Prefs.theme.collectAsState()
    val d = loader.data
    var template by remember { mutableStateOf(TextFieldValue("")) }
    var templateLoaded by remember { mutableStateOf(false) }
    var bgAlerts by remember { mutableStateOf(Prefs.backgroundAlerts) }
    var confirmLogout by remember { mutableStateOf(false) }

    LaunchedEffect(d?.whatsappTemplate) {
        if (d != null && !templateLoaded) {
            template = TextFieldValue(d.whatsappTemplate)
            templateLoaded = true
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) notifier.error("Notification permission ছাড়া অ্যালার্ট দেখানো যাবে না।")
    }

    fun setBgAlerts(on: Boolean) {
        bgAlerts = on
        Prefs.backgroundAlerts = on
        if (on) {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            LiveAlertService.start(ctx)
            notifier.success("ব্যাকগ্রাউন্ড অর্ডার অ্যালার্ট চালু হয়েছে")
        } else {
            LiveAlertService.stop(ctx)
            notifier.success("ব্যাকগ্রাউন্ড অ্যালার্ট বন্ধ")
        }
    }

    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            ScreenHeader(
                title = "Settings",
                subtitle = "Control center preferences",
                onBack = { nav.back() },
                brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF4F8BFF), Color(0xFF4F46E5), Color(0xFF7B3BFF)))
            )
        }
        if (d == null && loader.error != null) item { ErrorState(loader.error!!) { loader.load() } }
        if (d == null && loader.error == null) item { SkeletonList(2) }

        if (d != null) {
            item {
                SectionCard("Store settings", "Current store information", Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), accent = Brand.Blue) {
                    KeyValue("Store name", d.store?.name.orEmpty().ifBlank { "-" }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    KeyValue("Store URL", d.store?.url.orEmpty().ifBlank { "-" }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    GradientButton("Reconnect Store", Modifier.fillMaxWidth(), icon = Icons.Rounded.Link, height = 46.dp) { nav.go("connect") }
                }
            }
            item {
                val active = d.store?.realtimeStatus == "active"
                SectionCard("Instant sync", "Six WooCommerce webhooks auto-create/repair হয়। Cron থাকে safety fallback।", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), accent = Brand.Green) {
                    InfoBanner(
                        "Status: " + (d.store?.realtimeStatus ?: "connecting").replaceFirstChar { it.uppercase() } + " · Last webhook " + Fmt.date(d.store?.lastWebhookTs),
                        if (active) Brand.Green else Brand.Amber,
                        icon = if (active) Icons.Rounded.Bolt else Icons.Rounded.WarningAmber
                    )
                    Spacer(Modifier.height(10.dp))
                    GradientButton("Repair / Enable Realtime", Modifier.fillMaxWidth(), icon = Icons.Rounded.Bolt, loading = actions.isBusy("rt"), brush = Tone.Green.brush, height = 46.dp) {
                        actions.run("rt", onDone = { loader.load(silent = true) }) { Api.run("setup_realtime").message }
                    }
                    Spacer(Modifier.height(12.dp))
                    CopyField("Webhook URL", d.webhookUrl) { Intents.copy(ctx, d.webhookUrl) }
                    Spacer(Modifier.height(8.dp))
                    CopyField("Cron fallback URL", d.cronUrl) { Intents.copy(ctx, d.cronUrl) }
                    Spacer(Modifier.height(10.dp))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(LocalExtra.current.subtle).padding(12.dp)) {
                        Text("এই WooCommerce webhooks লাগবে", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        listOf(
                            "Order created, Order updated, Order deleted",
                            "Product created, Product updated, Product deleted",
                            "প্রতিটি event-এ উপরের একই Webhook URL দিন।",
                            "Visitor plugin endpoint: শেষে &topic=traffic.updated যোগ করে daily totals POST করুন।",
                        ).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.muted) }
                    }
                }
            }
            item {
                SectionCard("WhatsApp message template", "Order-এর WhatsApp বাটন এই template ব্যবহার করবে", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), accent = Brand.WhatsApp) {
                    OutlinedTextField(
                        value = template,
                        onValueChange = { template = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                        shape = RoundedCornerShape(16.dp),
                        label = { Text("Message text") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand.WhatsAppDark, unfocusedBorderColor = LocalExtra.current.cardBorder)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Placeholder ট্যাপ করে যোগ করুন", style = MaterialTheme.typography.labelMedium, color = LocalExtra.current.muted)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(d.placeholders) { p ->
                            Column(
                                Modifier.clip(RoundedCornerShape(12.dp)).background(Brand.WhatsApp.copy(alpha = 0.12f))
                                    .clickable {
                                        val t = template.text
                                        val start = template.selection.start.coerceIn(0, t.length)
                                        val end = template.selection.end.coerceIn(start, t.length)
                                        val nt = t.substring(0, start) + p.key + t.substring(end)
                                        template = TextFieldValue(nt, TextRange(start + p.key.length))
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(p.key, style = MaterialTheme.typography.labelMedium, color = Brand.WhatsAppDark, fontWeight = FontWeight.Bold)
                                Text(p.label, style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Preview", style = MaterialTheme.typography.labelMedium, color = LocalExtra.current.muted)
                    Text(
                        previewTemplate(template.text, d.store?.name.orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(14.dp)).background(Brand.WhatsApp.copy(alpha = 0.08f)).padding(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SoftButton("Default", Brand.Slate, Modifier.weight(1f), icon = Icons.Rounded.RestartAlt) {
                            template = TextFieldValue(d.whatsappTemplateDefault)
                        }
                        GradientButton("Save template", Modifier.weight(2f), icon = Icons.Rounded.Save, loading = actions.isBusy("tpl"), brush = Tone.Green.brush, height = 44.dp) {
                            actions.run("tpl") { Api.saveTemplate(template.text).message }
                        }
                    }
                }
            }
        }

        item {
            SectionCard("App settings", "এই ফোনের জন্য", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), accent = Brand.Violet) {
                SettingRow(Icons.Rounded.NotificationsActive, Tone.Amber, "ব্যাকগ্রাউন্ড অর্ডার অ্যালার্ট", "অ্যাপ বন্ধ থাকলেও নতুন অর্ডারের নোটিফিকেশন") {
                    Switch(checked = bgAlerts, onCheckedChange = { setBgAlerts(it) }, colors = SwitchDefaults.colors(checkedTrackColor = Brand.Violet))
                }
                LineDivider()
                SettingRow(Icons.Rounded.BatteryChargingFull, Tone.Green, "Battery optimization বন্ধ করুন", "অ্যালার্ট যেন বন্ধ না হয়ে যায়", onClick = {
                    try {
                        val pm = ctx.getSystemService(PowerManager::class.java)
                        if (pm.isIgnoringBatteryOptimizations(ctx.packageName)) notifier.success("ইতিমধ্যে চালু আছে ✓")
                        else ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + ctx.packageName)))
                    } catch (e: Exception) {
                        ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                })
                LineDivider()
                SettingRow(Icons.Rounded.Tune, Tone.Blue, "Notification settings", "সাউন্ড, ভাইব্রেশন ঠিক করুন", onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                })
                LineDivider()
                Text("থিম", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToneChip("System", theme == "system", Brand.Violet) { Prefs.setTheme("system") }
                    ToneChip("Light", theme == "light", Brand.Amber) { Prefs.setTheme("light") }
                    ToneChip("Dark", theme == "dark", Brand.Blue) { Prefs.setTheme("dark") }
                }
                Spacer(Modifier.height(12.dp))
                KeyValue("Dashboard server", Prefs.server, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                KeyValue("Logged in as", (d?.user?.name ?: Prefs.userName) + " · " + (d?.user?.email ?: Prefs.userEmail), Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                KeyValue("App version", "DS Admin " + BuildConfig.VERSION_NAME, Modifier.fillMaxWidth())
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            SoftButton("Logout", Brand.Rose, Modifier.fillMaxWidth().padding(horizontal = 16.dp), icon = Icons.AutoMirrored.Rounded.Logout) { confirmLogout = true }
            Spacer(Modifier.navigationBarsPadding())
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

private fun previewTemplate(t: String, store: String): String = t
    .replace("{customer_name}", "Rahim")
    .replace("{order_number}", "1234")
    .replace("{total_amount}", "৳99.00")
    .replace("{product_name}", "CapCut Pro")
    .replace("{products}", "CapCut Pro ×1")
    .replace("{items_count}", "1")
    .replace("{products_summary}", "CapCut Pro ×1")
    .replace("{store_name}", store.ifBlank { "DigitalSeba" })
    .replace("{customer_phone}", "01XXXXXXXXX")
    .replace("{status}", "pending")
    .ifBlank { "—" }

@Composable
private fun CopyField(label: String, value: String, onCopy: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(LocalExtra.current.subtle).clickable(enabled = value.isNotBlank(), onClick = onCopy).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted)
            Text(value.ifBlank { "-" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ContentCopy, "Copy", tint = Brand.Violet)
    }
}

@Composable
private fun SettingRow(icon: ImageVector, tone: Tone, title: String, sub: String, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tone.brush), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted)
        }
        if (trailing != null) trailing() else Icon(Icons.Rounded.ChevronRight, null, tint = LocalExtra.current.muted)
    }
}
