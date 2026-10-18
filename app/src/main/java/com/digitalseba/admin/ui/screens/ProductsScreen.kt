package com.digitalseba.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalseba.admin.data.Api
import com.digitalseba.admin.data.Product
import com.digitalseba.admin.ui.Fmt
import com.digitalseba.admin.ui.Nav
import com.digitalseba.admin.ui.components.*
import com.digitalseba.admin.ui.rememberActions
import com.digitalseba.admin.ui.rememberLive
import com.digitalseba.admin.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(nav: Nav, initialStock: String) {
    var stock by remember { mutableStateOf(initialStock) }
    var searchText by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(searchText) {
        delay(400)
        query = searchText.trim()
    }
    val loader = rememberLive(stock, query) { Api.products(query, stock) }
    val actions = rememberActions()
    var editing by remember { mutableStateOf<Product?>(null) }
    val d = loader.data

    PullToRefreshBox(isRefreshing = loader.refreshing, onRefresh = { loader.refresh() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ScreenHeader(
                    title = "Products & Inventory",
                    subtitle = "Stock status, price, SKU এবং quick stock update",
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF12B5A6), Color(0xFF2F6BFF), Color(0xFF6C3BFF))),
                    actions = {
                        HeaderIcon(Icons.Rounded.Sync, loading = actions.isBusy("sp")) {
                            actions.run("sp") { Api.run("sync_products").message }
                        }
                    },
                    bottom = { SearchField(searchText, { searchText = it }, "Search product or SKU…") }
                )
            }
            item {
                val s = d?.stats
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniStat("Total Products", Fmt.int(s?.totalProducts ?: 0), Tone.Blue, Icons.Rounded.Inventory2, Modifier.weight(1f)) { stock = "" }
                        MiniStat("In Stock", Fmt.int(s?.instock ?: 0), Tone.Green, Icons.Rounded.CheckCircle, Modifier.weight(1f)) { stock = "in" }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniStat("Low Stock", Fmt.int(s?.lowstock ?: 0), Tone.Amber, Icons.Rounded.WarningAmber, Modifier.weight(1f)) { stock = "low" }
                        MiniStat("Out of Stock", Fmt.int(s?.outstock ?: 0), Tone.Red, Icons.Rounded.RemoveShoppingCart, Modifier.weight(1f)) { stock = "out" }
                    }
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("" to ("All Stock" to Brand.Violet), "in" to ("In Stock" to Brand.Green), "low" to ("Low Stock" to Brand.Amber), "out" to ("Out of Stock" to Brand.Rose))) { (key, v) ->
                        ToneChip(v.first, stock == key, v.second) { stock = key }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            when {
                d == null && loader.error != null -> item { ErrorState(loader.error!!) { loader.load() } }
                d == null -> item { SkeletonList(4, PaddingValues(horizontal = 16.dp)) }
                d.products.isEmpty() -> item {
                    EmptyState(Icons.Rounded.Inventory2, "No products found", "Sync Products চাপুন অথবা filter reset করুন।", Tone.Teal) {
                        SoftButton("Sync Products", Brand.Teal, icon = Icons.Rounded.Sync, loading = actions.isBusy("sp")) {
                            actions.run("sp") { Api.run("sync_products").message }
                        }
                    }
                }
                else -> items(d.products, key = { it.id }) { p ->
                    ProductCard(p, Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { editing = p }
                }
            }
        }
    }

    val e = editing
    if (e != null) {
        StockDialog(
            product = e,
            busy = actions.isBusy("stock"),
            onDismiss = { editing = null },
            onSave = { qty ->
                actions.run("stock", onDone = { editing = null; loader.load(silent = true) }) { Api.updateStock(e.id, qty).message }
            }
        )
    }
}

@Composable
private fun ProductCard(p: Product, modifier: Modifier, onEdit: () -> Unit) {
    val c = classColor(p.stockClass)
    PlainCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProductImage(p.imageUrl, p.name, 64.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text((p.type.ifBlank { "product" }) + " · " + p.status.ifBlank { "status n/a" }, style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Fmt.money(p.price), style = NumberStyle, fontSize = 16.sp, color = Brand.Violet)
                    if (p.salePrice > 0 && p.regularPrice > p.salePrice) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            Fmt.money(p.regularPrice), fontSize = 12.sp, color = LocalExtra.current.muted,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            KeyValue("SKU", p.sku.ifBlank { "-" }, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(c.copy(alpha = 0.12f)).padding(12.dp)
            ) {
                Text("Stock", style = MaterialTheme.typography.labelSmall, color = c)
                Text(
                    (p.stockQuantity?.toString() ?: "-") + " · " + when (p.stockStatus) {
                        "instock" -> "In stock"; "outofstock" -> "Out of stock"; "onbackorder" -> "Backorder"; else -> p.stockStatus.ifBlank { "unknown" }
                    },
                    style = MaterialTheme.typography.titleSmall, color = c, maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(BrandGradient).clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Edit, "Edit stock", tint = Color.White) }
        }
    }
}

@Composable
private fun StockDialog(product: Product, busy: Boolean, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf((product.stockQuantity ?: 0).toString()) }
    val qty = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { ProductImage(product.imageUrl, product.name, 56.dp) },
        title = { Text("Stock update", textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(product.name, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, maxLines = 2)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepButton("−", Brand.Rose) { text = ((qty ?: 0) - 1).coerceAtLeast(0).toString() }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { v -> text = v.filter { it.isDigit() || it == '-' }.take(7) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.width(120.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    StepButton("+", Brand.Green) { text = ((qty ?: 0) + 1).toString() }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 5, 10, 50, 100).forEach { n ->
                        Pill(n.toString(), Brand.Violet, Modifier.clip(RoundedCornerShape(50)).clickable { text = n.toString() })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("WooCommerce store-এ সাথে সাথে আপডেট হবে।", style = MaterialTheme.typography.labelSmall, color = LocalExtra.current.muted)
            }
        },
        confirmButton = {
            GradientButton("Save", icon = Icons.Rounded.Check, loading = busy, enabled = qty != null, height = 44.dp) { qty?.let(onSave) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun StepButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
}
