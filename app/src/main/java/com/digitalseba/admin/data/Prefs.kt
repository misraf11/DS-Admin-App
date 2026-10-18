package com.digitalseba.admin.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Small persisted settings store (token, server, preferences). */
object Prefs {
    const val DEFAULT_SERVER = "https://admin.digitalseba.shop"

    private lateinit var sp: SharedPreferences

    private val _theme = MutableStateFlow("system")
    val theme: StateFlow<String> = _theme

    fun init(context: Context) {
        sp = context.getSharedPreferences("ds_admin", Context.MODE_PRIVATE)
        _theme.value = sp.getString("theme", "system") ?: "system"
    }

    var server: String
        get() = sp.getString("server", DEFAULT_SERVER) ?: DEFAULT_SERVER
        set(v) = sp.edit().putString("server", v).apply()

    var token: String
        get() = sp.getString("token", "") ?: ""
        set(v) = sp.edit().putString("token", v).apply()

    var userName: String
        get() = sp.getString("user_name", "") ?: ""
        set(v) = sp.edit().putString("user_name", v).apply()

    var userEmail: String
        get() = sp.getString("user_email", "") ?: ""
        set(v) = sp.edit().putString("user_email", v).apply()

    var storeName: String
        get() = sp.getString("store_name", "") ?: ""
        set(v) = sp.edit().putString("store_name", v).apply()

    var lastOrderId: Int
        get() = sp.getInt("last_order_id", 0)
        set(v) = sp.edit().putInt("last_order_id", v).apply()

    var backgroundAlerts: Boolean
        get() = sp.getBoolean("bg_alerts", false)
        set(v) = sp.edit().putBoolean("bg_alerts", v).apply()

    var askedNotificationPermission: Boolean
        get() = sp.getBoolean("asked_notif", false)
        set(v) = sp.edit().putBoolean("asked_notif", v).apply()

    fun setTheme(value: String) {
        sp.edit().putString("theme", value).apply()
        _theme.value = value
    }

    val loggedIn: Boolean get() = token.isNotBlank()

    fun clearSession() {
        sp.edit()
            .remove("token")
            .remove("user_name")
            .remove("user_email")
            .remove("last_order_id")
            .apply()
    }
}
