package com.fixoom.customer.ui.notifications

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.fixoom.core.data.notifications.PushState
import com.fixoom.core.data.repository.MarketplaceSnapshot
import com.fixoom.core.data.repository.MarketplaceState
import com.fixoom.core.model.*
import com.fixoom.customer.ui.theme.Fixoom_ServicesTheme
import com.fixoom.marketplace.ui.NotificationsInbox
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Presentation fixtures only. No repositories, sessions or production notification writes. */
class CustomerNotificationsTest {
    @get:Rule val compose = createComposeRule()
    private val first =
        BookingNotification(
            "customer-fixture-unread",
            "fixture-booking-a",
            "accepted",
            "Fixture accepted",
            "Your fixture booking has been accepted.",
            "2026-10-18T09:00:00Z",
        )
    private val second =
        BookingNotification(
            "customer-fixture-read",
            "fixture-booking-b",
            "message",
            "Fixture message",
            "Open the fixture booking to read the reply.",
            "2026-10-17T09:00:00Z",
            "2026-10-17T10:00:00Z",
        )
    private val initial =
        MarketplaceState(
            MarketplaceSnapshot(
                profile =
                    Profile("customer-fixture-profile", AppRole.CUSTOMER, AccountStatus.ACTIVE),
                notifications = listOf(first, second),
            ),
            loading = false,
        )
    private val connected = PushState(configured = true, registered = true)

    private fun show(
        model: State<MarketplaceState> = mutableStateOf(initial),
        push: PushState = connected,
        allowed: Boolean = true,
        dark: Boolean = false,
        fontScale: Float = 1f,
        motion: State<Boolean> = mutableStateOf(false),
        read: (String?) -> Unit = {},
        open: (String) -> Unit = {},
        enable: () -> Unit = {},
        refresh: () -> Unit = {},
        account: () -> Unit = {},
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Fixoom_ServicesTheme(darkTheme = dark) {
                    NotificationsInbox(
                        model.value,
                        push,
                        allowed,
                        refresh,
                        {},
                        account,
                        open,
                        read,
                        {},
                        enable,
                        motionEnabled = motion.value,
                        customerDesign = true,
                    )
                }
            }
        }
    }

    private fun scrollTo(matcher: SemanticsMatcher) {
        compose.onNodeWithTag("customer-inbox-list").performScrollToNode(matcher)
    }

    @Test
    fun consumerLayoutFiltersSavedStateAndOpensExactBookingOnlyOnTap() {
        val reads = mutableListOf<String?>()
        val opened = mutableListOf<String>()
        show(
            model = mutableStateOf(initial.copy(notice = "Unrelated fixture success")),
            read = { reads += it },
            open = { opened += it },
        )
        compose.onNodeWithText("In the\nknow.").assertIsDisplayed()
        compose.onNodeWithText("Stay in\nthe loop.").assertDoesNotExist()
        compose.onNodeWithText("Unrelated fixture success").assertDoesNotExist()
        scrollTo(hasTestTag("inbox-filter-unread"))
        compose.onNodeWithTag("inbox-filter-unread").performClick().assertIsSelected()
        compose.onNodeWithText("Unread (1)").assertExists()
        compose.onNodeWithText("All (2)").assertExists()
        compose.onNodeWithTag("notification-customer-fixture-read").assertDoesNotExist()
        scrollTo(hasTestTag("notification-customer-fixture-unread"))
        compose.runOnIdle {
            assertTrue(reads.isEmpty())
            assertTrue(opened.isEmpty())
        }
        compose.onNodeWithTag("notification-customer-fixture-unread").performClick()
        compose.runOnIdle {
            assertEquals(listOf("customer-fixture-unread"), reads)
            assertEquals(listOf("fixture-booking-a"), opened)
        }
        scrollTo(hasTestTag("inbox-filter-all"))
        compose.onNodeWithTag("inbox-filter-all").performClick()
        scrollTo(hasTestTag("notification-customer-fixture-read"))
        compose.onNodeWithTag("notification-customer-fixture-read").assertIsDisplayed()
    }

    @Test
    fun markAllAndSavingStatePreserveWriteGuards() {
        val model = mutableStateOf(initial)
        val reads = mutableListOf<String?>()
        show(model, read = { reads += it })
        scrollTo(hasContentDescription("Mark all as read"))
        compose.onNodeWithContentDescription("Mark all as read").performClick()
        compose.runOnIdle {
            assertEquals(listOf<String?>(null), reads)
            model.value = initial.copy(saving = true)
        }
        scrollTo(hasContentDescription("Mark all as read"))
        compose.onNodeWithContentDescription("Mark all as read").assertIsNotEnabled()
        scrollTo(hasTestTag("notification-customer-fixture-unread"))
        compose.onNodeWithTag("notification-customer-fixture-unread").assertIsNotEnabled()
    }

    @Test
    fun deniedPhonePermissionKeepsWorkingRecoveryInsteadOfConnectedClaim() {
        var enabled = false
        show(allowed = false, enable = { enabled = true })
        scrollTo(hasText("Enable phone alerts"))
        compose.onNodeWithText("Phone alerts connected").assertDoesNotExist()
        compose.onNodeWithText("Enable phone alerts").performClick()
        compose.runOnIdle { assertTrue(enabled) }
    }

    @Test
    fun emptyUnreadReturnsToAllAndErrorOffersRetryWithoutFalseSuccess() {
        val model =
            mutableStateOf(initial.copy(data = initial.data.copy(notifications = listOf(second))))
        var refreshed = false
        show(model, refresh = { refreshed = true })
        scrollTo(hasTestTag("inbox-filter-unread"))
        compose.onNodeWithTag("inbox-filter-unread").performClick()
        scrollTo(hasText("Show all updates"))
        compose.onNodeWithText("Show all updates").performClick()
        scrollTo(hasTestTag("notification-customer-fixture-read"))
        compose.onNodeWithTag("notification-customer-fixture-read").assertIsDisplayed()
        compose.runOnIdle {
            model.value =
                initial.copy(
                    data = initial.data.copy(notifications = emptyList()),
                    error = "Fixture refresh failed",
                )
        }
        scrollTo(hasText("Try again"))
        compose.onNodeWithText("Fixture refresh failed").assertExists()
        compose.onNodeWithText("All updates read").assertDoesNotExist()
        compose.onNodeWithText("A little quiet, for now").assertDoesNotExist()
        compose.onNodeWithText("Try again").performClick()
        compose.runOnIdle { assertTrue(refreshed) }
    }

    @Test
    fun darkThemeWithLargeTextKeepsActionsAndFullUpdateBodyReachable() {
        var booking: String? = null
        show(dark = true, fontScale = 1.6f, open = { booking = it })
        scrollTo(hasTestTag("inbox-filter-unread"))
        compose.onNodeWithTag("inbox-filter-unread").assertIsDisplayed().performClick()
        scrollTo(hasTestTag("notification-customer-fixture-unread"))
        compose.onNodeWithText(first.body).assertIsDisplayed()
        compose.onNodeWithTag("notification-customer-fixture-unread").performClick()
        compose.runOnIdle { assertEquals("fixture-booking-a", booking) }
    }

    @Test
    fun sculptureMovesWhileHeadlineStaysStillAndReducedMotionStopsFrames() {
        compose.mainClock.autoAdvance = false
        val motion = mutableStateOf(true)
        show(motion = motion)
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
        val title = compose.onNodeWithText("In the\nknow.").fetchSemanticsNode().boundsInRoot
        val before = compose.onRoot().captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(1200)
        compose.waitForIdle()
        assertFalse(before.sameAs(compose.onRoot().captureToImage().asAndroidBitmap()))
        assertEquals(
            title,
            compose.onNodeWithText("In the\nknow.").fetchSemanticsNode().boundsInRoot,
        )
        compose.runOnIdle { motion.value = false }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val still = compose.onRoot().captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(1600)
        compose.waitForIdle()
        assertTrue(still.sameAs(compose.onRoot().captureToImage().asAndroidBitmap()))
    }
}
