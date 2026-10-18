package com.digitalseba.admin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(message: String, val httpCode: Int = 0, val isAuth: Boolean = false) : Exception(message)

object Api {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun normalizeServer(raw: String): String {
        var s = raw.trim().trimEnd('/')
        if (s.isEmpty()) s = Prefs.DEFAULT_SERVER
        if (!s.startsWith("http://") && !s.startsWith("https://")) s = "https://$s"
        s = s.removeSuffix("/api/mobile.php").removeSuffix("/dashboard.php").removeSuffix("/login.php").trimEnd('/')
        return s
    }

    /** Raw call returning the parsed JSON object. Runs on the IO dispatcher. */
    suspend fun raw(
        action: String,
        params: Map<String, String> = emptyMap(),
        post: Boolean = false,
        server: String = Prefs.server,
        token: String = Prefs.token,
    ): JsonObject = withContext(Dispatchers.IO) {
        val base = (normalizeServer(server) + "/api/mobile.php").toHttpUrlOrNull()
            ?: throw ApiException("Server address ঠিক নেই।")
        val urlBuilder = base.newBuilder().addQueryParameter("action", action)
        if (!post) params.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }
        val builder = Request.Builder().url(urlBuilder.build())
            .header("Accept", "application/json")
            .header("X-Requested-With", "DSAdminApp")
        if (token.isNotBlank()) {
            builder.header("X-DS-Token", token)
            builder.header("Authorization", "Bearer $token")
        }
        if (post) {
            val form = FormBody.Builder()
            params.forEach { (k, v) -> form.add(k, v) }
            builder.post(form.build())
        }
        val response = try {
            client.newCall(builder.build()).execute()
        } catch (e: IOException) {
            throw ApiException("সার্ভারে কানেক্ট করা যাচ্ছে না। ইন্টারনেট চেক করুন।")
        }
        response.use { res ->
            val body = res.body?.string().orEmpty()
            val obj: JsonObject? = try {
                json.parseToJsonElement(body).jsonObject
            } catch (e: Exception) {
                null
            }
            if (obj == null) {
                val hint = when {
                    res.code == 404 -> "সার্ভারে api/mobile.php ফাইল পাওয়া যায়নি। ফাইলটি আপলোড করুন।"
                    body.contains("<html", ignoreCase = true) -> "সার্ভার থেকে সঠিক উত্তর আসেনি (HTTP ${res.code})। api/mobile.php আপলোড হয়েছে কিনা দেখুন।"
                    else -> "সার্ভার থেকে সঠিক উত্তর আসেনি (HTTP ${res.code})।"
                }
                throw ApiException(hint, res.code)
            }
            val message = obj["message"]?.jsonPrimitive?.contentOrNull
            val code = obj["code"]?.jsonPrimitive?.contentOrNull
            if (!res.isSuccessful) {
                throw ApiException(message ?: "Request failed (HTTP ${res.code}).", res.code, res.code == 401 && code == "auth")
            }
            obj
        }
    }

    suspend inline fun <reified T> call(
        action: String,
        params: Map<String, String> = emptyMap(),
        post: Boolean = false,
    ): T {
        try {
            val obj = raw(action, params, post)
            return json.decodeFromJsonElement(kotlinx.serialization.serializer<T>(), obj)
        } catch (e: ApiException) {
            if (e.isAuth) AuthBus.expire()
            throw e
        }
    }

    /* ----------------------------------------------------------- endpoints */

    suspend fun login(server: String, email: String, password: String, device: String): LoginResp {
        val obj = raw("login", mapOf("email" to email, "password" to password, "device" to device), post = true, server = server, token = "")
        return json.decodeFromJsonElement(LoginResp.serializer(), obj)
    }

    suspend fun logout() = runCatching { raw("logout", post = true) }

    suspend fun live(sync: Boolean = true): LiveResp = call("live", mapOf("sync" to if (sync) "1" else "0"))
    suspend fun dashboard(): DashboardResp = call("dashboard")
    suspend fun orders(status: String, q: String, period: String, page: Int, perPage: Int = 20): OrdersResp =
        call("orders", mapOf("status" to status, "q" to q, "period" to period, "page" to page.toString(), "per_page" to perPage.toString()))
    suspend fun order(id: Int): OrderDetailResp = call("order", mapOf("id" to id.toString()))
    suspend fun setOrderStatus(id: Int, status: String): ActionResp =
        call("order_status", mapOf("order_id" to id.toString(), "status" to status), post = true)
    suspend fun markWhatsApp(id: Int, mark: Boolean): ActionResp =
        call("whatsapp_mark", mapOf("order_id" to id.toString(), "mark" to if (mark) "1" else "0"), post = true)
    suspend fun products(q: String, stock: String): ProductsResp = call("products", mapOf("q" to q, "stock" to stock))
    suspend fun updateStock(id: Int, qty: Int): ActionResp =
        call("product_stock", mapOf("product_id" to id.toString(), "stock_quantity" to qty.toString()), post = true)
    suspend fun customers(q: String): CustomersResp = call("customers", mapOf("q" to q))
    suspend fun customerOrders(name: String, phone: String, email: String): OrdersListResp =
        call("customer_orders", mapOf("name" to name, "phone" to phone, "email" to email))
    suspend fun whatsapp(): WhatsAppResp = call("whatsapp")
    suspend fun notifications(): NotificationsResp = call("notifications")
    suspend fun markNotificationsRead(id: Int = 0): ActionResp = call("notifications_read", mapOf("id" to id.toString()), post = true)
    suspend fun reports(): ReportsResp = call("reports")
    suspend fun syncLogs(): SyncLogsResp = call("sync_logs")
    suspend fun health(): HealthResp = call("health")
    suspend fun settings(): SettingsResp = call("settings")
    suspend fun saveTemplate(t: String): ActionResp = call("save_template", mapOf("template" to t), post = true)
    suspend fun run(type: String): ActionResp = call("run", mapOf("type" to type), post = true)
    suspend fun connectInfo(): ConnectResp = call("connect")
    suspend fun connect(name: String, url: String, key: String, secret: String): ActionResp =
        call("connect", mapOf("store_name" to name, "store_url" to url, "consumer_key" to key, "consumer_secret" to secret), post = true)
    suspend fun search(q: String): SearchResp = call("search", mapOf("q" to q))
}
