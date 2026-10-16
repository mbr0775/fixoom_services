package com.fixoom.marketplace.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fixoom.core.data.notifications.PushMessaging
import com.fixoom.core.data.repository.MarketplaceViewModel
import com.fixoom.core.model.serviceTime

@Composable
fun ConnectedNotifications(
    vm: MarketplaceViewModel,
    onOpen: (String) -> Unit,
    onAccount: () -> Unit,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val push by PushMessaging.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { PushMessaging.refresh(context) }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) PushMessaging.refresh(context)
        }
    MarketplacePage("Notifications", state, vm::refresh, onBack) {
        item {
            WorkspaceDepthHeader(
                "Stay connected.",
                "Booking requests, replies and service updates are saved here.",
                Icons.Rounded.Notifications,
            )
        }
        if (state.data.profile == null)
            item {
                InfoCard("Sign in for updates", "Your inbox belongs to your signed-in account.") {
                    Button(onAccount) { Text("Sign in") }
                }
            }
        else {
            item {
                InfoCard(
                    "Booking alerts",
                    if (!push.configured)
                        "Your inbox updates while the app is open. Background phone alerts are not configured for this build."
                    else if (push.registered)
                        "This phone is registered for booking and reply alerts. Phone settings and connectivity can affect delivery."
                    else
                        "Allow phone notifications and connect your account to receive booking and reply alerts.",
                ) {
                    if (Build.VERSION.SDK_INT >= 33)
                        OutlinedButton({
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }) {
                            Text("Allow phone notifications")
                        }
                    push.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (push.configured && !push.registered)
                        TextButton({ PushMessaging.refresh(context) }) {
                            Text("Connect phone alerts")
                        }
                    TextButton(
                        { vm.runAction { markNotificationRead() } },
                        enabled =
                            !state.saving && state.data.notifications.any { it.readAt == null },
                    ) {
                        Text("Mark all as read")
                    }
                }
            }
            if (!state.loading && state.data.notifications.isEmpty())
                item {
                    InfoCard(
                        "You're all caught up",
                        "New booking activity and replies will appear here.",
                    )
                }
            items(state.data.notifications, key = { it.id }) { notification ->
                Card(
                    onClick = {
                        vm.runAction { markNotificationRead(notification.id) }
                        onOpen(notification.bookingId)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            notification.title,
                            fontWeight =
                                if (notification.readAt == null) FontWeight.Bold
                                else FontWeight.Medium,
                        )
                        Text(notification.body, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            serviceTime(notification.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        if (notification.readAt == null)
                            Text(
                                "Unread",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                    }
                }
            }
            item {
                Text(
                    "Latest 100 notifications. Your full booking history remains in Bookings.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
