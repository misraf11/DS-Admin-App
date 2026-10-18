package com.digitalseba.admin

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.digitalseba.admin.data.LiveHub
import com.digitalseba.admin.data.OrderAlerts
import com.digitalseba.admin.data.Prefs
import com.digitalseba.admin.service.LiveAlertService

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        OrderAlerts.createChannels(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    LiveHub.appInForeground = true
                    if (Prefs.loggedIn) {
                        LiveHub.start(this)
                        LiveAlertService.start(this)
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    LiveHub.appInForeground = false
                    LiveHub.stop()
                }
                else -> Unit
            }
        })
    }
}
