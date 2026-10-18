package com.fixoom.marketplace.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fixoom.core.data.notifications.PushMessaging
import com.fixoom.core.data.repository.MarketplaceViewModel

@Composable
fun ConnectedNotifications(
    vm: MarketplaceViewModel,
    onOpen: (String) -> Unit,
    onAccount: () -> Unit,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val push by PushMessaging.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var alertsAllowed by remember { mutableStateOf(phoneAlertsAllowed(context)) }
    var declined by rememberSaveable { mutableStateOf(false) }
    var settingsError by remember { mutableStateOf<String?>(null) }
    DisposableEffect(context, owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                alertsAllowed = phoneAlertsAllowed(context)
                if (alertsAllowed) PushMessaging.refresh(context)
            }
        }
        owner.lifecycle.addObserver(observer)
        alertsAllowed = phoneAlertsAllowed(context)
        if (alertsAllowed) PushMessaging.refresh(context)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            declined = !granted
            alertsAllowed = phoneAlertsAllowed(context)
            if (granted) PushMessaging.refresh(context)
        }
    NotificationsInbox(
        state = state,
        push = push,
        alertsAllowed = alertsAllowed,
        phoneError = settingsError ?: push.error,
        onRefresh = vm::refresh,
        onBack = onBack,
        onAccount = onAccount,
        onOpen = onOpen,
        onRead = { id -> vm.runAction { markNotificationRead(id) } },
        onConnect = { PushMessaging.refresh(context) },
        onEnableAlerts = {
            settingsError = null
            if (
                Build.VERSION.SDK_INT >= 33 &&
                    !declined &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
            ) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                } catch (_: Exception) {
                    settingsError =
                        "Open Android Settings → Apps → Fixoom → Notifications to enable phone alerts."
                }
        },
    )
}

private fun phoneAlertsAllowed(context: Context): Boolean {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
    val channel =
        context
            .getSystemService(NotificationManager::class.java)
            .getNotificationChannel("fixoom_bookings")
    return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
}
