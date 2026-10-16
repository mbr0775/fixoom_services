package com.fixoom.customer.connected

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.fixoom.core.model.ServiceCategory
import com.fixoom.core.model.ServiceProfessional
import com.fixoom.customer.ui.screens.ExploreScreen
import com.fixoom.customer.ui.theme.Fixoom_ServicesTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Isolated screen inputs; these fixtures never enter the running app or Supabase. */
class RealProviderDiscoveryTest {
    @get:Rule val compose = createComposeRule()
    private val categories = listOf(ServiceCategory("plumbing", "Plumbing"))

    @Test
    fun exploreTracksBackendDirectoryAndOpensTheReturnedProviderId() {
        val providers = mutableStateOf(emptyList<ServiceProfessional>())
        var selected: String? = null
        compose.setContent {
            Fixoom_ServicesTheme {
                ExploreScreen(
                    onServiceClick = {},
                    categories = categories,
                    professionals = providers.value,
                    onProviderClick = { selected = it },
                )
            }
        }
        scrollTo("No available professionals")
        compose.onNodeWithText("No available professionals").assertIsDisplayed()
        compose.onNodeWithText("Apex Plumbing").assertDoesNotExist()
        compose.onNodeWithText("Recommended near you").assertDoesNotExist()
        compose.runOnIdle {
            providers.value =
                listOf(
                    ServiceProfessional(
                        "returned-provider-id",
                        "Directory test professional",
                        "carpentry",
                        "Registered service area",
                        "",
                        true,
                        approvedCategories = listOf("plumbing", "carpentry"),
                    )
                )
        }
        scrollTo("Directory test professional")
        compose.onNodeWithText("Registered service area").assertIsDisplayed()
        compose.onNodeWithText("Directory test professional").performClick()
        compose.runOnIdle { assertEquals("returned-provider-id", selected) }
        compose.onNodeWithText("No available professionals").assertDoesNotExist()
        compose.runOnIdle { providers.value = emptyList() }
        scrollTo("No available professionals")
        compose.onNodeWithText("Directory test professional").assertDoesNotExist()
    }

    @Test
    fun failedDirectoryShowsRecoveryWithoutClaimingEmptyResults() {
        compose.setContent {
            Fixoom_ServicesTheme {
                ExploreScreen(
                    onServiceClick = {},
                    categories = categories,
                    dataError = "Network unavailable",
                )
            }
        }
        scrollTo("Network unavailable")
        compose.onNodeWithText("Network unavailable").assertIsDisplayed()
        compose.onNodeWithText("Try again").assertExists()
        scrollTo("Available professionals")
        compose.onNodeWithText("No available professionals").assertDoesNotExist()
        compose.onNodeWithText("Apex Plumbing").assertDoesNotExist()
    }

    private fun scrollTo(text: String) {
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasText(text))
    }
}
