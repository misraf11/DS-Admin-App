package com.digitalseba.admin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoreInfo(
    val id: Int = 0,
    val name: String = "",
    val url: String = "",
    @SerialName("connection_status") val connectionStatus: String = "",
    @SerialName("realtime_status") val realtimeStatus: String = "pending",
    val currency: String = "BDT",
    @SerialName("last_sync_ts") val lastSyncTs: Long? = null,
    @SerialName("last_order_sync_ts") val lastOrderSyncTs: Long? = null,
    @SerialName("last_product_sync_ts") val lastProductSyncTs: Long? = null,
    @SerialName("last_webhook_ts") val lastWebhookTs: Long? = null,
)

@Serializable
data class UserInfo(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val role: String = "owner",
)

@Serializable
data class Stats(
    @SerialName("total_orders") val totalOrders: Double = 0.0,
    @SerialName("total_sales") val totalSales: Double = 0.0,
    @SerialName("pending_orders") val pendingOrders: Double = 0.0,
    @SerialName("processing_orders") val processingOrders: Double = 0.0,
    @SerialName("completed_orders") val completedOrders: Double = 0.0,
    @SerialName("cancelled_orders") val cancelledOrders: Double = 0.0,
    @SerialName("cod_orders") val codOrders: Double = 0.0,
    @SerialName("today_orders") val todayOrders: Double = 0.0,
    @SerialName("today_sales") val todaySales: Double = 0.0,
    @SerialName("today_pending") val todayPending: Double = 0.0,
    @SerialName("today_completed") val todayCompleted: Double = 0.0,
    @SerialName("today_cancelled") val todayCancelled: Double = 0.0,
    @SerialName("yesterday_orders") val yesterdayOrders: Double = 0.0,
    @SerialName("yesterday_sales") val yesterdaySales: Double = 0.0,
    @SerialName("total_products") val totalProducts: Double = 0.0,
    val instock: Double = 0.0,
    val outstock: Double = 0.0,
    val lowstock: Double = 0.0,
    @SerialName("product_revenue") val productRevenue: Double = 0.0,
    @SerialName("visitors_today") val visitorsToday: Double = 0.0,
    @SerialName("pageviews_today") val pageviewsToday: Double = 0.0,
    @SerialName("sessions_today") val sessionsToday: Double = 0.0,
    @SerialName("traffic_connected") val trafficConnected: Double = 0.0,
    @SerialName("today_date") val todayDate: String = "",
)

@Serializable
data class Badges(
    @SerialName("pending_orders") val pendingOrders: Int = 0,
    @SerialName("unread_notifications") val unreadNotifications: Int = 0,
)

@Serializable
data class Order(
    val id: Int = 0,
    @SerialName("wc_order_id") val wcOrderId: Long = 0,
    val number: String = "",
    val status: String = "",
    val currency: String = "BDT",
    val total: Double = 0.0,
    @SerialName("discount_total") val discountTotal: Double = 0.0,
    @SerialName("shipping_total") val shippingTotal: Double = 0.0,
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("payment_title") val paymentTitle: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_phone") val customerPhone: String = "",
    @SerialName("customer_email") val customerEmail: String = "",
    @SerialName("billing_address") val billingAddress: String = "",
    @SerialName("shipping_address") val shippingAddress: String = "",
    @SerialName("customer_note") val customerNote: String = "",
    @SerialName("is_cod") val isCod: Boolean = false,
    @SerialName("whatsapp_sent_ts") val whatsappSentTs: Long? = null,
    @SerialName("created_ts") val createdTs: Long? = null,
    @SerialName("updated_ts") val updatedTs: Long? = null,
    @SerialName("whatsapp_message") val whatsappMessage: String = "",
    @SerialName("whatsapp_url") val whatsappUrl: String = "",
    @SerialName("items_primary") val itemsPrimary: String = "",
    @SerialName("items_secondary") val itemsSecondary: String = "",
    @SerialName("items_count") val itemsCount: Int = 0,
)

@Serializable
data class OrderItem(
    val id: Int = 0,
    @SerialName("product_id") val productId: Long = 0,
    val name: String = "",
    val sku: String = "",
    val quantity: Int = 1,
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
    @SerialName("image_url") val imageUrl: String = "",
)

@Serializable
data class Product(
    val id: Int = 0,
    @SerialName("wc_product_id") val wcProductId: Long = 0,
    val name: String = "",
    val sku: String = "",
    val type: String = "",
    val status: String = "",
    val price: Double = 0.0,
    @SerialName("regular_price") val regularPrice: Double = 0.0,
    @SerialName("sale_price") val salePrice: Double = 0.0,
    @SerialName("stock_quantity") val stockQuantity: Int? = null,
    @SerialName("stock_status") val stockStatus: String = "",
    @SerialName("stock_class") val stockClass: String = "neutral",
    @SerialName("manage_stock") val manageStock: Boolean = false,
    @SerialName("image_url") val imageUrl: String = "",
)

@Serializable
data class SyncLog(
    val id: Int = 0,
    val type: String = "",
    val status: String = "",
    val processed: Int = 0,
    val created: Int = 0,
    val updated: Int = 0,
    val error: String = "",
    @SerialName("started_ts") val startedTs: Long? = null,
    @SerialName("completed_ts") val completedTs: Long? = null,
)

@Serializable
data class Customer(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    @SerialName("orders_count") val ordersCount: Int = 0,
    @SerialName("total_spent") val totalSpent: Double = 0.0,
    @SerialName("cancelled_count") val cancelledCount: Int = 0,
    val risk: String = "low",
    @SerialName("last_order_ts") val lastOrderTs: Long? = null,
    @SerialName("whatsapp_url") val whatsappUrl: String = "",
)

@Serializable
data class AppNotification(
    val id: Int = 0,
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val status: String = "",
    val unread: Boolean = false,
    @SerialName("created_ts") val createdTs: Long? = null,
)

@Serializable
data class LiveSync(
    val attempted: Boolean = false,
    val ok: Boolean = true,
    val changed: Int = 0,
    val processed: Int = 0,
    val mode: String = "",
    val error: String = "",
)

/* ---------------------------------------------------------------- responses */

@Serializable
data class LoginResp(val token: String = "", val user: UserInfo = UserInfo(), val store: StoreInfo? = null)

@Serializable
data class MeResp(val user: UserInfo = UserInfo(), val store: StoreInfo? = null)

@Serializable
data class LiveResp(
    val version: String = "",
    @SerialName("server_time") val serverTime: Long = 0,
    @SerialName("day_key") val dayKey: String = "",
    @SerialName("live_channel") val liveChannel: String = "",
    @SerialName("live_sync") val liveSync: LiveSync = LiveSync(),
    @SerialName("max_order_id") val maxOrderId: Int = 0,
    @SerialName("latest_orders") val latestOrders: List<Order> = emptyList(),
    val badges: Badges = Badges(),
    val stats: Stats = Stats(),
    val store: StoreInfo? = null,
)

@Serializable
data class DashboardResp(
    val store: StoreInfo? = null,
    val stats: Stats = Stats(),
    val badges: Badges = Badges(),
    @SerialName("recent_orders") val recentOrders: List<Order> = emptyList(),
    @SerialName("urgent_orders") val urgentOrders: List<Order> = emptyList(),
    @SerialName("low_stock") val lowStock: List<Product> = emptyList(),
    @SerialName("sync_logs") val syncLogs: List<SyncLog> = emptyList(),
    @SerialName("missing_columns") val missingColumns: List<String> = emptyList(),
)

@Serializable
data class OrdersResp(
    val orders: List<Order> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val page: Int = 1,
    val total: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class OrderDetailResp(
    val order: Order = Order(),
    val items: List<OrderItem> = emptyList(),
    @SerialName("next_status") val nextStatus: String? = null,
    @SerialName("can_cancel") val canCancel: Boolean = false,
    val statuses: List<String> = emptyList(),
)

@Serializable
data class ActionResp(
    val message: String = "",
    val order: Order? = null,
    val product: Product? = null,
    val marked: Boolean? = null,
    @SerialName("whatsapp_sent_ts") val whatsappSentTs: Long? = null,
    val changed: Int = 0,
    @SerialName("whatsapp_template") val whatsappTemplate: String? = null,
    val badges: Badges? = null,
    val store: StoreInfo? = null,
)

@Serializable
data class ProductStats(
    @SerialName("total_products") val totalProducts: Int = 0,
    val instock: Int = 0,
    val lowstock: Int = 0,
    val outstock: Int = 0,
)

@Serializable
data class ProductsResp(val products: List<Product> = emptyList(), val stats: ProductStats = ProductStats())

@Serializable
data class CustomersResp(val customers: List<Customer> = emptyList())

@Serializable
data class OrdersListResp(val orders: List<Order> = emptyList())

@Serializable
data class Template(val title: String = "", val description: String = "")

@Serializable
data class WhatsAppResp(
    @SerialName("pending_count") val pendingCount: Int = 0,
    @SerialName("cod_count") val codCount: Int = 0,
    val templates: List<Template> = emptyList(),
    val queue: List<Order> = emptyList(),
)

@Serializable
data class NotificationsResp(val notifications: List<AppNotification> = emptyList(), val badges: Badges = Badges())

@Serializable
data class DaySales(val date: String = "", val orders: Int = 0, val sales: Double = 0.0)

@Serializable
data class StatusRow(val status: String = "", val count: Int = 0, val total: Double = 0.0)

@Serializable
data class PaymentRow(val method: String = "", val count: Int = 0, val total: Double = 0.0)

@Serializable
data class TopProduct(val name: String = "", val units: Int = 0, val revenue: Double = 0.0)

@Serializable
data class TrafficRow(val date: String = "", val visitors: Int = 0, val pageviews: Int = 0, val sessions: Int = 0)

@Serializable
data class ReportsResp(
    val stats: Stats = Stats(),
    val daily: List<DaySales> = emptyList(),
    @SerialName("by_status") val byStatus: List<StatusRow> = emptyList(),
    val payments: List<PaymentRow> = emptyList(),
    @SerialName("top_products") val topProducts: List<TopProduct> = emptyList(),
    val traffic: List<TrafficRow> = emptyList(),
)

@Serializable
data class SyncLogsResp(val logs: List<SyncLog> = emptyList())

@Serializable
data class HealthResp(
    val store: StoreInfo? = null,
    @SerialName("missing_columns") val missingColumns: List<String> = emptyList(),
    @SerialName("total_orders") val totalOrders: Int = 0,
    @SerialName("total_products") val totalProducts: Int = 0,
    @SerialName("last_log") val lastLog: SyncLog? = null,
)

@Serializable
data class Placeholder(val key: String = "", val label: String = "")

@Serializable
data class SettingsResp(
    val store: StoreInfo? = null,
    val user: UserInfo = UserInfo(),
    @SerialName("webhook_url") val webhookUrl: String = "",
    @SerialName("cron_url") val cronUrl: String = "",
    @SerialName("dashboard_url") val dashboardUrl: String = "",
    @SerialName("whatsapp_template") val whatsappTemplate: String = "",
    @SerialName("whatsapp_template_default") val whatsappTemplateDefault: String = "",
    val placeholders: List<Placeholder> = emptyList(),
)

@Serializable
data class ConnectResp(
    val store: StoreInfo? = null,
    @SerialName("consumer_key_hint") val consumerKeyHint: String = "",
    @SerialName("consumer_secret_hint") val consumerSecretHint: String = "",
)

@Serializable
data class SearchResp(
    val orders: List<Order> = emptyList(),
    val products: List<Product> = emptyList(),
    val customers: List<Customer> = emptyList(),
)
