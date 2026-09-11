package com.fixoom.customer.ui.discovery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.fixoom.customer.R
import com.fixoom.customer.ui.theme.FixoomMint
import com.fixoom.customer.ui.theme.Fixoom_ServicesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ServiceDiscoveryTest {
    @get:Rule val compose = createComposeRule()

    private val services = listOf(
        ServiceCategoryUi("Plumbing", "Leaks and pipe repairs", R.drawable.ic_service_plumbing, FixoomMint),
        ServiceCategoryUi("Electrical", "Wiring and electrical faults", R.drawable.ic_service_electrical, FixoomMint),
        ServiceCategoryUi("Carpentry", "Furniture and woodwork", R.drawable.ic_service_carpentry, FixoomMint)
    )

    private fun show(dark: Boolean = false, onClick: (String) -> Unit = {}) {
        compose.setContent {
            Fixoom_ServicesTheme(darkTheme = dark) {
                HomeServiceDiscovery(services, onClick)
            }
        }
    }

    @Test fun searchUpdatesResultsButDoesNotNavigateUntilServiceIsTapped() {
        var clicked: String? = null
        show(onClick = { clicked = it })
        compose.onNodeWithTag("service_search").performTextInput("PIPE leaks")
        compose.onNodeWithTag("service_search").performImeAction()
        compose.onNodeWithText("1 service").assertIsDisplayed()
        compose.onNodeWithTag("service_result_Electrical").assertDoesNotExist()
        compose.runOnIdle { assertNull(clicked) }
        compose.onNodeWithTag("service_result_Plumbing").performClick()
        compose.runOnIdle { assertEquals("Plumbing", clicked) }
    }

    @Test fun multipleCategoriesApplyTogetherAndChipsCanBeRemoved() {
        show()
        compose.onNodeWithTag("service_filters").performClick()
        compose.onNodeWithTag("filter_category_Plumbing").performClick()
        compose.onNodeWithTag("filter_category_Electrical").performClick()
        compose.onNodeWithText("Show 2 services").assertIsDisplayed()
        compose.onNodeWithTag("apply_service_filters").performClick()
        compose.onNodeWithTag("service_result_Carpentry").assertDoesNotExist()
        compose.onNodeWithContentDescription("Open service filters, 2 active filters").assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove Plumbing filter").performClick()
        compose.onNodeWithTag("service_result_Plumbing").assertDoesNotExist()
        compose.onNodeWithText("Clear filters").performClick()
        compose.onNodeWithText("3 services").assertIsDisplayed()
    }

    @Test fun closeDiscardsUnappliedChanges() {
        show()
        compose.onNodeWithTag("service_filters").performClick()
        compose.onNodeWithTag("filter_category_Plumbing").performClick()
        compose.onNodeWithContentDescription("Close service filters").performClick()
        compose.onNodeWithText("3 services").assertIsDisplayed()
        compose.onNodeWithTag("service_filters").performClick()
        compose.onNodeWithTag("filter_category_Plumbing").assertIsOff()
    }

    @Test fun zeroResultsCanResetBothSearchAndCategoriesInDarkTheme() {
        show(dark = true)
        compose.onNodeWithTag("service_search").performTextInput("electrical")
        compose.onNodeWithTag("service_filters").performClick()
        compose.onNodeWithTag("filter_category_Plumbing").performClick()
        compose.onNodeWithText("Show 0 services").assertIsDisplayed()
        compose.onNodeWithTag("apply_service_filters").performClick()
        compose.onNodeWithText("No matching services").assertIsDisplayed()
        compose.onNodeWithText("Reset search and filters").performClick()
        compose.onNodeWithText("3 services").assertIsDisplayed()
        compose.onNodeWithContentDescription("Open service filters").assertIsDisplayed()
    }

    @Test fun appliedFiltersAndLayoutSurviveSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            Fixoom_ServicesTheme {
                HomeServiceDiscovery(services, {})
            }
        }
        compose.onNodeWithContentDescription("List view").performClick()
        compose.onNodeWithTag("service_filters").performClick()
        compose.onNodeWithTag("filter_category_Plumbing").performClick()
        compose.onNodeWithTag("service_filter_options").performScrollToNode(hasText("Name: Z–A"))
        compose.onNodeWithText("Name: Z–A").performClick()
        compose.onNodeWithTag("apply_service_filters").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("1 service").assertIsDisplayed()
        compose.onNodeWithContentDescription("Open service filters, 2 active filters").assertIsDisplayed()
        compose.onNodeWithContentDescription("List view").assertIsSelected()
    }

    @Test fun resetInsideSheetDoesNotChangeAppliedFiltersUntilApplied() {
        show()
        compose.onNodeWithTag("service_filters").performClick()
        compose.onNodeWithTag("filter_category_Plumbing").performClick()
        compose.onNodeWithTag("apply_service_filters").performClick()
        compose.onNodeWithTag("service_filters").performClick()
        compose.onNodeWithText("Reset").performClick()
        compose.onNodeWithText("Show 3 services").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close service filters").performClick()
        compose.onNodeWithText("1 service").assertIsDisplayed()
    }
}
