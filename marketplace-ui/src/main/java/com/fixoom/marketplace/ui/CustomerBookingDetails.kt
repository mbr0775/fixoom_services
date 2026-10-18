package com.fixoom.marketplace.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fixoom.core.data.repository.BookingDetailState
import com.fixoom.core.data.repository.MarketplaceState
import com.fixoom.core.model.*

/** Customer presentation only. Callbacks retain the authenticated booking repository. */
@Composable
fun CustomerBookingDetailsScreen(
    state: MarketplaceState,
    detail: BookingDetailState,
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onSupport: () -> Unit,
    onMap: (JobLocation) -> Unit,
    evidence: @Composable () -> Unit,
    confirmation: Boolean = false,
    mapError: String? = null,
    motionEnabled: Boolean = rememberMarketplaceMotionAllowed(),
) {
    val booking = detail.booking
    var section by rememberSaveable(booking?.id) { mutableStateOf("Overview") }
    var mediaOpen by rememberSaveable(booking?.id) { mutableStateOf(false) }
    var cancel by rememberSaveable(booking?.id) { mutableStateOf(false) }
    val list = rememberLazyListState()
    val heroVisible by remember {
        derivedStateOf { list.layoutInfo.visibleItemsInfo.any { it.key == "booking-hero" } }
    }
    LaunchedEffect(section) { list.scrollToItem(0) }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .heightIn(min = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BookingRoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", true, onBack)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        if (confirmation) "Request sent" else "Your booking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    booking?.let {
                        Text(
                            it.reference,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                BookingRoundButton(Icons.Rounded.Refresh, "Refresh", !state.saving, onRefresh)
            }
        },
        bottomBar = {
            if (booking != null && section == "Messages")
                BookingMessageComposer(message, state.saving, onMessageChange, onSend)
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).testTag("customer-booking-list"),
            state = list,
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.saving)
                item(key = "booking-saving") {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Saving your update…", style = MaterialTheme.typography.bodySmall)
                }
            if (detail.loading)
                item(key = "booking-loading") {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Loading your booking…", style = MaterialTheme.typography.bodySmall)
                }
            (state.error ?: detail.error)?.let { error ->
                item(key = "booking-error") {
                    BookingDetailSection("Could not refresh", Icons.Rounded.CloudOff) {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        if (booking != null)
                            Text(
                                "Showing the last saved booking details.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        TextButton(onRefresh, enabled = !state.saving) { Text("Try again") }
                    }
                }
            }
            if (booking == null && !detail.loading)
                item {
                    BookingDetailSection("Booking unavailable", Icons.Rounded.Lock) {
                        Text(
                            "Sign in with the account that owns this booking and refresh.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedButton(onRefresh) { Text("Refresh booking") }
                    }
                }
            if (booking != null) {
                if (confirmation)
                    item(key = "request-saved") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "Your request is saved to this booking.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                item(key = "booking-hero") {
                    CustomerBookingHero(
                        booking,
                        motionEnabled && heroVisible,
                        motionEnabled && heroVisible && !list.isScrollInProgress,
                    ) {
                        list.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.key == "booking-hero" }
                            ?.let { (-it.offset).coerceAtLeast(0).toFloat() / 120f } ?: 1f
                    }
                }
                stickyHeader(key = "booking-tabs") { BookingDetailTabs(section) { section = it } }
                when (section) {
                    "Overview" -> {
                        item(key = "booking-progress") { CustomerBookingProgress(booking) }
                        item(key = "appointment") { CustomerAppointment(booking) }
                        item(key = "professional") {
                            CustomerProfessional(
                                booking,
                                state.data.professionals
                                    .firstOrNull { it.id == booking.providerId }
                                    ?.avatarPath,
                            ) {
                                section = "Messages"
                            }
                        }
                        item(key = "service-details") { CustomerServiceDetails(booking) }
                        if (booking.status in listOf("accepted", "in_progress"))
                            item(key = "live-location") {
                                CustomerLiveLocation(detail, mapError, onMap)
                            }
                        item(key = "payment") { CustomerBookingPayment(booking) }
                        item(key = "booking-evidence") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (!mediaOpen)
                                    BookingDetailSection(
                                        "Photos & videos",
                                        Icons.Rounded.PermMedia,
                                    ) {
                                        Text(
                                            "View or add files for this service, shared with the booking participants and administrator.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        OutlinedButton(
                                            { mediaOpen = true },
                                            modifier =
                                                Modifier.heightIn(min = 48.dp).fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                        ) {
                                            Text("Open photos & videos")
                                        }
                                    }
                                else {
                                    evidence()
                                    TextButton({ mediaOpen = false }) {
                                        Text("Hide photos & videos")
                                    }
                                }
                            }
                        }
                        item(key = "support") {
                            OutlinedButton(
                                onSupport,
                                enabled = !state.saving,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            ) {
                                Icon(Icons.Rounded.SupportAgent, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Report an issue privately")
                            }
                        }
                        if (booking.customerCanCancel)
                            item(key = "cancel") {
                                TextButton(
                                    { cancel = true },
                                    enabled = !state.saving,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.EventBusy,
                                        null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Cancel booking", color = MaterialTheme.colorScheme.error)
                                }
                            }
                    }
                    "History" -> {
                        item {
                            Text(
                                "Booking history",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        detail.historyError?.let { error ->
                            item {
                                BookingDetailSection(
                                    "History could not refresh",
                                    Icons.Rounded.CloudOff,
                                ) {
                                    Text(error, color = MaterialTheme.colorScheme.error)
                                    TextButton(onRefresh) { Text("Retry history") }
                                }
                            }
                        }
                        if (detail.events.isEmpty() && detail.historyError == null)
                            item {
                                BookingDetailSection(
                                    "Your service timeline",
                                    Icons.Rounded.History,
                                ) {
                                    Text(
                                        if (detail.extrasLoading) "Loading booking history…"
                                        else "No recorded changes yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        items(detail.events, key = { "event-${it.id}" }) { event ->
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border =
                                    BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = .45f),
                                    ),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                    ) {
                                        Icon(
                                            Icons.Rounded.History,
                                            null,
                                            Modifier.padding(10.dp).size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Column(
                                        Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            serviceStatus(event.kind),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            serviceTime(event.createdAt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "Messages" -> {
                        detail.messagesError?.let { error ->
                            item {
                                BookingDetailSection(
                                    "Messages unavailable",
                                    Icons.Rounded.CloudOff,
                                ) {
                                    Text(error, color = MaterialTheme.colorScheme.error)
                                    TextButton(onRefresh) { Text("Retry messages") }
                                }
                            }
                        }
                        if (detail.messages.isEmpty() && detail.messagesError == null)
                            item {
                                BookingDetailSection(
                                    if (detail.extrasLoading) "Loading messages…"
                                    else "No messages yet",
                                    Icons.Rounded.Forum,
                                ) {
                                    Text(
                                        "Message your provider about this service.",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        items(detail.messages, key = { "message-${it.id}" }) { chat ->
                            CustomerBookingMessage(
                                chat,
                                chat.senderId == state.data.profile?.id,
                                booking.providerName,
                            )
                        }
                        item {
                            Text(
                                "Showing the latest 100 saved messages. Updates appear while this booking is open.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
    if (cancel && booking?.customerCanCancel == true)
        AlertDialog(
            onDismissRequest = { if (!state.saving) cancel = false },
            icon = { Icon(Icons.Rounded.EventBusy, null) },
            title = { Text("Cancel this booking?") },
            text = {
                Text(
                    "This cancels the service request for both you and the provider. The change will be saved in booking history."
                )
            },
            confirmButton = {
                TextButton(
                    {
                        cancel = false
                        onCancel()
                    },
                    enabled = !state.saving,
                ) {
                    Text("Confirm cancellation")
                }
            },
            dismissButton = {
                TextButton({ cancel = false }, enabled = !state.saving) { Text("Keep booking") }
            },
        )
}

@Composable
private fun BookingRoundButton(
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
        IconButton(click, enabled = enabled, modifier = Modifier.size(48.dp)) {
            Icon(icon, label, Modifier.size(21.dp))
        }
    }
}

@Composable
private fun BookingDetailTabs(selectedTab: String, select: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        listOf("Overview", "Messages", "History").forEach { label ->
            val active = label == selectedTab
            Surface(
                onClick = { select(label) },
                shape = RoundedCornerShape(16.dp),
                color = if (active) InboxTeal else Color.Transparent,
                modifier =
                    Modifier.weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("booking-tab-$label")
                        .semantics {
                            selected = active
                            role = Role.Tab
                        },
            ) {
                Box(
                    Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerBookingMessage(chat: BookingMessage, own: Boolean, provider: String) {
    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = if (own) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.92f),
            shape =
                RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 22.dp,
                    bottomStart = if (own) 22.dp else 5.dp,
                    bottomEnd = if (own) 5.dp else 22.dp,
                ),
            color =
                if (own) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .3f)),
        ) {
            val color =
                if (own) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (own) "You" else provider.ifBlank { "Provider" },
                    color = color,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(chat.body, color = color, style = MaterialTheme.typography.bodyLarge)
                Text(
                    serviceTime(chat.createdAt),
                    color = color.copy(alpha = .8f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun BookingMessageComposer(
    message: String,
    saving: Boolean,
    change: (String) -> Unit,
    send: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                message,
                change,
                Modifier.fillMaxWidth().testTag("booking-message-input"),
                label = { Text("Write a message") },
                enabled = !saving,
                maxLines = 4,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions =
                    KeyboardActions(onSend = { if (message.isNotBlank() && !saving) send(message) }),
            )
            Button(
                { send(message) },
                enabled = message.isNotBlank() && !saving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (saving) "Sending…" else "Send message")
            }
        }
    }
}
