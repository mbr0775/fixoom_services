package com.fixoom.customer.connected

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.fixoom.core.data.repository.AuthRepository
import com.fixoom.core.model.AppRole
import com.fixoom.core.model.AuthResult
import com.fixoom.customer.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class CustomerConnectedAppTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var authenticated = false

    @After
    fun signOut() {
        if (authenticated) runBlocking { AuthRepository().signOut() }
    }

    @Test
    fun homeOpensRealProviderAndBookingForm() {
        val args = InstrumentationRegistry.getArguments()
        val email = args.getString("fixoom.customer.email").orEmpty()
        val password = args.getString("fixoom.password").orEmpty()
        assumeTrue(
            email.startsWith("fixoom-e2e-") &&
                email.endsWith("@example.invalid") &&
                password.isNotBlank()
        )
        runBlocking {
            AuthRepository().signOut()
            check(
                AuthRepository().signIn(email, password, AppRole.CUSTOMER)
                    is AuthResult.Authenticated
            )
            authenticated = true
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(30_000) {
                runCatching {
                    compose
                        .onAllNodes(hasScrollToIndexAction())
                        .onFirst()
                        .performScrollToNode(hasText("Fixoom Integration Professional"))
                }
                compose
                    .onAllNodesWithText("Fixoom Integration Professional")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            InstrumentationRegistry.getInstrumentation()
                .uiAutomation
                .executeShellCommand(
                    "screencap -p /data/local/tmp/fixoom-connected-customer-home.png"
                )
                .use { descriptor ->
                    android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).readBytes()
                }
            compose.onNodeWithText("Fixoom Integration Professional").performClick()
            compose.waitUntil(20_000) {
                compose.onAllNodesWithText("Request a service").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Request a service").performScrollTo().performClick()
            compose.waitUntil(20_000) {
                compose.onAllNodesWithText("Service address").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Service address").assertExists()
        }
    }
}
