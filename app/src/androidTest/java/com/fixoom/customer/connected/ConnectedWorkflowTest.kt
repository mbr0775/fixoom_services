package com.fixoom.customer.connected

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.platform.app.InstrumentationRegistry
import com.fixoom.core.data.repository.*
import com.fixoom.core.model.*
import com.fixoom.customer.ui.theme.Fixoom_ServicesTheme as MaterialTheme
import com.fixoom.marketplace.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in live integration checks; accepts only temporary example.invalid fixture accounts. */
class ConnectedWorkflowTest {
    @get:Rule val compose = createComposeRule()
    private val args
        get() = InstrumentationRegistry.getArguments()

    private lateinit var market: MarketplaceViewModel
    private var authenticated = false
    private val repository = MarketplaceRepository()

    private fun signIn(role: String) {
        val email = args.getString("fixoom.${role}.email").orEmpty()
        val password = args.getString("fixoom.password").orEmpty()
        assumeTrue(
            "Temporary fixture credentials required",
            email.startsWith("fixoom-e2e-") &&
                email.endsWith("@example.invalid") &&
                password.isNotBlank(),
        )
        runBlocking {
            AuthRepository().signOut()
            val result =
                AuthRepository()
                    .signIn(
                        email,
                        password,
                        if (role == "provider") AppRole.PROVIDER else AppRole.CUSTOMER,
                    )
            check(
                result is AuthResult.Authenticated || result == AuthResult.ProviderApprovalPending
            ) {
                "Fixture sign-in failed"
            }
            authenticated = true
        }
    }

    private fun waitFor(text: String) {
        compose.waitUntil(30_000) {
            if (compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isEmpty())
                runCatching {
                    compose
                        .onNode(hasScrollToIndexAction())
                        .performScrollToNode(hasText(text, substring = true))
                }
            compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun scroll(text: String) {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
        compose.onNodeWithText(text).performScrollTo()
    }

    private fun click(text: String) {
        scroll(text)
        compose.waitUntil(30_000) {
            compose.onAllNodes(hasText(text) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(text).performClick()
    }

    private fun shot(name: String) {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/fixoom-connected-$name.png")
            .use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
    }

    @After
    fun signOut() {
        if (authenticated) runBlocking { AuthRepository().signOut() }
    }

    @Test
    fun providerProfileSetup() {
        signIn("provider")
        compose.setContent {
            MaterialTheme {
                market = viewModel()
                ObserveMarketplace(market)
                ConnectedProviderProfile(market, true, {}, {}, {})
            }
        }
        compose.waitUntil(30_000) {
            ::market.isInitialized && market.state.value.data.ownService != null
        }
        compose.onNodeWithText("Professional or business name").performTextClearance()
        compose
            .onNodeWithText("Professional or business name")
            .performTextInput("Fixoom Integration Professional")
        click("Plumbing")
        compose
            .onNodeWithText("Service area, e.g. Colombo")
            .performScrollTo()
            .performTextInput("Colombo")
        compose
            .onNodeWithText("Experience and services offered")
            .performScrollTo()
            .performTextInput("Temporary professional for connected workflow testing.")
        click("Save professional profile")
        compose.waitUntil(30_000) {
            market.state.value.data.ownService?.serviceArea == "Colombo" &&
                !market.state.value.saving
        }
        assertFalse(market.state.value.data.ownService!!.isAvailable)
        shot("provider-profile")
    }

    @Test
    fun customerRequestsService() {
        signIn("customer")
        compose.setContent {
            MaterialTheme {
                market = viewModel()
                ObserveMarketplace(market)
                var step by remember { mutableStateOf("directory") }
                var provider by remember { mutableStateOf("") }
                var booking by remember { mutableStateOf("") }
                when (step) {
                    "directory" ->
                        ConnectedProviderList(
                            "Plumbing",
                            market,
                            {
                                provider = it
                                step = "professional"
                            },
                            {},
                        )
                    "professional" ->
                        ConnectedProviderDetails(provider, market, {}, { step = "request" })
                    "request" ->
                        ConnectedBookingForm(
                            provider,
                            market,
                            {},
                            {},
                            {
                                booking = it
                                step = "confirmed"
                            },
                        )
                    else ->
                        ConnectedBookingDetails(booking, market, onBack = {}, confirmation = true)
                }
            }
        }
        waitFor("Fixoom Integration Professional")
        click("Fixoom Integration Professional")
        click("Request a service")
        compose
            .onNodeWithText("Service address")
            .performScrollTo()
            .performTextInput("Integration test address, Colombo")
        compose
            .onNodeWithText("What needs fixing?")
            .performScrollTo()
            .performTextInput("Fixoom connected workflow test: repair the kitchen tap.")
        click("Send booking request")
        waitFor("Your request is saved")
        scroll("Write a message")
        compose.onNodeWithText("Write a message").performTextInput("Customer integration message")
        click("Send message")
        compose.waitUntil(30_000) { !market.state.value.saving }
        val bookings = runBlocking { repository.snapshot().bookings }
        assertEquals(1, bookings.size)
        assertEquals("requested", bookings.single().status)
        assertEquals(
            "Customer integration message",
            runBlocking { repository.messages(bookings.single().id) }.single().body,
        )
        shot("customer-request")
    }

    @Test
    fun providerBecomesAvailable() {
        signIn("provider")
        compose.setContent {
            MaterialTheme {
                market = viewModel()
                ObserveMarketplace(market)
                ConnectedProviderProfile(market, false, {}, {}, {})
            }
        }
        compose.waitUntil(30_000) {
            ::market.isInitialized && market.state.value.data.ownService != null
        }
        compose.onNode(isToggleable()).performScrollTo().performClick()
        click("Save professional profile")
        compose.waitUntil(30_000) {
            market.state.value.data.ownService?.isAvailable == true && !market.state.value.saving
        }
        shot("provider-available")
    }

    @Test
    fun providerCompletesService() {
        signIn("provider")
        val booking = runBlocking { repository.snapshot().bookings }.single()
        compose.setContent {
            MaterialTheme {
                market = viewModel()
                ObserveMarketplace(market)
                ConnectedBookingDetails(booking.id, market, provider = true, onBack = {})
            }
        }
        if (booking.status == "requested") {
            waitFor("Accept request")
            click("Accept request")
            compose.onNodeWithText("Confirm").performClick()
        }
        if (booking.status in listOf("requested", "accepted")) {
            waitFor("Start job")
            click("Start job")
            compose.onNodeWithText("Confirm").performClick()
        }
        if (booking.status in listOf("requested", "accepted", "in_progress")) {
            waitFor("Complete job")
            click("Complete job")
            compose.onNodeWithText("Final agreed amount (LKR)").performTextInput("2500.50")
            compose.onNodeWithText("Confirm").performClick()
        }
        if (!booking.directPaymentReceived) {
            waitFor("Record payment received")
            click("Record payment received")
            compose.onNodeWithText("Confirm").performClick()
        }
        waitFor("Provider has recorded payment received.")
        scroll("Write a message")
        compose.onNodeWithText("Write a message").performTextInput("Provider integration reply")
        click("Send message")
        compose.waitUntil(30_000) { !market.state.value.saving }
        val completed = runBlocking { repository.booking(booking.id) }!!
        assertEquals("completed", completed.status)
        assertEquals(2500.50, completed.finalAmount!!, 0.001)
        assertTrue(completed.directPaymentReceived)
        shot("provider-completed")
    }

    @Test
    fun customerSeesReceipt() {
        signIn("customer")
        val booking = runBlocking { repository.snapshot().bookings }.single()
        compose.setContent {
            MaterialTheme {
                market = viewModel()
                ObserveMarketplace(market)
                ConnectedBookingDetails(booking.id, market, onBack = {})
            }
        }
        waitFor("Provider has recorded payment received.")
        waitFor("Provider integration reply")
        assertTrue(
            runBlocking { repository.events(booking.id) }
                .any { it.kind == "direct_payment_received" }
        )
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        shot("customer-receipt")
    }

    @Test
    fun customerAccountSignIn() {
        val email = args.getString("fixoom.customer.email").orEmpty()
        val password = args.getString("fixoom.password").orEmpty()
        assumeTrue(
            email.startsWith("fixoom-e2e-") &&
                email.endsWith("@example.invalid") &&
                password.isNotBlank()
        )
        runBlocking { AuthRepository().signOut() }
        authenticated = true
        compose.setContent {
            MaterialTheme {
                market = viewModel()
                ObserveMarketplace(market)
                ConnectedAccount(market, {})
            }
        }
        compose.onNodeWithText("Email address").performTextInput(email)
        compose.onNodeWithText("Password").performTextInput(password)
        click("Sign in")
        compose.waitUntil(30_000) {
            market.state.value.data.profile?.role == AppRole.CUSTOMER && !market.state.value.saving
        }
        assertTrue(
            market.state.value.data.profile!!.fullName.orEmpty().startsWith("Fixoom Test customer")
        )
        shot("customer-account")
    }
}
