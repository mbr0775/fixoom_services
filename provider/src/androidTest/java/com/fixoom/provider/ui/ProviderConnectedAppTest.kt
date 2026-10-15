package com.fixoom.provider.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.fixoom.core.data.repository.AuthRepository
import com.fixoom.core.model.AppRole
import com.fixoom.core.model.AuthResult
import com.fixoom.provider.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Uses only explicit temporary fixtures and launches the actual provider application. */
class ProviderConnectedAppTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var authenticated = false

    @After
    fun signOut() {
        if (authenticated) runBlocking { AuthRepository().signOut() }
    }

    @Test
    fun dashboardOpensConnectedReceipts() {
        val args = InstrumentationRegistry.getArguments()
        val email = args.getString("fixoom.provider.email").orEmpty()
        val password = args.getString("fixoom.password").orEmpty()
        assumeTrue(
            email.startsWith("fixoom-e2e-") &&
                email.endsWith("@example.invalid") &&
                password.isNotBlank()
        )
        runBlocking {
            AuthRepository().signOut()
            check(
                AuthRepository().signIn(email, password, AppRole.PROVIDER)
                    is AuthResult.Authenticated
            )
            authenticated = true
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(30_000) {
                compose.onAllNodesWithText("Earnings").fetchSemanticsNodes().isNotEmpty()
            }
            InstrumentationRegistry.getInstrumentation()
                .uiAutomation
                .executeShellCommand(
                    "screencap -p /data/local/tmp/fixoom-connected-provider-home.png"
                )
                .use { descriptor ->
                    android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).readBytes()
                }
            compose.onNodeWithText("Earnings").performScrollTo().performClick()
            compose.waitUntil(30_000) {
                compose
                    .onAllNodesWithText("Recorded as received")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            compose.onNodeWithText("LKR 2500.50", substring = true).assertExists()
        }
    }
}
