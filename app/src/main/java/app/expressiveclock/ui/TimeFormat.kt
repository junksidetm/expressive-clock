package app.expressiveclock.ui

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Event-driven: updates visible text after system preference changes without polling. */
@Composable
internal fun rememberSystem24HourFormat(): Boolean {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales.toLanguageTags()
    var use24h by remember(context) { mutableStateOf(DateFormat.is24HourFormat(context)) }
    DisposableEffect(context, owner, locale) {
        fun refresh() { use24h = DateFormat.is24HourFormat(context) }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        val lifecycle = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        context.contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.TIME_12_24), false, observer)
        owner.lifecycle.addObserver(lifecycle)
        refresh()
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
            owner.lifecycle.removeObserver(lifecycle)
        }
    }
    return use24h
}
