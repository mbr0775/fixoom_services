package com.fixoom.provider.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.fixoom.provider.ui.components.ProviderWorkHub
import com.fixoom.provider.ui.theme.Fixoom_ServicesTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Presentation fixtures only; no profile, booking or backend writes. */
class ProviderWorkHubTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun allFiveShortcutsKeepTheirOwnNavigationActions() {
        val opened = mutableSetOf<String>()
        compose.setContent {
            Fixoom_ServicesTheme {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
                ) {
                    ProviderWorkHub(
                        { opened += "requests" },
                        { opened += "active" },
                        { opened += "schedule" },
                        { opened += "earnings" },
                        { opened += "history" },
                        motionEnabled = false,
                    )
                }
            }
        }
        for (action in listOf("requests", "active", "schedule", "history", "earnings")) {
            compose
                .onNodeWithTag("work_card_$action")
                .performScrollTo()
                .assertIsDisplayed()
                .performClick()
        }
        compose.runOnIdle {
            assertEquals(setOf("requests", "active", "schedule", "history", "earnings"), opened)
        }
    }

    @Test
    fun largeTextStacksCardsAtFullWidthAndKeepsActionsVisible() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                Fixoom_ServicesTheme {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
                    ) {
                        ProviderWorkHub({}, {}, {}, {}, {}, motionEnabled = false)
                    }
                }
            }
        }
        compose.onNodeWithTag("work_card_active").performScrollTo().assertIsDisplayed()
        val width =
            compose.onNodeWithTag("provider_work_hub").fetchSemanticsNode().boundsInRoot.width
        assertEquals(
            width,
            compose.onNodeWithTag("work_card_active").fetchSemanticsNode().boundsInRoot.width,
            1f,
        )
        compose.onNodeWithTag("work_card_history").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("work_card_earnings").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun reducedMotionRendersWithoutAdvancingTheAnimationClock() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            Fixoom_ServicesTheme {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
                ) {
                    ProviderWorkHub({}, {}, {}, {}, {}, motionEnabled = false)
                }
            }
        }
        compose.onNodeWithText("Job requests").assertIsDisplayed()
        compose.onNodeWithTag("work_card_schedule").assertExists()
        compose.onNodeWithTag("work_card_earnings").assertExists()
    }
}
