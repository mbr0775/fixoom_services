package com.fixoom.provider.ui

import android.os.ParcelFileDescriptor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.fixoom.core.model.AccountStatus
import com.fixoom.core.model.AppRole
import com.fixoom.core.model.Profile
import com.fixoom.provider.ui.components.ProviderDepthHero
import com.fixoom.provider.ui.screens.auth.ProviderWelcomeScreen
import com.fixoom.provider.ui.screens.dashboard.ProviderDashboardScreen
import com.fixoom.provider.ui.theme.Fixoom_ServicesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProviderRedesignTest {
    @get:Rule val compose = createComposeRule()
    private val fixture =
        Profile("ui-fixture", AppRole.PROVIDER, AccountStatus.ACTIVE, "Test Professional")

    @Test
    fun historyIsReachableFromBottomNavigationAndWorkActions() {
        var opened = 0
        compose.setContent {
            Fixoom_ServicesTheme {
                ProviderDashboardScreen(
                    fixture,
                    true,
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    onOpenHistory = { opened++ },
                )
            }
        }
        compose.onNodeWithTag("provider_nav_history").assertIsDisplayed().performClick()
        compose.onNodeWithText("Request history").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2, opened) }
    }

    @Test
    fun welcomeActionsRemainVisibleWithLargeTextAndScroll() {
        var created = false
        var signedIn = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                Fixoom_ServicesTheme() {
                    ProviderWelcomeScreen({ created = true }, { signedIn = true })
                }
            }
        }
        compose.onNodeWithText("Become a provider").assertIsDisplayed().performClick()
        compose.onNodeWithText("Sign in", substring = false).assertIsDisplayed().performClick()
        compose.onNodeWithText("Make room for your best work.").performScrollTo()
        compose.onNodeWithText("Become a provider").assertIsDisplayed()
        compose.runOnIdle { assertTrue(created && signedIn) }
        screenshot("welcome-large-text")
    }

    @Test
    fun unknownAvailabilityOffersSetupAndBottomNavigationWorksInLightTheme() {
        var profileOpened = 0
        var scheduleOpened = 0
        compose.setContent {
            Fixoom_ServicesTheme() {
                ProviderDashboardScreen(
                    fixture,
                    null,
                    {},
                    {},
                    {},
                    { scheduleOpened++ },
                    {},
                    { profileOpened++ },
                    {},
                    {},
                )
            }
        }
        screenshot("dashboard-setup-light-fixture")
        compose.onNodeWithTag("provider_nav_schedule").assertIsDisplayed().performClick()
        compose.onNodeWithTag("provider_nav_profile").assertIsDisplayed().performClick()
        compose.onNodeWithText("Set up").performScrollTo()
        compose.onNodeWithText("Availability not set up").assertIsDisplayed()
        compose.onNodeWithText("Set up").performClick()
        compose.runOnIdle {
            assertEquals(1, scheduleOpened)
            assertEquals(2, profileOpened)
        }
    }

    @Test
    fun availabilityAndWorkActionsKeepTheirCallbacksInLightTheme() {
        var jobsOpened = false
        var earningsOpened = false
        compose.setContent {
            var available by remember { mutableStateOf(false) }
            Fixoom_ServicesTheme() {
                ProviderDashboardScreen(
                    fixture,
                    available,
                    { available = it },
                    { jobsOpened = true },
                    {},
                    {},
                    { earningsOpened = true },
                    {},
                    {},
                    {},
                )
            }
        }
        screenshot("dashboard-light-fixture")
        compose.onNodeWithText("View job requests").performScrollTo().performClick()
        compose.onNode(isToggleable()).performScrollTo().assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("Ready for new requests").assertIsDisplayed()
        compose.onNodeWithText("Earnings").performScrollTo()
        compose.onNodeWithText("Earnings").performClick()
        compose.runOnIdle { assertTrue(jobsOpened && earningsOpened) }
    }

    @Test
    fun reducedMotionHeroRendersItsContentWithoutAnimationClockAdvancement() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            Fixoom_ServicesTheme() {
                ProviderDepthHero(
                    "Your craft.",
                    "FIXOOM",
                    "Your workspace",
                    { 1000f },
                    motionEnabled = false,
                )
            }
        }
        compose.onNodeWithText("Your craft.").assertIsDisplayed()
        compose.onNodeWithText("Your workspace").assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        // Shell-owned QA images survive Gradle uninstalling the instrumented app.
        // Names are fixed test constants; no user input enters this command.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        ParcelFileDescriptor.AutoCloseInputStream(
                automation.executeShellCommand(
                    "screencap -p /data/local/tmp/fixoom-provider-$name.png"
                )
            )
            .use { it.readBytes() }
    }
}
