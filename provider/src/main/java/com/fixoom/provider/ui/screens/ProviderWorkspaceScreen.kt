package com.fixoom.provider.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fixoom.core.data.location.JobLocationSharing
import com.fixoom.core.data.repository.MarketplaceViewModel
import com.fixoom.core.model.AccountStatus
import com.fixoom.marketplace.ui.*
import com.fixoom.provider.ui.auth.ProviderAuthViewModel
import com.fixoom.provider.ui.components.rememberProviderMotionAllowed
import com.fixoom.provider.ui.screens.dashboard.ProviderDashboardScreen

@Composable
fun ProviderWorkspaceScreen(
    auth: ProviderAuthViewModel,
    pending: Boolean = false,
    notificationBooking: String? = null,
    onNotificationConsumed: () -> Unit = {},
) {
    val appContext = LocalContext.current.applicationContext
    val market: MarketplaceViewModel = viewModel()
    val state by market.state.collectAsStateWithLifecycle()
    val approval by auth.approvalStatus.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(if (pending) "profile" else "home") }
    var selectedBooking by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(notificationBooking) {
        if (!pending && notificationBooking != null) {
            selectedBooking = notificationBooking
            onNotificationConsumed()
        }
    }
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
    val motion = rememberProviderMotionAllowed()
    AnimatedContent(
        targetState = selectedBooking to section,
        transitionSpec = {
            if (motion)
                (fadeIn(tween(210)) + slideInHorizontally(tween(250)) { it / 14 }).togetherWith(
                    fadeOut(tween(120))
                )
            else fadeIn(snap()).togetherWith(fadeOut(snap()))
        },
        label = "provider_workspace_navigation",
    ) { (bookingId, destination) ->
        Box(
            Modifier.fillMaxSize()
                .then(
                    if (destination == "home" && !pending && bookingId == null) Modifier
                    else Modifier.safeDrawingPadding()
                )
        ) {
            when {
                bookingId != null ->
                    ConnectedBookingDetails(
                        bookingId,
                        market,
                        provider = true,
                        onBack = { selectedBooking = null },
                    )
                pending || destination == "profile" ->
                    ConnectedProviderProfile(
                        market,
                        pending,
                        home,
                        auth::checkApprovalStatus,
                        {
                            JobLocationSharing.stop(appContext)
                            auth.signOut()
                        },
                        checkingApproval = approval.checking,
                        approvalMessage = approval.message,
                        approvalError = approval.error,
                    )
                destination == "activity" ->
                    ConnectedNotifications(
                        market,
                        { selectedBooking = it },
                        { section = "profile" },
                        home,
                    )
                destination == "receipts" ->
                    ConnectedReceipts(market, { selectedBooking = it }, home)
                destination != "home" ->
                    ConnectedBookings(
                        market,
                        provider = true,
                        mode = destination,
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
                                    ?.takeIf { service ->
                                        state.data.categories.any { service.offers(it.slug) } &&
                                            service.serviceArea.isNotBlank()
                                    }
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
                            onOpenHistory = { section = "history" },
                            onOpenEarnings = { section = "receipts" },
                            onOpenProfile = { section = "profile" },
                            onOpenNotifications = { section = "activity" },
                            onSignOut = {
                                JobLocationSharing.stop(appContext)
                                auth.signOut()
                            },
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
}
