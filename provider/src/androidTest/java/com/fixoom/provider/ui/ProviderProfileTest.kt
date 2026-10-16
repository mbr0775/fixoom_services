package com.fixoom.provider.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.fixoom.core.data.repository.MarketplaceSnapshot
import com.fixoom.core.data.repository.MarketplaceState
import com.fixoom.core.model.*
import com.fixoom.marketplace.ui.ProviderProfileScreens
import com.fixoom.provider.ui.theme.Fixoom_ServicesTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ProviderProfileTest {
    @get:Rule val compose = createComposeRule()
    private val fixture =
        MarketplaceState(
            data =
                MarketplaceSnapshot(
                    profile =
                        Profile(
                            "profile-fixture",
                            AppRole.PROVIDER,
                            AccountStatus.ACTIVE,
                            "Test Professional",
                            phone = "+94110000000",
                            email = "provider@example.invalid",
                        ),
                    ownService =
                        ServiceProfessional(
                            "profile-fixture",
                            "Test Professional",
                            "plumbing",
                            "Test service area",
                            "Test service description",
                            true,
                        ),
                    categories = listOf(ServiceCategory("plumbing", "Plumbing")),
                ),
            loading = false,
        )

    @Test
    fun profileIsReadOnlyUntilEditAndSuccessfulSaveReturnsToUpdatedView() {
        var saves = 0
        compose.setContent {
            var state by remember { mutableStateOf(fixture) }
            ProfileTestSurface {
                ProviderProfileScreens(
                    state,
                    false,
                    {},
                    {},
                    { name, category, area, bio, available, done ->
                        saves++
                        state =
                            state.copy(
                                data =
                                    state.data.copy(
                                        ownService =
                                            state.data.ownService!!.copy(
                                                displayName = name,
                                                categorySlug = category,
                                                serviceArea = area,
                                                bio = bio,
                                                isAvailable = available,
                                            )
                                    )
                            )
                        done()
                    },
                    { _, _ -> fail("Viewing a profile cannot upload a photo") },
                    {},
                    {},
                )
            }
        }
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onAllNodesWithText("Test Professional").onFirst().assertIsDisplayed()
        compose.onNodeWithContentDescription("Edit profile").assertIsDisplayed()
        compose.onNodeWithText("3/4 added").assertIsDisplayed()
        screenshot("overview")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Personal details"))
        compose.onNodeWithText("Personal details").performClick()
        compose.onNodeWithText("provider@example.invalid").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("+94110000000").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Edit profile").performClick()
        compose.onNodeWithText("Edit profile").assertIsDisplayed()
        compose.onNodeWithContentDescription("Edit profile photo").assertExists()
        compose
            .onNode(hasSetTextAction() and hasText("Test Professional"))
            .performScrollTo()
            .performTextReplacement("Updated Professional")
        screenshot("edit")
        compose.onNodeWithText("Save professional profile").performScrollTo().performClick()
        compose.onNodeWithText("My profile").assertIsDisplayed()
        compose.onNodeWithText("Updated Professional").assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.runOnIdle { assertEquals(1, saves) }
    }

    @Test
    fun backFromEditingDiscardsFormWithoutSavingOrLeavingProfile() {
        var saves = 0
        var exits = 0
        compose.setContent {
            ProfileTestSurface {
                ProviderProfileScreens(
                    fixture,
                    false,
                    {},
                    { exits++ },
                    { _, _, _, _, _, _ -> saves++ },
                    { _, _ -> saves++ },
                    {},
                    {},
                )
            }
        }
        compose.onNodeWithContentDescription("Edit profile").performClick()
        compose
            .onNode(hasSetTextAction() and hasText("Test Professional"))
            .performScrollTo()
            .performTextReplacement("Unsaved change")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onAllNodesWithText("Test Professional").onFirst().assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.runOnIdle {
            assertEquals(0, saves)
            assertEquals(0, exits)
        }
    }

    @Test
    fun invalidFormAndFailedSaveStayInEditor() {
        var saves = 0
        compose.setContent {
            var state by remember { mutableStateOf(fixture) }
            ProfileTestSurface {
                ProviderProfileScreens(
                    state,
                    false,
                    {},
                    {},
                    { _, _, _, _, _, _ ->
                        saves++
                        state = state.copy(error = "Test save failed")
                    },
                    { _, _ -> },
                    {},
                    {},
                )
            }
        }
        compose.onNodeWithContentDescription("Edit profile").performClick()
        compose
            .onNode(hasSetTextAction() and hasText("Test Professional"))
            .performScrollTo()
            .performTextClearance()
        compose.onNodeWithText("Save professional profile").performScrollTo().performClick()
        compose
            .onNodeWithText("Add your professional name, category and service area.")
            .performScrollTo()
            .assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, saves) }
        compose
            .onNodeWithText("Professional or business name")
            .performScrollTo()
            .performTextReplacement("Valid name")
        compose.onNodeWithText("Save professional profile").performScrollTo().performClick()
        compose.onNodeWithText("Edit profile").assertIsDisplayed()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Test save failed"))
        compose.onNodeWithText("Test save failed").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, saves) }
    }

    @Test
    fun pendingProviderCanCheckApprovalAndOpenEditorWithLargeText() {
        var checks = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                ProfileTestSurface {
                    ProviderProfileScreens(
                        fixture.copy(
                            data =
                                fixture.data.copy(
                                    profile =
                                        fixture.data.profile!!.copy(
                                            accountStatus = AccountStatus.PENDING
                                        )
                                )
                        ),
                        true,
                        {},
                        {},
                        { _, _, _, _, _, _ -> },
                        { _, _ -> },
                        { checks++ },
                        {},
                    )
                }
            }
        }
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose
            .onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Check approval status"))
        compose.onNodeWithText("Check approval status").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, checks) }
        compose.onNodeWithContentDescription("Edit profile").performClick()
        compose.onNodeWithText("Save professional profile").performScrollTo().assertIsDisplayed()
        compose.onAllNodes(isToggleable()).assertCountEquals(0)
        compose.onNodeWithContentDescription("Back").performClick()
        compose
            .onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Check approval status"))
        compose.onNodeWithText("Check approval status").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun missingDetailsShowHonestCompletionAndSectionsWorkWithoutMotion() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            ProfileTestSurface {
                ProviderProfileScreens(
                    fixture.copy(
                        data =
                            fixture.data.copy(
                                profile = fixture.data.profile!!.copy(phone = null),
                                ownService = fixture.data.ownService!!.copy(bio = ""),
                            )
                    ),
                    false,
                    {},
                    {},
                    { _, _, _, _, _, _ -> },
                    { _, _ -> },
                    {},
                    {},
                    motionEnabled = false,
                )
            }
        }
        compose.onNodeWithText("1/4 added").assertIsDisplayed()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Personal details"))
        compose.onNodeWithText("Personal details").performClick()
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithText("No phone supplied").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Personal details").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithText("No phone supplied").assertDoesNotExist()
        compose.onNodeWithContentDescription("Edit profile").performClick()
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithText("Edit profile").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithText("My profile").assertIsDisplayed()
    }

    @Composable
    private fun ProfileTestSurface(content: @Composable () -> Unit) {
        Fixoom_ServicesTheme {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) { content() }
        }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
                automation.executeShellCommand(
                    "screencap -p /data/local/tmp/fixoom-provider-profile-$name-fixture.png"
                )
            )
            .use { it.readBytes() }
    }
}
