package com.fixoom.provider.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.fixoom.core.data.notifications.PushState
import com.fixoom.core.data.repository.MarketplaceSnapshot
import com.fixoom.core.data.repository.MarketplaceState
import com.fixoom.core.model.*
import com.fixoom.marketplace.ui.NotificationsInbox
import com.fixoom.provider.ui.theme.Fixoom_ServicesTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Isolated presentation fixtures. Does not initialize repositories or write real account data. */
class NotificationsInboxTest {
    @get:Rule val compose = createComposeRule()
    private val unread =
        BookingNotification(
            "unread-1",
            "booking-1",
            "requested",
            "Fixture request",
            "Fixture booking request details.",
            "2026-10-18T09:00:00Z",
        )
    private val read =
        BookingNotification(
            "read-2",
            "booking-2",
            "accepted",
            "Fixture accepted",
            "Fixture acceptance details.",
            "2026-10-17T09:00:00Z",
            "2026-10-17T10:00:00Z",
        )
    private val profile = Profile("fixture-profile", AppRole.PROVIDER, AccountStatus.ACTIVE)
    private val state =
        MarketplaceState(
            MarketplaceSnapshot(profile = profile, notifications = listOf(unread, read)),
            loading = false,
        )
    private val connected = PushState(configured = true, registered = true)

    private fun show(
        state: MarketplaceState = this.state,
        push: PushState = connected,
        allowed: Boolean = true,
        onRead: (String?) -> Unit = {},
        onOpen: (String) -> Unit = {},
        onEnable: () -> Unit = {},
        onAccount: () -> Unit = {},
        onRefresh: () -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        compose.setContent {
            Fixoom_ServicesTheme {
                NotificationsInbox(
                    state,
                    push,
                    allowed,
                    onRefresh,
                    onBack,
                    onAccount,
                    onOpen,
                    onRead,
                    {},
                    onEnable,
                    motionEnabled = false,
                )
            }
        }
    }

    private fun scrollTo(matcher: SemanticsMatcher) {
        compose.onNode(hasScrollAction()).performScrollToNode(matcher)
    }

    @Test
    fun filtersUseSavedReadStateAndTapKeepsExactBookingAndNotificationIds() {
        val marked = mutableListOf<String?>()
        val opened = mutableListOf<String>()
        show(
            state.copy(notice = "Stale fixture success"),
            onRead = { marked += it },
            onOpen = { opened += it },
        )
        compose.onNodeWithText("Stale fixture success").assertDoesNotExist()
        scrollTo(hasTestTag("inbox-filter-unread"))
        compose.onNodeWithTag("inbox-filter-unread").performClick().assertIsSelected()
        compose.onNodeWithText("Unread (1)").assertExists()
        compose.onNodeWithText("All (2)").assertExists()
        compose.onNodeWithTag("notification-read-2").assertDoesNotExist()
        scrollTo(hasTestTag("notification-unread-1"))
        compose.runOnIdle {
            assertTrue(marked.isEmpty())
            assertTrue(opened.isEmpty())
        }
        compose.onNodeWithTag("notification-unread-1").performClick()
        compose.runOnIdle {
            assertEquals(listOf("unread-1"), marked)
            assertEquals(listOf("booking-1"), opened)
        }
        scrollTo(hasTestTag("inbox-filter-all"))
        compose.onNodeWithTag("inbox-filter-all").performClick()
        scrollTo(hasTestTag("notification-read-2"))
        compose.onNodeWithTag("notification-read-2").assertIsDisplayed()
    }

    @Test
    fun markAllOnlyRunsOnTapAndSavingDisablesWrites() {
        val model = mutableStateOf(state)
        val calls = mutableListOf<String?>()
        compose.setContent {
            Fixoom_ServicesTheme {
                NotificationsInbox(
                    model.value,
                    connected,
                    true,
                    {},
                    {},
                    {},
                    {},
                    { calls += it },
                    {},
                    {},
                    motionEnabled = false,
                )
            }
        }
        scrollTo(hasText("Mark all as read"))
        compose.runOnIdle { assertTrue(calls.isEmpty()) }
        compose.onNodeWithText("Mark all as read").performClick()
        compose.runOnIdle {
            assertEquals(listOf<String?>(null), calls)
            model.value = state.copy(saving = true)
        }
        scrollTo(hasText("Mark all as read"))
        compose.onNodeWithText("Mark all as read").assertIsNotEnabled()
        scrollTo(hasTestTag("notification-unread-1"))
        compose.onNodeWithTag("notification-unread-1").assertIsNotEnabled()
    }

    @Test
    fun androidPermissionBlockedDoesNotClaimConnectedEvenWhenTokenIsRegistered() {
        var enabled = false
        show(allowed = false, onEnable = { enabled = true })
        scrollTo(hasText("Enable phone alerts"))
        compose.onNodeWithText("Phone alerts connected").assertDoesNotExist()
        compose.onNodeWithText("Enable phone alerts").performClick()
        compose.runOnIdle { assertTrue(enabled) }
    }

    @Test
    fun guestAndBackRefreshActionsStayAvailable() {
        var signedIn = false
        var refreshed = false
        var backed = false
        show(
            MarketplaceState(loading = false),
            onAccount = { signedIn = true },
            onRefresh = { refreshed = true },
            onBack = { backed = true },
        )
        scrollTo(hasText("Sign in"))
        compose.onNodeWithText("Sign in").performClick()
        compose.onNodeWithContentDescription("Refresh").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle {
            assertTrue(signedIn)
            assertTrue(refreshed)
            assertTrue(backed)
        }
        compose.onNodeWithText("All updates read").assertDoesNotExist()
    }

    @Test
    fun emptyUnreadCanReturnToAllAndErrorsHaveNoFalseSuccess() {
        val model = mutableStateOf(state.copy(data = state.data.copy(notifications = listOf(read))))
        compose.setContent {
            Fixoom_ServicesTheme {
                NotificationsInbox(
                    model.value,
                    PushState(),
                    true,
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    motionEnabled = false,
                )
            }
        }
        scrollTo(hasTestTag("inbox-filter-unread"))
        compose.onNodeWithTag("inbox-filter-unread").performClick()
        scrollTo(hasText("Show all updates"))
        compose.onNodeWithText("You’re all caught up").assertExists()
        compose.onNodeWithText("Show all updates").performClick()
        scrollTo(hasTestTag("notification-read-2"))
        compose.onNodeWithTag("notification-read-2").assertIsDisplayed()
        compose.runOnIdle {
            model.value =
                state.copy(
                    data = state.data.copy(notifications = emptyList()),
                    error = "Fixture network failure",
                )
        }
        scrollTo(hasText("Fixture network failure"))
        compose.onNodeWithText("Fixture network failure").assertExists()
        compose.onNodeWithText("All updates read").assertDoesNotExist()
        compose.onNodeWithText("You’re all caught up").assertDoesNotExist()
        compose.onNodeWithText("A little quiet, for now").assertDoesNotExist()
    }

    @Test
    fun largeTextAndDarkThemeKeepBookingAndPermissionControlsReachable() {
        var opened: String? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    NotificationsInbox(
                        state,
                        connected,
                        false,
                        {},
                        {},
                        {},
                        { opened = it },
                        {},
                        {},
                        {},
                        motionEnabled = false,
                    )
                }
            }
        }
        scrollTo(hasText("Enable phone alerts"))
        compose.onNodeWithText("Enable phone alerts").assertIsDisplayed()
        scrollTo(hasTestTag("inbox-filter-unread"))
        compose.onNodeWithTag("inbox-filter-unread").assertIsDisplayed()
        scrollTo(hasTestTag("notification-unread-1"))
        compose.onNodeWithTag("notification-unread-1").performClick()
        compose.runOnIdle { assertEquals("booking-1", opened) }
    }

    @Test
    fun bellAnimatesWithoutMovingTextAndReducedMotionStopsDecorativeFrames() {
        compose.mainClock.autoAdvance = false
        val motion = mutableStateOf(true)
        compose.setContent {
            Fixoom_ServicesTheme {
                NotificationsInbox(
                    state,
                    connected,
                    true,
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    motionEnabled = motion.value,
                )
            }
        }
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
        val textBounds =
            compose.onNodeWithText("Stay in\nthe loop.").fetchSemanticsNode().boundsInRoot
        val before = compose.onRoot().captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(1200)
        compose.waitForIdle()
        assertFalse(before.sameAs(compose.onRoot().captureToImage().asAndroidBitmap()))
        assertEquals(
            textBounds,
            compose.onNodeWithText("Stay in\nthe loop.").fetchSemanticsNode().boundsInRoot,
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
