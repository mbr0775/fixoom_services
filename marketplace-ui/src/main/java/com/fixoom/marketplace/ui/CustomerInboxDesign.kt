package com.fixoom.marketplace.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
