package com.digitalseba.admin.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.digitalseba.admin.ui.theme.Brand
import com.digitalseba.admin.ui.theme.BrandGradient
import com.digitalseba.admin.ui.theme.LocalExtra
import com.digitalseba.admin.ui.theme.NumberStyle
import com.digitalseba.admin.ui.theme.Tone
import com.digitalseba.admin.ui.theme.statusColor
import com.digitalseba.admin.ui.theme.statusLabel

val CardShape = RoundedCornerShape(22.dp)

/* --------------------------------------------------------------- header */

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    brush: Brush = BrandGradient,
    actions: @Composable RowScope.() -> Unit = {},
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .background(brush)
    ) {
        // soft decorative bubbles
        Box(Modifier.size(170.dp).offset(x = 250.dp, y = (-60).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)))
        Box(Modifier.size(110.dp).offset(x = (-40).dp, y = 70.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)))
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = if (bottom != null) 16.dp else 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    HeaderIcon(Icons.AutoMirrored.Rounded.ArrowBack, onClick = onBack)
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) {
                        Text(subtitle, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
            }
            if (bottom != null) {
                Spacer(Modifier.height(14.dp))
                bottom()
            }
        }
    }
}

@Composable
fun HeaderIcon(icon: ImageVector, badge: Int = 0, loading: Boolean = false, onClick: () -> Unit) {
    Box {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        if (badge > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-2).dp)
                    .clip(CircleShape)
                    .background(Brand.Rose)
                    .border(1.5.dp, Color.White, CircleShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(if (badge > 99) "99+" else badge.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Translucent pill buttons used inside the gradient header. */
@Composable
fun HeaderChipButton(text: String, icon: ImageVector, loading: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(15.dp), color = Color.White, strokeWidth = 2.dp)
        else Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun LivePill(text: String, online: Boolean) {
    val t = rememberInfiniteTransition(label = "live")
    val a by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a")
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).alpha(if (online) a else 1f).clip(CircleShape).background(if (online) Color(0xFF4CF29A) else Color(0xFFFFB4B4)))
        Spacer(Modifier.width(6.dp))
        Text(if (online) text else "Offline", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/* ---------------------------------------------------------------- tiles */

@Composable
fun MetricTile(
    label: String,
    value: String,
    sub: String,
    tone: Tone,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier
            .height(124.dp)
            .shadow(10.dp, CardShape, ambientColor = tone.end.copy(alpha = 0.5f), spotColor = tone.end.copy(alpha = 0.5f))
            .clip(CardShape)
            .background(tone.brush)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Box(Modifier.size(96.dp).offset(x = 98.dp, y = (-30).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)))
        Box(Modifier.size(60.dp).offset(x = 120.dp, y = 76.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)))
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(label, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.weight(1f))
            AutoText(value, color = Color.White, maxSize = 24.sp)
            Text(sub, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Shrinks long numbers so they always fit on one line. */
@Composable
fun AutoText(text: String, color: Color, maxSize: TextUnit = 24.sp) {
    val size = when {
        text.length > 14 -> (maxSize.value * 0.66f).sp
        text.length > 11 -> (maxSize.value * 0.78f).sp
        text.length > 9 -> (maxSize.value * 0.88f).sp
        else -> maxSize
    }
    Text(text, color = color, style = NumberStyle, fontSize = size, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
}

@Composable
fun MiniStat(label: String, value: String, tone: Tone, icon: ImageVector, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, LocalExtra.current.cardBorder),
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tone.brush), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, style = NumberStyle, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(label, style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/* ---------------------------------------------------------------- cards */

@Composable
fun SectionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    accent: Color = Brand.Violet,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, LocalExtra.current.cardBorder),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(4.dp)).background(accent))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.muted)
                }
                if (action != null) action()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun PlainCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, LocalExtra.current.cardBorder),
        shadowElevation = 1.dp,
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun SmallLinkButton(text: String, color: Color = Brand.Violet, onClick: () -> Unit) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

/* --------------------------------------------------------------- chips */

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val c = statusColor(status)
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.13f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(c))
        Spacer(Modifier.width(5.dp))
        Text(statusLabel(status), color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, filled: Boolean = false) {
    Text(
        text,
        color = if (filled) Color.White else color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) color else color.copy(alpha = 0.13f))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

@Composable
fun ToneChip(label: String, selected: Boolean, color: Color, count: Int? = null, onClick: () -> Unit) {
    val bg = if (selected) color else MaterialTheme.colorScheme.surface
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, if (selected) color else LocalExtra.current.cardBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!selected) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                count.toString(),
                color = if (selected) color else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) Color.White else color)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

/* --------------------------------------------------------------- inputs */

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, onSearch: () -> Unit = {}) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Brand.Violet) },
        trailingIcon = {
            if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Rounded.Close, "Clear") }
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = LocalExtra.current.cardBorder,
            focusedBorderColor = Brand.Violet,
        )
    )
}

@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    brush: Brush = BrandGradient,
    height: Dp = 52.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) brush else Brush.linearGradient(listOf(Color(0xFFB8BCD0), Color(0xFFA5AAC2))))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            if (loading || icon != null) Spacer(Modifier.width(8.dp))
            Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge, fontSize = 15.sp, maxLines = 1)
        }
    }
}

@Composable
fun SoftButton(text: String, color: Color, modifier: Modifier = Modifier, icon: ImageVector? = null, loading: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(16.dp), color = color, strokeWidth = 2.dp)
        else if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        if (loading || icon != null) Spacer(Modifier.width(6.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/* --------------------------------------------------------------- states */

@Composable
fun EmptyState(icon: ImageVector = Icons.Rounded.Inventory2, title: String, text: String, tone: Tone = Tone.Purple, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(tone.brush), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = LocalExtra.current.muted, modifier = Modifier.widthIn(max = 300.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(14.dp))
            action()
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    EmptyState(icon = Icons.Rounded.ErrorOutline, title = "লোড করা যায়নি", text = message, tone = Tone.Red) {
        SoftButton("আবার চেষ্টা করুন", Brand.Rose, icon = Icons.Rounded.Refresh, onClick = onRetry)
    }
}

@Composable
fun SkeletonList(count: Int = 5, contentPadding: PaddingValues = PaddingValues(16.dp)) {
    val t = rememberInfiniteTransition(label = "sk")
    val a by t.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Column(Modifier.fillMaxWidth().padding(contentPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) { Box(Modifier.weight(1f).height(110.dp).alpha(a).clip(CardShape).background(LocalExtra.current.subtle)) }
        }
        repeat(count) {
            Box(Modifier.fillMaxWidth().height(96.dp).alpha(a).clip(CardShape).background(LocalExtra.current.subtle))
        }
    }
}

@Composable
fun InfoBanner(text: String, color: Color, modifier: Modifier = Modifier, icon: ImageVector = Icons.Rounded.ErrorOutline, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.11f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (action != null) {
            Spacer(Modifier.width(8.dp))
            action()
        }
    }
}

/* --------------------------------------------------------------- misc */

private val avatarTones = listOf(Tone.Blue, Tone.Purple, Tone.Pink, Tone.Teal, Tone.Amber, Tone.Green, Tone.Indigo, Tone.Red)

@Composable
fun Avatar(name: String, size: Dp = 44.dp) {
    val letter = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "G"
    val tone = avatarTones[(name.hashCode() and 0x7fffffff) % avatarTones.size]
    Box(Modifier.size(size).clip(CircleShape).background(tone.brush), contentAlignment = Alignment.Center) {
        Text(letter, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.42f).sp)
    }
}

@Composable
fun ProductImage(url: String, name: String, size: Dp = 52.dp) {
    if (url.isNotBlank()) {
        AsyncImage(
            model = url,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(LocalExtra.current.subtle)
        )
    } else {
        val tone = avatarTones[(name.hashCode() and 0x7fffffff) % avatarTones.size]
        Box(Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(tone.brush), contentAlignment = Alignment.Center) {
            Text(name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "P", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
    }
}

@Composable
fun KeyValue(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(LocalExtra.current.subtle)
            .padding(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun IconCircle(icon: ImageVector, color: Color, size: Dp = 40.dp, onClick: (() -> Unit)? = null) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.13f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun BoxScope.CheckBadge() {
    Box(Modifier.align(Alignment.BottomEnd).size(16.dp).clip(CircleShape).background(Brand.Green), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(11.dp))
    }
}

@Composable
fun BarRow(label: String, valueText: String, fraction: Float, color: Color) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(valueText, style = MaterialTheme.typography.bodySmall, color = LocalExtra.current.muted)
        }
        Spacer(Modifier.height(6.dp))
        val bg = LocalExtra.current.subtle
        Canvas(Modifier.fillMaxWidth().height(10.dp)) {
            val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2, size.height / 2)
            drawRoundRect(color = bg, cornerRadius = r)
            val w = (size.width * fraction.coerceIn(0f, 1f)).coerceAtLeast(size.height)
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color)),
                size = androidx.compose.ui.geometry.Size(w, size.height),
                cornerRadius = r
            )
        }
    }
}
