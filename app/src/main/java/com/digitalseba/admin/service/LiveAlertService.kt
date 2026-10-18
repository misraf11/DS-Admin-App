package com.digitalseba.admin.service

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.digitalseba.admin.MainActivity
import com.digitalseba.admin.R
import com.digitalseba.admin.data.LiveHub
import com.digitalseba.admin.data.OrderAlerts
import com.digitalseba.admin.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps checking for new orders while the app is closed, so the owner gets an
 * instant notification. Only runs when "Background order alerts" is enabled.
 */
class LiveAlertService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Prefs.backgroundAlerts || !Prefs.loggedIn) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = NotificationCompat.Builder(this, OrderAlerts.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_order)
            .setColor(0xFF6C3BFF.toInt())
            .setContentTitle("DS Admin লাইভ")
            .setContentText("নতুন অর্ডারের জন্য নজর রাখছে…")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 1, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                ServiceCompat.startForeground(this, 4242, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(4242, notification)
            }
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (loop?.isActive != true) {
            loop = scope.launch {
                while (isActive) {
                    if (!Prefs.backgroundAlerts || !Prefs.loggedIn) {
                        stopSelf()
                        break
                    }
                    // While the app is open the faster in-app poller is already running.
                    if (!LiveHub.appInForeground) LiveHub.pollOnce(applicationContext)
                    delay(20_000)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            if (!Prefs.backgroundAlerts || !Prefs.loggedIn) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, LiveAlertService::class.java))
            } catch (_: Exception) {
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LiveAlertService::class.java))
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.init(context)
        LiveAlertService.start(context)
    }
}
