package com.fixoom.marketplace.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fixoom.core.data.repository.*
import com.fixoom.core.model.*
import java.util.Locale

@Composable
fun ConnectedBookings(
    vm: MarketplaceViewModel,
    provider: Boolean = false,
    mode: String = "all",
    onOpen: (String) -> Unit,
    onAccount: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selected by
        rememberSaveable(mode) {
            mutableStateOf(
                if (mode == "requests") "requested"
                else if (mode == "active" || mode == "schedule") "active" else "all"
            )
        }
    var query by rememberSaveable { mutableStateOf("") }
    val profile = state.data.profile
    val title =
        when (mode) {
            "schedule" -> "Your schedule"
            "activity" -> "Booking updates"
            "messages" -> "Booking conversations"
            else -> if (provider) "Your jobs" else "Your bookings"
        }
    val bookings =
        state.data.bookings
            .filter { b ->
                (selected == "all" ||
                    if (selected == "active") b.status in listOf("accepted", "in_progress")
                    else b.status == selected) &&
                    (b.reference +
                            " " +
                            b.customerName +
                            " " +
                            b.providerName +
                            " " +
                            b.serviceAddress)
                        .contains(query, true)
            }
            .let { if (mode == "schedule") it.sortedBy { b -> b.scheduledAt } else it }
    val list = rememberLazyListState()
    MarketplacePage(title, state, vm::refresh, onBack, listState = list) {
        if (provider)
            item(key = "workspace_header") {
                WorkspaceDepthHeader(
                    title,
                    when (mode) {
                        "schedule" ->
                            "Your accepted work, planned around the time reserved for each service."
                        "messages" -> "Keep every customer conversation connected to its booking."
                        "activity" ->
                            "Follow requests, job progress and completed work in one place."
                        else ->
                            "Your craft, your schedule. Review the service and reserved time before accepting."
                    },
                    when (mode) {
                        "schedule" -> Icons.Rounded.CalendarMonth
                        "messages" -> Icons.Rounded.Forum
                        else -> Icons.Rounded.Handyman
                    },
                    { list.firstVisibleItemScrollOffset.toFloat() },
                )
            }
        if (profile == null)
            item {
                InfoCard(
                    "Sign in to continue",
                    "Your bookings, updates and conversations are saved to your account.",
                ) {
                    Button(onAccount) { Text("Sign in") }
                }
            }
        else {
            item {
                OutlinedTextField(
                    query,
                    { query = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Search bookings") },
                    singleLine = true,
                )
            }
            if (mode !in listOf("activity", "messages"))
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("all", "requested", "active", "completed", "cancelled", "rejected")
                            .forEach { filter ->
                                FilterChip(
                                    selected == filter,
                                    { selected = filter },
                                    label = { Text(serviceStatus(filter)) },
                                )
                            }
                    }
                }
            if (mode == "messages")
                item {
                    Text(
                        "Open a booking to message its other participant.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            if (!state.loading && bookings.isEmpty())
                item {
                    InfoCard(
                        "No bookings to show",
                        "New service requests will appear here. You can refresh whenever you need.",
                    )
                }
            items(bookings, key = { it.id }) { booking ->
                BookingSummary(booking, provider) { onOpen(booking.id) }
            }
            item {
                Text(
                    "Showing up to your latest 200 bookings. Times are in Sri Lanka time.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun ConnectedBookingDetails(
    id: String,
    vm: MarketplaceViewModel,
    provider: Boolean = false,
    onBack: () -> Unit,
    confirmation: Boolean = false,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val detail: BookingDetailViewModel = viewModel(key = "booking-$id")
    val current by detail.state.collectAsStateWithLifecycle()
    LaunchedEffect(id) { detail.open(id) }
    ObserveServiceUpdates(detail::refresh)
    var message by rememberSaveable(id) { mutableStateOf("") }
    var action by remember { mutableStateOf<String?>(null) }
    var amount by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }
    val booking = current.booking
    MarketplacePage(
        if (confirmation) "Request sent" else booking?.reference ?: "Booking details",
        state.copy(loading = state.loading || current.loading),
        {
            vm.refresh()
            detail.refresh()
        },
        onBack,
    ) {
        current.error?.let { item { InfoCard("Could not refresh", it, error = true) } }
        if (booking == null && !current.loading)
            item {
                InfoCard(
                    "Booking unavailable",
                    "Sign in with the account that owns this booking and refresh.",
                )
            }
        if (booking != null) {
            if (provider)
                item {
                    WorkspaceDepthHeader(
                        booking.categoryName.ifBlank { serviceStatus(booking.categorySlug) },
                        "${serviceStatus(booking.status)} / ${booking.durationMinutes} minutes reserved",
                        Icons.Rounded.Handyman,
                    )
                }
            if (confirmation)
                item {
                    InfoCard(
                        "Your request is saved",
                        "The provider can now review your service request. Its status below updates as the job progresses.",
                    )
                }
            item {
                InfoCard(serviceStatus(booking.status), serviceTime(booking.scheduledAt)) {
                    Text("Customer: ${booking.customerName.ifBlank { "Customer" }}")
                    Text("Professional: ${booking.providerName.ifBlank { "Service provider" }}")
                    Text(
                        "Service: ${booking.categoryName.ifBlank { serviceStatus(booking.categorySlug) }}"
                    )
                    Text("Reserved time: ${booking.durationMinutes} minutes")
                    Text(booking.serviceAddress)
                    Text(booking.problemDescription)
                }
            }
            item {
                InfoCard(
                    "Direct payment",
                    if (booking.finalAmount == null)
                        "Agree the price with your provider before work starts. Pay the provider directly after service."
                    else
                        "Final amount: ${booking.currency} ${String.format(Locale.US,"%.2f",booking.finalAmount)}\n${if(booking.directPaymentReceived)"Provider has recorded payment received." else "Payment has not been recorded as received."}",
                )
            }
            if (provider && !booking.isClosed)
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        when (booking.status) {
                            "requested" -> {
                                Button({ action = "accepted" }, enabled = !state.saving) {
                                    Text("Accept request")
                                }
                                OutlinedButton({ action = "rejected" }, enabled = !state.saving) {
                                    Text("Decline")
                                }
                            }
                            "accepted" ->
                                Button({ action = "in_progress" }, enabled = !state.saving) {
                                    Text("Start job")
                                }
                            "in_progress" ->
                                Button({ action = "completed" }, enabled = !state.saving) {
                                    Text("Complete job")
                                }
                        }
                    }
                }
            if (!provider && booking.customerCanCancel)
                item {
                    OutlinedButton({ action = "cancelled" }, enabled = !state.saving) {
                        Text("Cancel booking")
                    }
                }
            if (
                provider &&
                    booking.status == "completed" &&
                    booking.finalAmount != null &&
                    !booking.directPaymentReceived
            )
                item {
                    Button({ action = "payment" }, enabled = !state.saving) {
                        Text("Record payment received")
                    }
                }
            item {
                InfoCard("Booking history", "") {
                    if (current.events.isEmpty()) Text("No recorded changes yet.")
                    current.events.forEach { event ->
                        Text(
                            "${serviceStatus(event.kind)} · ${serviceTime(event.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item { Text("Conversation", style = MaterialTheme.typography.titleLarge) }
            if (current.messages.isEmpty())
                item {
                    Text(
                        "Message your ${if(provider)"customer" else "provider"} about this service.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            items(current.messages, key = { it.id }) { chat ->
                InfoCard(
                    if (chat.senderId == state.data.profile?.id) "You"
                    else if (provider) booking.customerName.ifBlank { "Customer" }
                    else booking.providerName.ifBlank { "Provider" },
                    chat.body,
                ) {
                    Text(serviceTime(chat.createdAt), style = MaterialTheme.typography.labelSmall)
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        message,
                        { if (it.length <= 2000) message = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Write a message") },
                        minLines = 2,
                        enabled = !state.saving,
                    )
                    Button(
                        {
                            val body = message
                            vm.runAction {
                                sendMessage(id, body)
                                message = ""
                                detail.refresh()
                            }
                        },
                        enabled = message.isNotBlank() && !state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Send message")
                    }
                    Text(
                        "Messages update while this conversation is open. Showing the latest 100 messages.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
    action?.let { next ->
        AlertDialog(
            onDismissRequest = { if (!state.saving) action = null },
            title = {
                Text(if (next == "payment") "Confirm money received" else serviceStatus(next) + "?")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        when (next) {
                            "payment" ->
                                "Only confirm after you have actually received the customer's payment. This records a receipt; Fixoom does not transfer money."
                            "completed" ->
                                "Enter the final amount agreed with the customer in LKR. Completion closes the job."
                            "cancelled" ->
                                "Cancel this service request for both you and the provider?"
                            else -> "This change will be saved to the booking history."
                        }
                    )
                    if (next == "completed")
                        OutlinedTextField(
                            amount,
                            {
                                amount = it
                                amountError = false
                            },
                            label = { Text("Final agreed amount (LKR)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = amountError,
                            singleLine = true,
                        )
                    if (amountError)
                        Text(
                            "Enter an amount from 0 to 10,000,000.",
                            color = MaterialTheme.colorScheme.error,
                        )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val number = amount.toBigDecimalOrNull()
                        if (
                            next == "completed" &&
                                (number == null ||
                                    number.signum() < 0 ||
                                    number > java.math.BigDecimal("10000000"))
                        )
                            amountError = true
                        else {
                            action = null
                            vm.runAction {
                                when (next) {
                                    "completed" -> complete(id, number!!.toDouble())
                                    "payment" -> confirmPayment(id)
                                    else -> changeStatus(id, next)
                                }
                                detail.refresh()
                            }
                        }
                    },
                    enabled = !state.saving,
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton({ action = null }, enabled = !state.saving) { Text("Go back") }
            },
        )
    }
}

@Composable
fun ConnectedProviderProfile(
    vm: MarketplaceViewModel,
    pending: Boolean = false,
    onBack: () -> Unit,
    onCheckApproval: () -> Unit,
    onSignOut: () -> Unit,
    checkingApproval: Boolean = false,
    approvalMessage: String? = null,
    approvalError: String? = null,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ProviderProfileScreens(
        state = state,
        pending = pending,
        onRefresh = vm::refresh,
        onBack = onBack,
        onSaveProfile = { name, category, area, bio, available, done ->
            vm.runAction(onSaved = done) { saveProfessional(name, category, area, bio, available) }
        },
        onSavePhoto = vm::saveProviderPhoto,
        onCheckApproval = onCheckApproval,
        onSignOut = onSignOut,
        checkingApproval = checkingApproval,
        approvalMessage = approvalMessage,
        approvalError = approvalError,
        onRequestSkill = { category, description, done ->
            vm.runAction(onSaved = done) { requestSkill(category, description) }
        },
        onWithdrawSkill = { category -> vm.runAction { withdrawSkill(category) } },
    )
}

@Composable
fun ConnectedReceipts(vm: MarketplaceViewModel, onOpen: (String) -> Unit, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val completed = state.data.bookings.filter { it.status == "completed" }
    val received = completed.filter { it.directPaymentReceived }.sumOf { it.finalAmount ?: 0.0 }
    val list = rememberLazyListState()
    MarketplacePage("Direct payment records", state, vm::refresh, onBack, listState = list) {
        item {
            WorkspaceDepthHeader(
                "Work completed.\nPayments recorded.",
                "Your real completed jobs and direct payment receipts.",
                Icons.Rounded.ReceiptLong,
                { list.firstVisibleItemScrollOffset.toFloat() },
            )
        }
        item {
            InfoCard(
                "Recorded as received",
                "LKR ${String.format(Locale.US,"%.2f",received)}\nBased on your latest 200 bookings. These are provider-recorded receipts, not payments processed by Fixoom.",
            )
        }
        if (completed.isEmpty())
            item {
                InfoCard(
                    "No completed jobs yet",
                    "Completed services and their agreed amounts will appear here.",
                )
            }
        items(completed, key = { it.id }) { booking ->
            BookingSummary(booking, true) { onOpen(booking.id) }
        }
    }
}
