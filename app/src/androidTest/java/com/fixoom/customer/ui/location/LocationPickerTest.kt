package com.fixoom.customer.ui.location

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.fixoom.customer.R
import com.fixoom.customer.ui.theme.Fixoom_ServicesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LocationPickerTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun manualAreaSearchAndSelectionWorkWithoutLocationPermission() {
        var selected: String? = null
        compose.setContent {
            Fixoom_ServicesTheme(darkTheme = false) {
                LocationPickerSheet(
                    state = HomeLocationUiState(ready = true),
                    onDismiss = {},
                    onManualSelection = { selected = it },
                    onUseGps = {},
                    onRecovery = {}
                )
            }
        }
        compose.onNodeWithTag("location_search").performTextInput("Kandy")
        compose.onNodeWithTag("location_picker").performScrollToNode(
            hasText("Kandy, Sri Lanka")
        )
        compose.onNodeWithText("Kandy, Sri Lanka").performClick()
        compose.runOnIdle { assertEquals("Kandy, Sri Lanka", selected) }
    }

    @Test
    fun gpsActionWorksInDarkTheme() {
        var requested = false
        compose.setContent {
            Fixoom_ServicesTheme(darkTheme = true) {
                LocationPickerSheet(
                    state = HomeLocationUiState(ready = true),
                    onDismiss = {},
                    onManualSelection = {},
                    onUseGps = { requested = true },
                    onRecovery = {}
                )
            }
        }
        compose.onNodeWithTag("use_current_location").performClick()
        compose.runOnIdle { assertTrue(requested) }
    }

    @Test
    fun permissionDenialOffersSettingsWithoutBlockingManualSelection() {
        var settingsRequested = false
        compose.setContent {
            Fixoom_ServicesTheme(darkTheme = false) {
                LocationPickerSheet(
                    state = HomeLocationUiState(
                        ready = true,
                        message = R.string.location_permission_denied,
                        recovery = LocationRecovery.APP_SETTINGS
                    ),
                    onDismiss = {},
                    onManualSelection = {},
                    onUseGps = {},
                    onRecovery = { settingsRequested = true }
                )
            }
        }
        compose.onNodeWithText("Open app permissions").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(settingsRequested) }
    }

    @Test
    fun locatingStatePreventsDuplicateGpsRequests() {
        compose.setContent {
            Fixoom_ServicesTheme(darkTheme = false) {
                LocationPickerSheet(
                    state = HomeLocationUiState(ready = true, locating = true),
                    onDismiss = {},
                    onManualSelection = {},
                    onUseGps = {},
                    onRecovery = {}
                )
            }
        }
        compose.onNodeWithTag("use_current_location").assertIsNotEnabled()
        compose.onNodeWithText("Finding your location…").assertIsDisplayed()
    }
}
