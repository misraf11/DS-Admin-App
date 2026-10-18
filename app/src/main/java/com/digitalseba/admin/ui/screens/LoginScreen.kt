package com.digitalseba.admin.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalseba.admin.R
import com.digitalseba.admin.data.Api
import com.digitalseba.admin.data.LiveHub
import com.digitalseba.admin.data.Prefs
import com.digitalseba.admin.service.LiveAlertService
import com.digitalseba.admin.ui.components.GradientButton
import com.digitalseba.admin.ui.components.InfoBanner
import com.digitalseba.admin.ui.theme.Brand
import com.digitalseba.admin.ui.theme.BrandGradient
import com.digitalseba.admin.ui.theme.LocalExtra
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(Prefs.userEmail) }
    var password by remember { mutableStateOf("") }
    var server by remember { mutableStateOf(Prefs.server) }
    var showPass by remember { mutableStateOf(false) }
    var showServer by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (loading) return
        if (email.isBlank() || password.isBlank()) {
            error = "ইমেইল ও পাসওয়ার্ড দিন।"
            return
        }
        loading = true
        error = null
        scope.launch {
            try {
                val s = Api.normalizeServer(server)
                val r = Api.login(s, email.trim(), password, "${Build.MANUFACTURER} ${Build.MODEL}")
                Prefs.server = s
                Prefs.token = r.token
                Prefs.userName = r.user.name
                Prefs.userEmail = r.user.email
                Prefs.storeName = r.store?.name.orEmpty()
                Prefs.lastOrderId = 0
                LiveHub.reset()
                LiveHub.start(context)
                LiveAlertService.start(context)
                onLoggedIn()
            } catch (e: Exception) {
                error = e.message ?: "লগইন হয়নি"
            } finally {
                loading = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(BrandGradient)) {
        Box(Modifier.size(260.dp).offset(x = 200.dp, y = (-70).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)))
        Box(Modifier.size(180.dp).offset(x = (-60).dp, y = 260.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)))
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Box(
                Modifier.size(92.dp).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, modifier = Modifier.size(110.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("DS Admin", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "ডিজিটাল সেবা · লাইভ অর্ডার কন্ট্রোল প্যানেল",
                color = Color.White.copy(alpha = 0.88f), fontSize = 14.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("লগইন করুন", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text("ওয়েব ড্যাশবোর্ডের একই ইমেইল ও পাসওয়ার্ড ব্যবহার করুন।", style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.muted)
                    val colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = LocalExtra.current.cardBorder, focusedBorderColor = Brand.Violet)
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Rounded.Email, null, tint = Brand.Violet) },
                        singleLine = true, shape = RoundedCornerShape(16.dp), colors = colors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = Brand.Violet) },
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Show password")
                            }
                        },
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true, shape = RoundedCornerShape(16.dp), colors = colors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        Modifier.clip(RoundedCornerShape(10.dp)).clickable { showServer = !showServer }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Dns, null, tint = LocalExtra.current.muted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("সার্ভার: " + Api.normalizeServer(server).removePrefix("https://").removePrefix("http://"), style = MaterialTheme.typography.labelMedium, color = LocalExtra.current.muted)
                        Spacer(Modifier.width(6.dp))
                        Text(if (showServer) "লুকান" else "বদলান", style = MaterialTheme.typography.labelMedium, color = Brand.Violet, fontWeight = FontWeight.Bold)
                    }
                    AnimatedVisibility(showServer) {
                        OutlinedTextField(
                            value = server, onValueChange = { server = it },
                            label = { Text("Dashboard URL") },
                            placeholder = { Text(Prefs.DEFAULT_SERVER) },
                            singleLine = true, shape = RoundedCornerShape(16.dp), colors = colors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (error != null) InfoBanner(error!!, Brand.Rose)
                    GradientButton("লগইন", icon = Icons.AutoMirrored.Rounded.Login, loading = loading, modifier = Modifier.fillMaxWidth(), onClick = { submit() })
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("নিরাপদ টোকেন লগইন · রিয়েলটাইম সিঙ্ক", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}
