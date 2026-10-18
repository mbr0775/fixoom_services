package com.fixoom.customer.ui.booking

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.fixoom.core.data.repository.*
import com.fixoom.core.model.*
import com.fixoom.customer.ui.theme.Fixoom_ServicesTheme
import com.fixoom.marketplace.ui.CustomerBookingDetailsScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Isolated presentation fixtures. Does not create repositories, bookings, chats or uploads. */
class CustomerBookingDetailsTest {
    @get:Rule val compose = createComposeRule()
    private val booking =
        ServiceBooking(
            "fixture-booking",
            "FX-FIXTURE",
            "fixture-customer",
            "fixture-provider",
            "plumbing",
            "2026-10-20T04:30:00Z",
            "Fixture service address",
            "Fixture leaking tap description",
            "requested",
            "2026-10-18T04:30:00Z",
            providerName = "Fixture professional",
            customerName = "Fixture customer",
            categoryName = "Plumbing",
            durationMinutes = 60,
        )
    private val initial = BookingDetailState(booking = booking, loading = false)
    private val global =
        MarketplaceState(
            MarketplaceSnapshot(
                profile = Profile("fixture-customer", AppRole.CUSTOMER, AccountStatus.ACTIVE)
            ),
            loading = false,
        )

    private fun show(
        detail: State<BookingDetailState> = mutableStateOf(initial),
        state: State<MarketplaceState> = mutableStateOf(global),
        draft: MutableState<String> = mutableStateOf(""),
        dark: Boolean = false,
        scale: Float = 1f,
        send: (String) -> Unit = {},
        cancel: () -> Unit = {},
        support: () -> Unit = {},
        map: (JobLocation) -> Unit = {},
        refresh: () -> Unit = {},
        back: () -> Unit = {},
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                Fixoom_ServicesTheme(darkTheme = dark) {
                    CustomerBookingDetailsScreen(
                        state.value,
                        detail.value,
                        draft.value,
                        { if (it.length <= 2000) draft.value = it },
                        send,
                        refresh,
                        back,
                        cancel,
                        support,
                        map,
                        { Text("Fixture attachment controls") },
                        motionEnabled = false,
                    )
                }
            }
        }
    }

    private fun scrollTo(matcher: SemanticsMatcher) {
        compose.onNodeWithTag("customer-booking-list").performScrollToNode(matcher)
    }

    private fun tab(label: String) {
        scrollTo(hasTestTag("booking-tab-$label"))
        compose.onNodeWithTag("booking-tab-$label").performClick().assertIsSelected()
    }

    @Test
    fun overviewShowsSavedBookingDetailsAndProviderShortcutOpensMessages() {
        show()
        compose.onNodeWithText("FX-FIXTURE").assertIsDisplayed()
        compose.onNodeWithTag("booking-current-status").assertTextEquals("Awaiting reply")
        scrollTo(hasText("20 Oct 2026, 10:00 am"))
        compose.onNodeWithText("60 minutes reserved").assertExists()
        scrollTo(hasText(booking.serviceAddress))
        compose.onNodeWithText(booking.problemDescription).assertExists()
        scrollTo(hasText("Price to be agreed"))
        compose.onNodeWithText("Price to be agreed").assertIsDisplayed()
        scrollTo(hasText("Message provider"))
        compose.onNodeWithText("Message provider").performClick()
        compose.onNodeWithTag("booking-tab-Messages").assertIsSelected()
        compose.onNodeWithTag("booking-message-input").assertIsDisplayed()
    }

    @Test
    fun cancellationRequiresConfirmationAndClosedJobsDoNotOfferIt() {
        var calls = 0
        val model = mutableStateOf(initial)
        show(detail = model, cancel = { calls++ })
        scrollTo(hasText("Cancel booking"))
        compose.onNodeWithText("Cancel booking").performClick()
        compose.runOnIdle { assertEquals(0, calls) }
        compose.onNodeWithText("Keep booking").performClick()
        compose.runOnIdle { assertEquals(0, calls) }
        compose.onNodeWithText("Cancel booking").performClick()
        compose.onNodeWithText("Confirm cancellation").performClick()
        compose.runOnIdle {
            assertEquals(1, calls)
            model.value = initial.copy(booking = booking.copy(status = "completed"))
        }
        scrollTo(hasText("Payment"))
        compose.onNodeWithText("Cancel booking").assertDoesNotExist()
    }

    @Test
    fun messagesKeepDraftOnFailureAndSavingDisablesDuplicateSends() {
        val state = mutableStateOf(global)
        val draft = mutableStateOf("")
        val sent = mutableListOf<String>()
        show(state = state, draft = draft, send = { sent += it })
        tab("Messages")
        compose.onNodeWithText("Send message").assertIsNotEnabled()
        compose.onNodeWithTag("booking-message-input").performTextInput("Fixture timing question")
        compose.onNodeWithText("Send message").performClick()
        compose.runOnIdle {
            assertEquals(listOf("Fixture timing question"), sent)
            state.value = global.copy(saving = true)
        }
        compose.onNodeWithText("Sending…").assertIsNotEnabled()
        compose.onNodeWithTag("booking-message-input").assertIsNotEnabled()
        compose.runOnIdle { state.value = global.copy(error = "Fixture message could not save") }
        compose.onNodeWithTag("booking-message-input").assertTextContains("Fixture timing question")
    }

    @Test
    fun historyAndConversationRenderOnlySavedEventsAndMessages() {
        val events =
            listOf(
                BookingEvent("event-a", booking.id, "requested", "2026-10-18T04:30:00Z"),
                BookingEvent("event-b", booking.id, "accepted", "2026-10-18T05:00:00Z"),
            )
        val messages =
            listOf(
                BookingMessage(
                    "message-a",
                    booking.id,
                    "fixture-provider",
                    "Fixture provider reply",
                    "2026-10-18T05:10:00Z",
                ),
                BookingMessage(
                    "message-b",
                    booking.id,
                    "fixture-customer",
                    "Fixture customer reply",
                    "2026-10-18T05:15:00Z",
                ),
            )
        show(detail = mutableStateOf(initial.copy(events = events, messages = messages)))
        tab("History")
        scrollTo(hasText("Accepted"))
        compose.onNodeWithText("18 Oct 2026, 10:30 am").assertExists()
        compose.onNodeWithText("Completed").assertDoesNotExist()
        tab("Messages")
        scrollTo(hasText("Fixture provider reply"))
        compose.onNodeWithText("Fixture provider reply").assertIsDisplayed()
        scrollTo(hasText("Fixture customer reply"))
        compose.onNodeWithText("You").assertExists()
        compose.onNodeWithText("Fixture customer reply").assertIsDisplayed()
    }

    @Test
    fun locationDistinguishesLoadingFailureNotSharedAndRealPosition() {
        val model =
            mutableStateOf(
                initial.copy(booking = booking.copy(status = "accepted"), extrasLoading = true)
            )
        var mapped: JobLocation? = null
        show(detail = model, map = { mapped = it })
        scrollTo(hasText("Refreshing live location…"))
        compose.onNodeWithText("View provider position on map").assertDoesNotExist()
        compose.runOnIdle {
            model.value =
                model.value.copy(extrasLoading = false, locationError = "Fixture location failure")
        }
        scrollTo(hasText("Fixture location failure"))
        compose.onNodeWithText("Fixture location failure").assertIsDisplayed()
        compose
            .onNodeWithText(
                "Live location is not currently shared, or the last position has expired."
            )
            .assertDoesNotExist()
        compose.runOnIdle { model.value = model.value.copy(locationError = null) }
        scrollTo(
            hasText("Live location is not currently shared, or the last position has expired.")
        )
        val position = JobLocation(booking.id, 6.9, 79.8, 12.0, "2026-10-18T05:30:00Z")
        compose.runOnIdle { model.value = model.value.copy(location = position) }
        scrollTo(hasText("View provider position on map"))
        compose.onNodeWithText("View provider position on map").performClick()
        compose.runOnIdle { assertEquals(position, mapped) }
    }

    @Test
    fun darkLargeTextReceiptMediaAndSupportRemainReachable() {
        var support = false
        show(
            detail =
                mutableStateOf(
                    initial.copy(
                        booking =
                            booking.copy(
                                status = "completed",
                                finalAmount = 3750.0,
                                directPaymentReceived = true,
                            )
                    )
                ),
            dark = true,
            scale = 1.6f,
            support = { support = true },
        )
        scrollTo(hasText("LKR 3750.00"))
        compose.onNodeWithText("Provider has recorded payment received.").assertExists()
        scrollTo(hasText("Open photos & videos"))
        compose.onNodeWithText("Open photos & videos").performClick()
        scrollTo(hasText("Fixture attachment controls"))
        compose.onNodeWithText("Fixture attachment controls").assertIsDisplayed()
        scrollTo(hasText("Report an issue privately"))
        compose.onNodeWithText("Report an issue privately").performClick()
        compose.runOnIdle { assertTrue(support) }
    }

    @Test
    fun unavailableBookingHasRecoveryAndNoPrivateControlsOrFalseConfirmation() {
        var refreshed = false
        var back = false
        show(
            detail =
                mutableStateOf(
                    BookingDetailState(loading = false, error = "Fixture booking read failed")
                ),
            refresh = { refreshed = true },
            back = { back = true },
        )
        compose.onNodeWithText("Fixture booking read failed").assertExists()
        scrollTo(hasText("Refresh booking"))
        compose.onNodeWithText("Refresh booking").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle {
            assertTrue(refreshed)
            assertTrue(back)
        }
        compose.onNodeWithTag("booking-tab-Messages").assertDoesNotExist()
        compose.onNodeWithText("Your request is saved to this booking.").assertDoesNotExist()
    }
}
