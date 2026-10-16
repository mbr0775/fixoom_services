package com.fixoom.provider.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.fixoom.core.model.*
import com.fixoom.marketplace.ui.BookingTrackingCard
import com.fixoom.provider.ui.theme.Fixoom_ServicesTheme
import org.junit.Rule
import org.junit.Test

/** UI state only: never requests GPS, starts a service, or sends a notification. */
class BookingTrackingTest {
    @get:Rule val compose = createComposeRule()
    private val booking =
        ServiceBooking(
            "fixture",
            "TEST",
            "customer",
            "provider",
            "plumbing",
            "2026-11-01T09:00:00Z",
            "Test address",
            "Test service",
            "accepted",
            "2026-10-16T10:00:00Z",
        )

    @Test
    fun customerSeesHonestUnsharedStateAndNoProviderControl() {
        compose.setContent {
            Fixoom_ServicesTheme { BookingTrackingCard(booking, null, false, {}) }
        }
        compose
            .onNodeWithText(
                "Live location is not currently shared, or the last position has expired."
            )
            .assertIsDisplayed()
        compose.onNodeWithText("Share live location for this job").assertDoesNotExist()
        compose.onNodeWithText("View provider position on map").assertDoesNotExist()
    }

    @Test
    fun completedJobsHaveNoLocationControlsOrLiveMap() {
        compose.setContent {
            Fixoom_ServicesTheme {
                BookingTrackingCard(booking.copy(status = "completed"), null, true, {})
            }
        }
        compose.onNodeWithText("Service completed.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Share live location for this job").assertDoesNotExist()
    }
}
