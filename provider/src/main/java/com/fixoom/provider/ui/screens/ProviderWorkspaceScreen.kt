package com.fixoom.provider.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fixoom.core.data.repository.MarketplaceViewModel
import com.fixoom.core.model.AccountStatus
import com.fixoom.marketplace.ui.*
import com.fixoom.provider.ui.auth.ProviderAuthViewModel
import com.fixoom.provider.ui.screens.dashboard.ProviderDashboardScreen

@Composable
fun ProviderWorkspaceScreen(auth: ProviderAuthViewModel, pending: Boolean = false) {
    val market: MarketplaceViewModel = viewModel()
    val state by market.state.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(if (pending) "profile" else "home") }
    var selectedBooking by rememberSaveable { mutableStateOf<String?>(null) }
    ObserveMarketplace(market)
    LaunchedEffect(state.data.profile?.accountStatus) {
        val status = state.data.profile?.accountStatus
        if (
            status != null &&
                ((pending && status == AccountStatus.ACTIVE) ||
                    (!pending && status != AccountStatus.ACTIVE))
        )
            auth.restoreSession()
    }
    val home: () -> Unit = {
        section = "home"
        selectedBooking = null
    }
    BackHandler(selectedBooking != null || (!pending && section != "home"), onBack = home)
    Box(
        Modifier.fillMaxSize()
            .then(
                if (section == "home" && !pending && selectedBooking == null) Modifier
                else Modifier.safeDrawingPadding()
            )
    ) {
        when {
            selectedBooking != null ->
                ConnectedBookingDetails(
                    selectedBooking!!,
                    market,
                    provider = true,
                    onBack = { selectedBooking = null },
                )
            pending || section == "profile" ->
                ConnectedProviderProfile(market, pending, home, auth::restoreSession, auth::signOut)
            section == "receipts" -> ConnectedReceipts(market, { selectedBooking = it }, home)
            section != "home" ->
                ConnectedBookings(
                    market,
                    provider = true,
                    mode = section,
                    onOpen = { selectedBooking = it },
                    onAccount = { section = "profile" },
                    onBack = home,
                )
            state.data.profile != null ->
                Column(Modifier.fillMaxSize()) {
                    state.error?.let { InfoCard("Unable to update", it, error = true) }
                    ProviderDashboardScreen(
                        profile = state.data.profile!!,
                        availability =
                            state.data.ownService
                                ?.takeIf { it.categorySlug != null && it.serviceArea.isNotBlank() }
                                ?.isAvailable,
                        onAvailabilityChange = { available ->
                            state.data.ownService?.let { service ->
                                market.runAction {
                                    saveProfessional(
                                        service.displayName,
                                        service.categorySlug.orEmpty(),
                                        service.serviceArea,
                                        service.bio,
                                        available,
                                    )
                                }
                            }
                        },
                        onOpenJobRequests = { section = "requests" },
                        onOpenActiveJobs = { section = "active" },
                        onOpenSchedule = { section = "schedule" },
                        onOpenEarnings = { section = "receipts" },
                        onOpenProfile = { section = "profile" },
                        onOpenNotifications = { section = "activity" },
                        onSignOut = auth::signOut,
                        modifier = Modifier.weight(1f),
                    )
                }
            else ->
                MarketplacePage("Your workspace", state, market::refresh) {
                    item {
                        InfoCard(
                            "Loading your account",
                            "Your service profile and jobs will appear here.",
                        )
                    }
                }
        }
    }
}
