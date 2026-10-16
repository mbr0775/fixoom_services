package com.fixoom.provider.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.fixoom.core.data.repository.BookingAvailabilityState
import com.fixoom.core.model.BusyTime
import com.fixoom.marketplace.ui.BookingAvailabilityPanel
import com.fixoom.provider.ui.theme.Fixoom_ServicesTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BookingAvailabilityUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun conflictShowsReservationAndOffersARealNextTimeAction() {
        var selected = false
        compose.setContent {
            Fixoom_ServicesTheme {
                BookingAvailabilityPanel(
                    BookingAvailabilityState(
                        "2026-10-17",
                        listOf(BusyTime("2026-10-17T03:30:00Z", "2026-10-17T04:30:00Z")),
                    ),
                    true,
                    {},
                    { selected = true },
                )
            }
        }
        compose
            .onNodeWithText("This time overlaps a confirmed job. Choose another time.")
            .assertIsDisplayed()
        compose.onNodeWithText("Use next unreserved time").performClick()
        compose.runOnIdle { assertTrue(selected) }
    }

    @Test
    fun failedAvailabilityDoesNotPretendTheDayIsFree() {
        compose.setContent {
            Fixoom_ServicesTheme {
                BookingAvailabilityPanel(
                    BookingAvailabilityState("2026-10-17", error = "Schedule unavailable"),
                    false,
                    {},
                    {},
                )
            }
        }
        compose.onNodeWithText("Schedule unavailable").assertIsDisplayed()
        compose.onNodeWithText("Retry availability check").assertIsDisplayed()
        compose.onNodeWithText("No confirmed reservations on this date yet.").assertDoesNotExist()
    }
}
