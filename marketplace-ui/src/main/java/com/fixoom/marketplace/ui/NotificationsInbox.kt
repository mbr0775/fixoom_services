package com.fixoom.marketplace.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fixoom.core.data.notifications.PushState
import com.fixoom.core.data.repository.MarketplaceState
import com.fixoom.core.model.BookingNotification
import com.fixoom.core.model.serviceTime

/** Presentation only. All counts and actions belong to the authenticated, persisted inbox. */
@Composable
fun NotificationsInbox(
    state: MarketplaceState,
    push: PushState,
    alertsAllowed: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onAccount: () -> Unit,
    onOpen: (String) -> Unit,
    onRead: (String?) -> Unit,
    onConnect: () -> Unit,
    onEnableAlerts: () -> Unit,
    phoneError: String? = push.error,
    motionEnabled: Boolean = rememberMarketplaceMotionAllowed(),
) {
    val signedIn = state.data.profile != null
    val notifications = state.data.notifications
    val unread = notifications.count { it.readAt == null }
    var unreadOnly by rememberSaveable(state.data.profile?.id) { mutableStateOf(false) }
    val visible = if (unreadOnly) notifications.filter { it.readAt == null } else notifications
    val list = rememberLazyListState()
    val heroVisible by remember {
        derivedStateOf { list.layoutInfo.visibleItemsInfo.any { it.key == "inbox-hero" } }
    }
    val heroSummary =
        when {
            !signedIn -> "Your service inbox"
            state.loading -> "Checking for updates"
            unread > 0 -> "$unread unread ${if (unread == 1) "update" else "updates"}"
            state.error != null -> "Saved inbox"
            else -> "All updates read"
        }
    // Read state/counts provide confirmation; unrelated workspace save notices do not belong here.
    MarketplacePage(
        "Notifications",
        state.copy(notice = null),
        onRefresh,
        onBack,
        listState = list,
    ) {
        item(key = "inbox-hero") {
            NotificationHero(
                heroSummary,
                motionEnabled && heroVisible,
                motionEnabled && heroVisible && !list.isScrollInProgress,
            ) {
                list.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.key == "inbox-hero" }
                    ?.let { (-it.offset).coerceAtLeast(0).toFloat() } ?: 180f
            }
        }
        if (!signedIn) {
            item {
                InboxEmpty(
                    "Your updates, together",
                    "Sign in to see your service requests, replies and booking updates.",
                    "Sign in",
                    onAccount,
                )
            }
        } else {
            item(key = "phone-alerts") {
                PhoneAlertCard(
                    push,
                    alertsAllowed,
                    phoneError,
                    state.saving,
                    onConnect,
                    onEnableAlerts,
                )
            }
            item(key = "inbox-controls") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "Recent activity",
                            Modifier.align(Alignment.CenterVertically),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(
                            onClick = { onRead(null) },
                            enabled = unread > 0 && !state.saving,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Icon(Icons.Rounded.DoneAll, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mark all as read")
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FilterChip(
                            selected = !unreadOnly,
                            onClick = { unreadOnly = false },
                            label = { Text("All (${notifications.size})") },
                            leadingIcon = { Icon(Icons.Rounded.Inbox, null, Modifier.size(18.dp)) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("inbox-filter-all"),
                        )
                        FilterChip(
                            selected = unreadOnly,
                            onClick = { unreadOnly = true },
                            label = { Text("Unread ($unread)") },
                            leadingIcon = {
                                Icon(Icons.Rounded.MarkEmailUnread, null, Modifier.size(18.dp))
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier =
                                Modifier.heightIn(min = 48.dp).testTag("inbox-filter-unread"),
                        )
                    }
                }
            }
            if (visible.isEmpty() && !state.loading && state.error == null) {
                item(key = "inbox-empty") {
                    InboxEmpty(
                        if (unreadOnly) "You’re all caught up" else "A little quiet, for now",
                        if (unreadOnly) "No unread updates in your loaded inbox."
                        else
                            "Your real booking requests, replies and service updates will appear here.",
                        if (unreadOnly) "Show all updates" else "Refresh inbox",
                        { if (unreadOnly) unreadOnly = false else onRefresh() },
                    )
                }
            }
            itemsIndexed(visible, key = { _, alert -> "alert-${alert.id}" }) { index, alert ->
                NotificationTile(
                    alert,
                    index,
                    !state.saving,
                    motionEnabled,
                    depth = {
                        val layout = list.layoutInfo
                        val item =
                            layout.visibleItemsInfo.firstOrNull { it.key == "alert-${alert.id}" }
                        if (item == null) 0f
                        else {
                            val height =
                                (layout.viewportEndOffset - layout.viewportStartOffset)
                                    .coerceAtLeast(1)
                            ((item.offset + item.size / 2f - layout.viewportStartOffset) / height *
                                    2 - 1)
                                .coerceIn(-1f, 1f)
                        }
                    },
                ) {
                    onRead(alert.id)
                    onOpen(alert.bookingId)
                }
            }
            if (notifications.isNotEmpty())
                item {
                    Text(
                        "Showing your latest saved updates (up to 100). Open Bookings for your full service history.",
                        Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
        }
    }
}

@Composable
private fun PhoneAlertCard(
    push: PushState,
    allowed: Boolean,
    error: String?,
    busy: Boolean,
    connect: () -> Unit,
    enable: () -> Unit,
) {
    val connected = push.configured && push.registered && allowed
    val title =
        when {
            !push.configured -> "In-app updates"
            !allowed -> "Turn on phone alerts"
            connected -> "Phone alerts connected"
            else -> "Connect phone alerts"
        }
    val description =
        when {
            !push.configured ->
                "Phone alerts are not configured for this build. Your saved updates are available here."
            !allowed ->
                "Allow Fixoom notifications in Android to receive booking alerts outside the app."
            connected -> "This phone is registered for your booking updates."
            else -> "Register this phone to receive your booking updates outside the app."
        }
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFE4EFDE)) {
                    Icon(
                        if (connected) Icons.Rounded.NotificationsActive
                        else Icons.Rounded.NotificationsNone,
                        null,
                        Modifier.padding(11.dp).size(22.dp),
                        tint = InboxTeal,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (error != null)
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            if (push.configured && (!connected || error != null)) {
                OutlinedButton(
                    onClick = if (!allowed) enable else connect,
                    enabled = !busy,
                    modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        if (!allowed) "Enable phone alerts"
                        else if (connected) "Retry connection" else "Connect phone alerts"
                    )
                }
            }
        }
    }
}

@Composable
private fun InboxEmpty(title: String, body: String, action: String, onAction: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Rounded.Inbox,
                null,
                Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(action)
            }
        }
    }
}

@Composable
private fun NotificationTile(
    alert: BookingNotification,
    index: Int,
    enabled: Boolean,
    motion: Boolean,
    depth: () -> Float,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press =
        animateFloatAsState(
            if (motion && pressed) 1f else 0f,
            spring(stiffness = Spring.StiffnessMediumLow),
            label = "notification_press",
        )
    val entrance = remember(alert.id) { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(alert.id, motion) {
        if (motion)
            entrance.animateTo(
                1f,
                tween(320, delayMillis = index.coerceAtMost(3) * 35, easing = FastOutSlowInEasing),
            )
        else entrance.snapTo(1f)
    }
    val unread = alert.readAt == null
    val art = notificationArtwork(alert.kind)
    Card(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        modifier =
            Modifier.fillMaxWidth()
                .testTag("notification-${alert.id}")
                .semantics { stateDescription = if (unread) "Unread" else "Read" }
                .graphicsLayer {
                    val progress = if (motion) entrance.value else 1f
                    val pressure = if (motion) press.value else 0f
                    val scroll = if (motion) depth() else 0f
                    alpha = .5f + .5f * progress
                    translationY = (1 - progress) * 12.dp.toPx() - scroll * 3.dp.toPx()
                    rotationX = (1 - progress) * 8f - scroll * 1.5f
                    rotationY = pressure * -1.5f
                    scaleX = 1 - pressure * .018f
                    scaleY = scaleX
                    cameraDistance = 24 * density
                },
        shape = RoundedCornerShape(23.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
        border =
            BorderStroke(
                1.dp,
                if (unread) MaterialTheme.colorScheme.primary.copy(alpha = .18f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (unread) 4.dp else 1.dp),
    ) {
        Row(
            Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = art.second,
                shadowElevation = 3.dp,
                modifier =
                    Modifier.graphicsLayer {
                        rotationZ = -6f
                        rotationY = if (motion) depth() * 8f else -5f
                        translationY = if (motion) -press.value * 2.dp.toPx() else 0f
                        cameraDistance = 18 * density
                    },
            ) {
                Icon(art.first, null, Modifier.padding(12.dp).size(22.dp), tint = InboxTeal)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    alert.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Medium,
                )
                Text(
                    alert.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    serviceTime(alert.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (unread)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                "UNREAD",
                                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            "View booking",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            null,
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

private fun notificationArtwork(kind: String): Pair<ImageVector, Color> =
    when (kind) {
        "requested" -> Icons.Rounded.Handyman to Color(0xFFD3EEDC)
        "accepted" -> Icons.Rounded.TaskAlt to Color(0xFFD3EEE7)
        "in_progress" -> Icons.Rounded.WorkHistory to Color(0xFFDAECF2)
        "completed" -> Icons.Rounded.Verified to Color(0xFFE1EDCB)
        "cancelled",
        "rejected" -> Icons.Rounded.EventBusy to Color(0xFFFFE6D7)
        "message" -> Icons.AutoMirrored.Rounded.Chat to Color(0xFFE5DFFA)
        "direct_payment_received" -> Icons.Rounded.AccountBalanceWallet to Color(0xFFEBF3C7)
        else -> Icons.Rounded.NotificationsNone to Color(0xFFE3ECE8)
    }
