package com.fixoom.marketplace.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixoom.core.data.repository.MarketplaceState
import com.fixoom.core.model.BookingNotification
import com.fixoom.core.model.serviceTime

@Composable
internal fun CustomerInboxPage(
    state: MarketplaceState,
    refresh: () -> Unit,
    back: () -> Unit,
    list: LazyListState,
    content: LazyListScope.() -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                InboxRoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", true, back)
                Text(
                    "Notifications",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                InboxRoundButton(Icons.Rounded.Refresh, "Refresh", !state.saving, refresh)
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).testTag("customer-inbox-list"),
            state = list,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.loading)
                item(key = "inbox-loading") {
                    LinearProgressIndicator(
                        Modifier.fillMaxWidth(),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            if (state.saving)
                item(key = "inbox-saving") {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Updating read status…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            state.error?.let { error ->
                item(key = "inbox-error") {
                    InfoCard("Could not update your inbox", error, error = true) {
                        TextButton(refresh, enabled = !state.saving) { Text("Try again") }
                    }
                }
            }
            content()
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun InboxRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    click: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .45f)),
    ) {
        IconButton(onClick = click, enabled = enabled, modifier = Modifier.size(48.dp)) {
            Icon(icon, label, Modifier.size(21.dp))
        }
    }
}

@Composable
internal fun CustomerInboxHero(
    summary: String,
    motion: Boolean,
    floating: Boolean,
    offset: () -> Float,
) {
    Column(
        Modifier.fillMaxWidth().testTag("customer-inbox-hero").padding(top = 8.dp, bottom = 12.dp)
    ) {
        Text(
            "YOUR FIXOOM INBOX",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 280.dp || LocalDensity.current.fontScale > 1.25f) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    InboxHeadline()
                    CustomerInboxSculpture(
                        Modifier.size(176.dp).align(Alignment.CenterHorizontally),
                        motion,
                        floating,
                        offset,
                    )
                }
            } else
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { InboxHeadline() }
                    CustomerInboxSculpture(Modifier.size(176.dp), motion, floating, offset)
                }
        }
        Surface(shape = RoundedCornerShape(50), color = InboxLime) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.DoneAll, null, Modifier.size(16.dp), tint = InboxTeal)
                Text(
                    summary,
                    color = InboxTeal,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            "Bookings, replies and every step in between.",
            Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InboxHeadline() {
    Text(
        "In the\nknow.",
        style =
            MaterialTheme.typography.displayLarge.copy(
                fontSize = 40.sp,
                lineHeight = 44.sp,
                letterSpacing = (-1.5).sp,
            ),
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun CustomerPhoneStatus() {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = .45f),
                RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(shape = CircleShape, color = Color(0xFFE0EBCF)) {
            Icon(
                Icons.Rounded.NotificationsActive,
                null,
                Modifier.padding(7.dp).size(18.dp),
                tint = InboxTeal,
            )
        }
        Text(
            "Phone alerts connected",
            Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            Icons.Rounded.CheckCircle,
            null,
            Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun CustomerInboxControls(
    total: Int,
    unread: Int,
    unreadOnly: Boolean,
    enabled: Boolean,
    filter: (Boolean) -> Unit,
    readAll: () -> Unit,
    motion: Boolean,
) {
    Column(
        Modifier.padding(top = 6.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .selectableGroup()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f))
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (onlyUnread in listOf(false, true)) {
                val selected = unreadOnly == onlyUnread
                val fill by
                    animateColorAsState(
                        if (selected) InboxTeal else Color.Transparent,
                        if (motion) tween(180) else snap(),
                        label = "customer_inbox_tab",
                    )
                Surface(
                    onClick = { filter(onlyUnread) },
                    color = fill,
                    shape = RoundedCornerShape(17.dp),
                    modifier =
                        Modifier.weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag(if (onlyUnread) "inbox-filter-unread" else "inbox-filter-all")
                            .semantics {
                                this.selected = selected
                                role = Role.Tab
                            },
                ) {
                    Box(
                        Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (onlyUnread) "Unread ($unread)" else "All ($total)",
                            color =
                                if (selected) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Latest updates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(
                onClick = readAll,
                enabled = enabled && unread > 0,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Rounded.DoneAll, "Mark all as read", Modifier.size(22.dp))
            }
        }
    }
}

@Composable
internal fun CustomerNotificationTile(
    alert: BookingNotification,
    index: Int,
    enabled: Boolean,
    motion: Boolean,
    depth: () -> Float,
    open: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val pressure =
        animateFloatAsState(
            if (motion && pressed) 1f else 0f,
            spring(stiffness = Spring.StiffnessMedium),
            label = "customer_update_press",
        )
    val entrance = remember(alert.id) { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(alert.id, motion) {
        if (motion)
            entrance.animateTo(1f, tween(320, index.coerceAtMost(3) * 35, FastOutSlowInEasing))
        else entrance.snapTo(1f)
    }
    val unread = alert.readAt == null
    val art = notificationArtwork(alert.kind)
    val light = MaterialTheme.colorScheme.background.luminance() > .5f
    val color = if (unread && light) Color(0xFFF0F4E9) else MaterialTheme.colorScheme.surface
    Card(
        onClick = open,
        enabled = enabled,
        interactionSource = source,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        border =
            BorderStroke(
                1.dp,
                if (unread) MaterialTheme.colorScheme.primary.copy(alpha = .18f)
                else MaterialTheme.colorScheme.outline.copy(alpha = .45f),
            ),
        modifier =
            Modifier.fillMaxWidth()
                .testTag("notification-${alert.id}")
                .semantics { stateDescription = if (unread) "Unread" else "Read" }
                .graphicsLayer {
                    val progress = if (motion) entrance.value else 1f
                    val press = if (motion) pressure.value else 0f
                    alpha = .5f + progress * .5f
                    translationY = (1 - progress) * 12.dp.toPx()
                    scaleX = 1 - press * .02f
                    scaleY = scaleX
                    rotationX = (1 - progress) * 7f
                    cameraDistance = 24 * density
                },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(52.dp)
                        .clearAndSetSemantics {}
                        .graphicsLayer {
                            rotationY = if (motion) -16f + depth() * 12f else -16f
                            rotationX = if (motion) 10f - pressure.value * 5f else 10f
                            rotationZ = -8f
                            translationY = if (motion) -pressure.value * 3.dp.toPx() else 0f
                            cameraDistance = 18 * density
                        }
                ) {
                    Box(
                        Modifier.size(46.dp)
                            .offset(x = 3.dp, y = 4.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(InboxTeal.copy(alpha = .3f))
                    )
                    Box(
                        Modifier.size(46.dp)
                            .graphicsLayer {
                                    shadowElevation = 1.5.dp.toPx()
                                shape = RoundedCornerShape(15.dp)
                                clip = true
                            }
                            .background(
                                Brush.linearGradient(listOf(Color.White, art.second, art.second))
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(art.first, null, Modifier.size(24.dp), tint = InboxTeal)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        alert.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        serviceTime(alert.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (unread)
                    Box(
                        Modifier.size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .semantics { contentDescription = "Unread update" }
                    )
            }
            Text(
                alert.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "View booking",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Surface(
                    shape = CircleShape,
                    color = if (unread) InboxLime else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        null,
                        Modifier.padding(7.dp).size(16.dp),
                        tint =
                            if (unread) InboxTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
