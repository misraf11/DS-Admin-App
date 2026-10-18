package com.digitalseba.admin.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.digitalseba.admin.data.Api
import com.digitalseba.admin.data.AuthBus
import com.digitalseba.admin.data.LiveHub
import com.digitalseba.admin.data.Order
import com.digitalseba.admin.data.Prefs
import com.digitalseba.admin.service.LiveAlertService
import com.digitalseba.admin.ui.screens.ConnectScreen
import com.digitalseba.admin.ui.screens.CustomerOrdersScreen
import com.digitalseba.admin.ui.screens.CustomersScreen
import com.digitalseba.admin.ui.screens.HealthScreen
import com.digitalseba.admin.ui.screens.HomeScreen
import com.digitalseba.admin.ui.screens.LoginScreen
import com.digitalseba.admin.ui.screens.MoreScreen
import com.digitalseba.admin.ui.screens.NotificationsScreen
import com.digitalseba.admin.ui.screens.OrderDetailScreen
import com.digitalseba.admin.ui.screens.OrdersScreen
import com.digitalseba.admin.ui.screens.ProductsScreen
import com.digitalseba.admin.ui.screens.ReportsScreen
import com.digitalseba.admin.ui.screens.SearchScreen
import com.digitalseba.admin.ui.screens.SettingsScreen
import com.digitalseba.admin.ui.screens.SyncLogsScreen
import com.digitalseba.admin.ui.screens.WhatsAppScreen
import com.digitalseba.admin.ui.theme.Brand
import com.digitalseba.admin.ui.theme.BrandGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.net.Uri

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("home", "হোম", Icons.Rounded.Dashboard),
    Tab("orders", "অর্ডার", Icons.Rounded.ShoppingBag),
    Tab("products", "প্রোডাক্ট", Icons.Rounded.Inventory2),
    Tab("customers", "কাস্টমার", Icons.Rounded.People),
    Tab("more", "আরও", Icons.Rounded.GridView),
)

/** Navigation helper passed to every screen. */
class Nav(private val nav: NavHostController) {
    fun back() { nav.popBackStack() }
    fun order(id: Int) = nav.navigate("order/$id")
    fun orders(status: String = "", period: String = "") = nav.navigate("orders?status=$status&period=$period") { launchSingleTop = true }
    fun products(stock: String = "") = nav.navigate("products?stock=$stock") { launchSingleTop = true }
    fun customerOrders(name: String, phone: String, email: String) =
        nav.navigate("customer_orders?name=${Uri.encode(name)}&phone=${Uri.encode(phone)}&email=${Uri.encode(email)}")
    fun go(route: String) = nav.navigate(route) { launchSingleTop = true }
    fun tab(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    fun toLogin() = nav.navigate("login") {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
    fun toHome() = nav.navigate("home") {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun AppRoot(pendingOrder: Int?, onPendingHandled: () -> Unit) {
    val navController = rememberNavController()
    val nav = remember(navController) { Nav(navController) }
    val snackHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notifier = remember { Notifier(snackHost, scope) }
    val context = LocalContext.current
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val baseRoute = route.substringBefore("?").substringBefore("/")
    val live by LiveHub.live.collectAsState()
    var banner by remember { mutableStateOf<Order?>(null) }

    val logout: () -> Unit = {
        scope.launch {
            Api.logout()
            Prefs.clearSession()
            LiveHub.stop()
            LiveHub.reset()
            LiveAlertService.stop(context)
            nav.toLogin()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        AuthBus.expired.collect {
            LiveHub.stop()
            LiveHub.reset()
            LiveAlertService.stop(context)
            notifier.error("সেশন শেষ হয়েছে, আবার লগইন করুন।")
            nav.toLogin()
        }
    }
    LaunchedEffect(Unit) {
        LiveHub.newOrders.collect { list ->
            banner = list.lastOrNull()
        }
    }
    LaunchedEffect(banner) {
        if (banner != null) {
            delay(7000)
            banner = null
        }
    }
    LaunchedEffect(baseRoute) {
        if (baseRoute.isNotEmpty() && baseRoute != "login" && Build.VERSION.SDK_INT >= 33 && !Prefs.askedNotificationPermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Prefs.askedNotificationPermission = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(pendingOrder, baseRoute) {
        if (pendingOrder != null && Prefs.loggedIn && baseRoute.isNotEmpty() && baseRoute != "login") {
            if (pendingOrder > 0) nav.order(pendingOrder) else nav.orders()
            onPendingHandled()
        }
    }

    val showBar = baseRoute in tabs.map { it.route }

    CompositionLocalProvider(LocalNotifier provides notifier) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = {
                    SnackbarHost(snackHost) { data ->
                        val err = (data.visuals as? AppSnack)?.isError == true
                        Snackbar(
                            modifier = Modifier.padding(12.dp),
                            shape = RoundedCornerShape(16.dp),
                            containerColor = if (err) Brand.Rose else Color(0xFF1E2440),
                            contentColor = Color.White,
                        ) { Text(data.visuals.message) }
                    }
                },
                bottomBar = {
                    if (showBar) {
                        val badges = live?.badges
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                            tabs.forEach { t ->
                                val selected = baseRoute == t.route
                                val badge = when (t.route) {
                                    "orders" -> badges?.pendingOrders ?: 0
                                    "more" -> badges?.unreadNotifications ?: 0
                                    else -> 0
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { if (!selected) nav.tab(t.route) },
                                    icon = {
                                        BadgedBox(badge = { if (badge > 0) Badge(containerColor = Brand.Rose) { Text(if (badge > 99) "99+" else badge.toString()) } }) {
                                            Icon(t.icon, t.label)
                                        }
                                    },
                                    label = { Text(t.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = Brand.Violet,
                                        indicatorColor = Brand.Violet,
                                    )
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = if (Prefs.loggedIn) "home" else "login",
                    modifier = Modifier.padding(padding),
                ) {
                    composable("login") { LoginScreen(onLoggedIn = { nav.toHome() }) }
                    composable("home") { HomeScreen(nav) }
                    composable(
                        "orders?status={status}&period={period}",
                        arguments = listOf(
                            navArgument("status") { type = NavType.StringType; defaultValue = "" },
                            navArgument("period") { type = NavType.StringType; defaultValue = "" },
                        )
                    ) { e ->
                        OrdersScreen(nav, e.arguments?.getString("status").orEmpty(), e.arguments?.getString("period").orEmpty())
                    }
                    composable(
                        "products?stock={stock}",
                        arguments = listOf(navArgument("stock") { type = NavType.StringType; defaultValue = "" })
                    ) { e -> ProductsScreen(nav, e.arguments?.getString("stock").orEmpty()) }
                    composable("customers") { CustomersScreen(nav) }
                    composable("more") { MoreScreen(nav, onLogout = logout) }
                    composable("order/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { e ->
                        OrderDetailScreen(nav, e.arguments?.getInt("id") ?: 0)
                    }
                    composable(
                        "customer_orders?name={name}&phone={phone}&email={email}",
                        arguments = listOf(
                            navArgument("name") { type = NavType.StringType; defaultValue = "" },
                            navArgument("phone") { type = NavType.StringType; defaultValue = "" },
                            navArgument("email") { type = NavType.StringType; defaultValue = "" },
                        )
                    ) { e ->
                        CustomerOrdersScreen(
                            nav,
                            e.arguments?.getString("name").orEmpty(),
                            e.arguments?.getString("phone").orEmpty(),
                            e.arguments?.getString("email").orEmpty(),
                        )
                    }
                    composable("whatsapp") { WhatsAppScreen(nav) }
                    composable("notifications") { NotificationsScreen(nav) }
                    composable("reports") { ReportsScreen(nav) }
                    composable("synclogs") { SyncLogsScreen(nav) }
                    composable("health") { HealthScreen(nav) }
                    composable("settings") { SettingsScreen(nav, onLogout = logout) }
                    composable("connect") { ConnectScreen(nav) }
                    composable("search") { SearchScreen(nav) }
                }
            }

            // New-order banner (slides in from top)
            AnimatedVisibility(
                visible = banner != null && baseRoute != "login",
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                val o = banner
                if (o != null) NewOrderBanner(o, onOpen = { banner = null; nav.order(o.id) }, onClose = { banner = null })
            }
        }
    }
}

@Composable
private fun NewOrderBanner(o: Order, onOpen: () -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(BrandGradient)
                .clickable(onClick = onOpen)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                Text("🛒", fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("নতুন অর্ডার #${o.number}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text(
                    o.customerName.ifBlank { "Guest customer" } + " · " + Fmt.money(o.total),
                    color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Box(Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, "Close", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Standard padding for screen lists that sit above the bottom bar. */
val ListPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
