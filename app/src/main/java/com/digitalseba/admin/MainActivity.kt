package com.digitalseba.admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.digitalseba.admin.data.OrderAlerts
import com.digitalseba.admin.data.Prefs
import com.digitalseba.admin.ui.AppRoot
import com.digitalseba.admin.ui.theme.DSTheme

class MainActivity : ComponentActivity() {
    /** Order id from a tapped notification (0 = open the orders list). */
    private val pendingOrder = mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readIntent(intent)
        setContent {
            val theme by Prefs.theme.collectAsState()
            DSTheme(theme) {
                AppRoot(pendingOrder = pendingOrder.value, onPendingHandled = { pendingOrder.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent?) {
        if (intent?.hasExtra(OrderAlerts.EXTRA_ORDER_ID) == true) {
            pendingOrder.value = intent.getIntExtra(OrderAlerts.EXTRA_ORDER_ID, 0)
            intent.removeExtra(OrderAlerts.EXTRA_ORDER_ID)
        }
    }
}
