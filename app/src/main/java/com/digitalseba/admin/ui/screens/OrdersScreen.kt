package com.digitalseba.admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.digitalseba.admin.data.Api
import com.digitalseba.admin.data.LiveHub
import com.digitalseba.admin.data.Order
import com.digitalseba.admin.ui.Intents
import com.digitalseba.admin.ui.Nav
import com.digitalseba.admin.ui.components.*
import com.digitalseba.admin.ui.rememberActions
import com.digitalseba.admin.ui.theme.Brand
import com.digitalseba.admin.ui.theme.LocalExtra
import com.digitalseba.admin.ui.theme.Tone
import com.digitalseba.admin.ui.theme.statusColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private val statusTabs = listOf("all", "pending", "processing", "on-hold", "completed", "cancelled")
private val tabLabels = mapOf("all" to "All", "pending" to "Pending", "processing" to "Processing", "on-hold" to "On Hold", "completed" to "Completed", "cancelled" to "Cancelled")

/** Paged, filterable, live-refreshing order list state. */
class OrdersState(private val scope: CoroutineScope) {
    var status by mutableStateOf("all")
    var period by mutableStateOf("")
    var query by mutableStateOf("")
    val orders = mutableStateListOf<Order>()
    var counts by mutableStateOf<Map<String, Int>>(emptyMap())
    var total by mutableStateOf(0)
    var page by mutableStateOf(0)
    var hasMore by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var loadingMore by mutableStateOf(false)
    var refreshing by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    private var job: Job? = null

    fun reload(pull: Boolean = false) {
        job?.cancel()
        job = scope.launch {
            if (pull) refreshing = true else if (orders.isEmpty()) loading = true
            try {
                val r = Api.orders(status, query.trim(), period, 1)
                orders.clear(); orders.addAll(r.orders)
                counts = r.counts; total = r.total; page = 1; hasMore = r.hasMore; error = null
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false; refreshing = false
            }
        }
    }

    fun loadMore() {
        if (!hasMore || loadingMore || job?.isActive == true) return
        job = scope.launch {
            loadingMore = true
            try {
                val r = Api.orders(status, query.trim(), period, page + 1)
                val known = orders.map { it.id }.toHashSet()
                orders.addAll(r.orders.filter { it.id !in known })
                counts = r.counts; total = r.total; page += 1; hasMore = r.hasMore
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
            } finally {
                loadingMore = false
            }
        }
    }

    /** Silent live refresh of the pages already on screen. */
    fun liveRefresh() {
        if (job?.isActive == true) return
        job = scope.launch {
            try {
                val pages = page.coerceIn(1, 3)
                val fresh = mutableListOf<Order>()
                var more = false
                for (p in 1..pages) {
                    val r = Api.orders(status, query.trim(), period, p)
                    fresh.addAll(r.orders)
                    counts = r.counts; total = r.total; more = r.hasMore
                }
                val tail = if (page > pages) orders.drop(fresh.size) else emptyList()
                orders.clear(); orders.addAll(fresh); orders.addAll(tail.filter { t -> fresh.none { it.id == t.id } })
                if (page <= pages) { page = pages; hasMore = more }
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
            }
        }
    }

    fun replace(o: Order) {
        val i = orders.indexOfFirst { it.id == o.id }
        if (i >= 0) orders[i] = o
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(nav: Nav, initialStatus: String, initialPeriod: String) {
    val scope = rememberCoroutineScope()
    val state = remember {
        OrdersState(scope).apply {
            status = initialStatus.ifBlank { "all" }
            period = initialPeriod
        }
    }
    val actions = rememberActions()
    val ctx = LocalContext.current
    val listState = rememberLazyListState()
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        state.reload()
        LiveHub.tick.drop(1).collect { state.liveRefresh() }
    }
    LaunchedEffect(searchText) {
        if (searchText == state.query) return@LaunchedEffect
        delay(450)
        state.query = searchText
        state.reload()
    }
    // infinite scroll
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd, state.hasMore) { if (nearEnd && state.hasMore) state.loadMore() }

    fun toggleMark(o: Order) {
        val mark = o.whatsappSentTs == null
        actions.run("m${o.id}") {
            val r = Api.markWhatsApp(o.id, mark)
            state.replace(o.copy(whatsappSentTs = r.whatsappSentTs))
            r.message
        }
    }

    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = { state.reload(pull = true) }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ScreenHeader(
                    title = "Orders",
                    subtitle = "Review, fulfill এবং update · ${state.total} orders",
                    actions = {
                        HeaderIcon(Icons.Rounded.Sync, loading = actions.isBusy("sync")) {
                            actions.run("sync", onDone = { state.reload() }) { Api.run("sync_orders").message }
                        }
                    },
                    bottom = {
                        SearchField(searchText, { searchText = it }, "Search orders, products, customers, phone")
                    }
                )
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("" to "All dates", "today" to "Today (BD)", "yesterday" to "Yesterday (BD)")) { (key, label) ->
                        ToneChip(label, state.period == key, Brand.Violet) {
                            if (state.period != key) { state.period = key; state.reload() }
                        }
                    }
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statusTabs) { key ->
                        val color = if (key == "all") Brand.Violet else statusColor(key)
                        ToneChip(tabLabels[key] ?: key, state.status == key, color, count = state.counts[key] ?: 0) {
                            if (state.status != key) { state.status = key; state.reload() }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            when {
                state.loading && state.orders.isEmpty() -> item { SkeletonList(4, PaddingValues(horizontal = 16.dp)) }
                state.error != null && state.orders.isEmpty() -> item { ErrorState(state.error!!) { state.reload() } }
                state.orders.isEmpty() -> item {
                    EmptyState(Icons.Rounded.ShoppingBag, "No orders found", "Search বা filter বদলে দেখুন, অথবা store থেকে Sync করুন।", Tone.Blue) {
                        SoftButton("Sync", Brand.Blue, icon = Icons.Rounded.Sync, loading = actions.isBusy("sync")) {
                            actions.run("sync", onDone = { state.reload() }) { Api.run("sync_orders").message }
                        }
                    }
                }
                else -> {
                    items(state.orders, key = { it.id }) { o ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            OrderCard(
                                o,
                                onClick = { nav.order(o.id) },
                                onWhatsApp = { Intents.open(ctx, o.whatsappUrl) },
                                onToggleMark = { toggleMark(o) },
                                markBusy = actions.isBusy("m${o.id}"),
                            )
                        }
                    }
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            when {
                                state.loadingMore -> CircularProgressIndicator(Modifier.size(28.dp), color = Brand.Violet, strokeWidth = 3.dp)
                                state.hasMore -> SoftButton("আরও লোড করুন", Brand.Violet) { state.loadMore() }
                                else -> Text("Showing ${state.orders.size} of ${state.total} orders", style = MaterialTheme.typography.labelMedium, color = LocalExtra.current.muted)
                            }
                        }
                    }
                }
            }
        }
    }
}
