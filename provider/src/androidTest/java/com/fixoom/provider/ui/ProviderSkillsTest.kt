package com.fixoom.provider.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.fixoom.core.data.repository.MarketplaceSnapshot
import com.fixoom.core.data.repository.MarketplaceState
import com.fixoom.core.model.*
import com.fixoom.marketplace.ui.ProviderSkillsScreen
import com.fixoom.provider.ui.auth.ProviderAuthUiState
import com.fixoom.provider.ui.screens.auth.ProviderRegisterScreen
import com.fixoom.provider.ui.theme.Fixoom_ServicesTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Isolated UI callbacks only. No account, skill or review is written to Supabase. */
class ProviderSkillsTest {
    @get:Rule val compose = createComposeRule()
    private val fixture =
        MarketplaceState(
            data =
                MarketplaceSnapshot(
                    ownService =
                        ServiceProfessional(
                            "fixture",
                            "Test Professional",
                            "plumbing",
                            "Test area",
                            approvedCategories = listOf("plumbing"),
                            skills =
                                listOf(
                                    ProviderSkill(
                                        "fixture",
                                        "plumbing",
                                        "Approved service description",
                                        "approved",
                                    ),
                                    ProviderSkill("fixture", "carpentry", "", "pending"),
                                ),
                        ),
                    categories =
                        listOf(
                            ServiceCategory("plumbing", "Plumbing"),
                            ServiceCategory("carpentry", "Carpentry"),
                        ),
                ),
            loading = false,
        )

    @Test
    fun registrationSendsMultipleSelectedSkillsForReview() {
        var selected: List<String>? = null
        compose.setContent {
            Fixoom_ServicesTheme {
                ProviderRegisterScreen(
                    ProviderAuthUiState(isLoading = false),
                    { _, _, _, _, skills -> selected = skills },
                    {},
                    {},
                    {},
                    {},
                    categories = fixture.data.categories,
                )
            }
        }
        listOf(
                "Full name" to "Test Professional",
                "Email address" to "fixture@example.invalid",
                "Phone number" to "0771234567",
                "Password" to "fixture-password",
                "Confirm password" to "fixture-password",
            )
            .forEach { (label, value) ->
                compose
                    .onNode(hasSetTextAction() and hasText(label))
                    .performScrollTo()
                    .performTextInput(value)
            }
        compose.onNodeWithText("Plumbing").performScrollTo().performClick()
        compose.onNodeWithText("Carpentry").performScrollTo().performClick()
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("Create account", substring = false).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("plumbing", "carpentry"), selected) }
    }

    @Test
    fun pendingSkillNeedsDescriptionAndSuccessfulSubmissionClearsDraft() {
        var submitted: Pair<String, String>? = null
        compose.setContent {
            Fixoom_ServicesTheme {
                ProviderSkillsScreen(
                    fixture,
                    {},
                    {},
                    { slug, description, done ->
                        submitted = slug to description
                        done()
                    },
                    { fail("No removal requested") },
                )
            }
        }
        compose
            .onNodeWithTag("request_skill_carpentry")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        compose.onNodeWithText("Submit for approval").performScrollTo().performClick()
        compose.runOnIdle { assertNull(submitted) }
        compose
            .onNodeWithText(
                "Choose a category and describe your service in at least 10 characters."
            )
            .assertExists()
        compose
            .onNode(hasSetTextAction())
            .performScrollTo()
            .performTextInput("I offer furniture repairs and fitted shelves.")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithText("Submit for approval").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("carpentry" to "I offer furniture repairs and fitted shelves.", submitted)
        }
        compose.onNode(hasSetTextAction()).assertTextContains("Experience and service offered")
        compose.onNodeWithText("I offer furniture repairs and fitted shelves.").assertDoesNotExist()
    }

    @Test
    fun removingApprovedServiceRequiresConfirmationAndPreservesOtherSkills() {
        var removed: String? = null
        compose.setContent {
            Fixoom_ServicesTheme {
                ProviderSkillsScreen(
                    fixture,
                    {},
                    {},
                    { _, _, _ -> fail("Viewing must not submit a request") },
                    { removed = it },
                )
            }
        }
        compose.onNodeWithText("Remove service").performScrollTo().performClick()
        compose.onNodeWithText("Cancel", substring = false).performClick()
        compose.runOnIdle { assertNull(removed) }
        compose.onNodeWithText("Remove service").performClick()
        compose.onNodeWithText("Remove", substring = false).performClick()
        compose.runOnIdle { assertEquals("plumbing", removed) }
    }
}
