package com.fixoom.customer.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.fixoom.customer.ui.theme.Fixoom_ServicesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileContentTest {
    @get:Rule val compose = createComposeRule()

    private fun show(
        account: ProfileAccountState = ProfileAccountState.Guest,
        dark: Boolean = false,
        onBookings: () -> Unit = {},
        onExplore: () -> Unit = {},
        onLocation: () -> Unit = {},
        onDetails: () -> Unit = {},
        onRetry: () -> Unit = {}
    ) {
        compose.setContent {
            Fixoom_ServicesTheme(darkTheme = dark) {
                ProfileContent(
                    account, "Kandy, Sri Lanka", dark, "1.0",
                    onDarkModeChange = {}, onBookings = onBookings, onExplore = onExplore,
                    onLocation = onLocation, onNotifications = {}, onPermissions = {},
                    onAccountDetails = onDetails, onHelp = {}, onAbout = {}, onRetry = onRetry
                )
            }
        }
    }

    @Test fun guestStateHasWorkingExploreActionAndNoFakeAccountDetails() {
        var opened = false
        show(onExplore = { opened = true })
        compose.onNodeWithText("Browsing as guest").assertIsDisplayed()
        compose.onNodeWithText("Account details").assertDoesNotExist()
        compose.onNodeWithTag("profile_identity_action").performClick()
        compose.runOnIdle { assertTrue(opened) }
    }

    @Test fun bookingQuickActionInvokesNavigationCallback() {
        var opened = false
        show(onBookings = { opened = true })
        compose.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("profile_bookings"))
        compose.onNodeWithTag("profile_bookings").performClick()
        compose.runOnIdle { assertTrue(opened) }
    }

    @Test fun locationShowsSavedAreaAndOpensExistingPicker() {
        var opened = false
        show(onLocation = { opened = true })
        compose.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("profile_location"))
        compose.onNodeWithText("Kandy, Sri Lanka").assertIsDisplayed()
        compose.onNodeWithTag("profile_location").performClick()
        compose.runOnIdle { assertTrue(opened) }
    }

    @Test fun signedInIdentityUsesAccountDataAndOpensDetailsInDarkMode() {
        var opened = false
        show(account = ProfileAccountState.SignedIn("Test Customer", null), dark = true, onDetails = { opened = true })
        compose.onNodeWithText("Test Customer").assertIsDisplayed()
        compose.onNodeWithText("Customer account").assertIsDisplayed()
        compose.onNodeWithTag("profile_identity_action").performClick()
        compose.runOnIdle { assertTrue(opened) }
    }

    @Test fun unavailableProfileOffersRetryWithoutInventingAnIdentity() {
        var retries = 0
        show(account = ProfileAccountState.Unavailable, onRetry = { retries++ })
        compose.onNodeWithText("Account details unavailable").assertIsDisplayed()
        compose.onNodeWithTag("profile_identity_action").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun appearanceToggleUsesCallbackAndReflectsRestoredPreference() {
        val restoration = StateRestorationTester(compose)
        var callbackValue: Boolean? = null
        restoration.setContent {
            var dark by rememberSaveable { mutableStateOf(false) }
            Fixoom_ServicesTheme(darkTheme = dark) {
                ProfileContent(
                    ProfileAccountState.Guest, null, dark, "1.0",
                    onDarkModeChange = { dark = it; callbackValue = it },
                    onBookings = {}, onExplore = {}, onLocation = {}, onNotifications = {}, onPermissions = {},
                    onAccountDetails = {}, onHelp = {}, onAbout = {}, onRetry = {}
                )
            }
        }
        compose.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("profile_dark_mode"))
        compose.onNodeWithTag("profile_dark_mode").assertIsOff().performClick()
        compose.onNodeWithTag("profile_dark_mode").assertIsOn()
        compose.runOnIdle { assertEquals(true, callbackValue) }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("profile_list").performScrollToNode(hasTestTag("profile_dark_mode"))
        compose.onNodeWithTag("profile_dark_mode").assertIsOn()
    }

    @Test fun helpAnswersExpandAndCollapse() {
        compose.setContent {
            Fixoom_ServicesTheme {
                ProfileDetailSheet(ProfilePanel.HELP, ProfileAccountState.Guest, "1.0", {}, {})
            }
        }
        val answer = "Yes. Your light or dark appearance is saved on this device and used when you reopen Fixoom."
        compose.onNodeWithText(answer).assertDoesNotExist()
        compose.onNodeWithText("Will my appearance choice be saved?").performClick()
        compose.onNodeWithText(answer).assertIsDisplayed()
        compose.onNodeWithText("Will my appearance choice be saved?").performClick()
        compose.onNodeWithText(answer).assertDoesNotExist()
    }
}
