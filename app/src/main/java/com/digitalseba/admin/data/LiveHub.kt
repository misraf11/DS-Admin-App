package com.digitalseba.admin.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.digitalseba.admin.MainActivity
import com.digitalseba.admin.R
import com.digitalseba.admin.ui.Fmt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Broadcasts "session expired" so the UI can jump back to the login screen. */
object AuthBus {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired: SharedFlow<Unit> = _expired
    fun expire() {
        Prefs.clearSession()
        _expired.tryEmit(Unit)
    }
}

/** Realtime engine: polls the live endpoint and tells every screen when data changes. */
object LiveHub {
    private const val FOREGROUND_INTERVAL_MS = 5_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var job: Job? = null
    private var lastVersion: String? = null

    private val _live = MutableStateFlow<LiveResp?>(null)
    val live: StateFlow<LiveResp?> = _live

    /** Increments whenever server data changed — screens reload silently. */
    private val _tick = MutableStateFlow(0)
    val tick: StateFlow<Int> = _tick

    private val _online = MutableStateFlow(true)
    val online: StateFlow<Boolean> = _online

    private val _newOrders = MutableSharedFlow<List<Order>>(extraBufferCapacity = 8)
    val newOrders: SharedFlow<List<Order>> = _newOrders

    @Volatile var appInForeground: Boolean = false

    fun start(context: Context) {
        if (job?.isActive == true) return
        val app = context.applicationContext
        job = scope.launch {
            while (isActive) {
                pollOnce(app)
                delay(FOREGROUND_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    /** Force all visible screens to refresh now (after a local change). */
    fun bump() {
        _tick.value = _tick.value + 1
    }

    fun reset() {
        lastVersion = null
        _live.value = null
    }

    suspend fun pollOnce(context: Context) {
        if (!Prefs.loggedIn) return
        mutex.withLock {
            try {
                val r = Api.live(sync = true)
                _online.value = true
                _live.value = r
                if (lastVersion != null && r.version != lastVersion) bump()
                lastVersion = r.version
                val fresh = OrderAlerts.detect(context, r)
                if (fresh.isNotEmpty()) _newOrders.tryEmit(fresh)
            } catch (e: ApiException) {
                _online.value = e.httpCode != 0
            } catch (e: Exception) {
                _online.value = false
            }
        }
    }
}

/** Detects newly arrived orders and shows a system notification for them. */
object OrderAlerts {
    const val CHANNEL_ORDERS = "new_orders"
    const val CHANNEL_SERVICE = "live_service"
    const val EXTRA_ORDER_ID = "open_order_id"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        val orders = NotificationChannel(CHANNEL_ORDERS, "নতুন অর্ডার", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "নতুন অর্ডার এলে সাথে সাথে জানাবে"
            enableVibration(true)
            enableLights(true)
        }
        val service = NotificationChannel(CHANNEL_SERVICE, "লাইভ অ্যালার্ট সার্ভিস", NotificationManager.IMPORTANCE_MIN).apply {
            description = "ব্যাকগ্রাউন্ডে অর্ডার চেক করার জন্য"
            setShowBadge(false)
        }
        nm.createNotificationChannel(orders)
        nm.createNotificationChannel(service)
    }

    @Synchronized
    fun detect(context: Context, live: LiveResp): List<Order> {
        val max = live.maxOrderId
        val last = Prefs.lastOrderId
        if (max <= 0) return emptyList()
        if (last <= 0 || max < last) {
            Prefs.lastOrderId = max
            return emptyList()
        }
        if (max == last) return emptyList()
        val fresh = live.latestOrders.filter { it.id > last }.sortedBy { it.id }
        Prefs.lastOrderId = max
        notify(context, fresh, max - last)
        return fresh
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun notify(context: Context, fresh: List<Order>, count: Int) {
        if (!canNotify(context)) return
        val nm = NotificationManagerCompat.from(context)
        val list = if (fresh.isEmpty()) emptyList() else fresh.takeLast(3)
        try {
            if (list.isEmpty()) {
                nm.notify(5000, build(context, "🛒 $count টি নতুন অর্ডার", "ট্যাপ করে অর্ডার দেখুন", 0))
                return
            }
            list.forEach { o ->
                val who = o.customerName.ifBlank { "Guest customer" }
                val text = "$who · ${Fmt.money(o.total)}" + if (o.isCod) " · COD" else ""
                nm.notify(6000 + (o.id % 100000), build(context, "🛒 নতুন অর্ডার #${o.number}", text, o.id))
            }
        } catch (_: SecurityException) {
        }
    }

    private fun build(context: Context, title: String, text: String, orderId: Int) =
        NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(R.drawable.ic_stat_order)
            .setColor(0xFF6C3BFF.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    orderId,
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra(EXTRA_ORDER_ID, orderId),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()
}
