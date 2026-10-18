package com.digitalseba.admin.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.digitalseba.admin.data.LiveHub
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/* ------------------------------------------------------------- snackbars */

class AppSnack(override val message: String, val isError: Boolean) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Short
}

class Notifier(private val host: SnackbarHostState, private val scope: CoroutineScope) {
    fun success(msg: String) = show(msg, false)
    fun error(msg: String) = show(msg, true)
    private fun show(msg: String, err: Boolean) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            host.showSnackbar(AppSnack(msg, err))
        }
    }
}

val LocalNotifier = staticCompositionLocalOf<Notifier> { error("Notifier not provided") }

/* ---------------------------------------------------------- live loading */

/** Loads screen data and silently reloads it whenever the live engine sees a change. */
class LiveLoader<T>(private val scope: CoroutineScope, private val fetch: suspend () -> T) {
    var data by mutableStateOf<T?>(null)
    var loading by mutableStateOf(false)
    var refreshing by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    private var job: Job? = null

    fun load(silent: Boolean = false, pull: Boolean = false) {
        if (silent && job?.isActive == true) return
        job?.cancel()
        job = scope.launch {
            if (!silent) loading = data == null
            if (pull) refreshing = true
            try {
                data = fetch()
                error = null
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                if (!silent || data == null) error = e.message ?: "Something went wrong"
            } finally {
                loading = false
                refreshing = false
            }
        }
    }

    fun refresh() = load(pull = true)
}

@Composable
fun <T> rememberLive(vararg keys: Any?, fetch: suspend () -> T): LiveLoader<T> {
    val scope = rememberCoroutineScope()
    val loader = remember(*keys) { LiveLoader(scope, fetch) }
    LaunchedEffect(loader) {
        loader.load()
        LiveHub.tick.drop(1).collect { loader.load(silent = true) }
    }
    return loader
}

/* --------------------------------------------------------------- actions */

class ActionRunner(private val scope: CoroutineScope, private val notifier: Notifier) {
    var busy by mutableStateOf<String?>(null)

    fun isBusy(key: String) = busy == key

    fun run(key: String, onDone: () -> Unit = {}, block: suspend () -> String?) {
        if (busy != null) return
        scope.launch {
            busy = key
            try {
                val msg = block()
                if (!msg.isNullOrBlank()) notifier.success(msg)
                onDone()
                LiveHub.bump()
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                notifier.error(e.message ?: "Action failed")
            } finally {
                busy = null
            }
        }
    }
}

@Composable
fun rememberActions(): ActionRunner {
    val scope = rememberCoroutineScope()
    val notifier = LocalNotifier.current
    return remember { ActionRunner(scope, notifier) }
}
