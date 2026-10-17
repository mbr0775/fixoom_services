package com.fixoom.core.data.repository

import androidx.lifecycle.ViewModelStore
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.*
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

/** Isolated transport failures; no production accounts or bookings are created. */
@OptIn(
    SupabaseInternal::class,
    ExperimentalCoroutinesApi::class,
    kotlin.time.ExperimentalTime::class,
)
class BookingDetailViewModelTest {
    @get:Rule val timeout: Timeout = Timeout.seconds(15)

    @Test
    fun secondaryFailuresKeepSavedBookingAndRecoverIndependently() = runBlocking {
        fixture { vm, failure, _ ->
            for (part in listOf("booking_events", "booking_messages", "booking_locations")) {
                failure.set(part)
                vm.open("booking-a")
                val failed = loaded(vm)
                assertEquals("booking-a", failed.booking?.id)
                assertNull(failed.error)
                when (part) {
                    "booking_events" -> assertNotNull(failed.historyError)
                    "booking_messages" -> assertNotNull(failed.messagesError)
                    else -> assertNotNull(failed.locationError)
                }
                failure.set("")
                vm.refresh()
                val recovered = loaded(vm)
                assertNull(recovered.historyError)
                assertNull(recovered.messagesError)
                assertNull(recovered.locationError)
                assertEquals(1, recovered.events.size)
            }
        }
    }

    @Test
    fun primaryFailureShowsRetryAndAccessLossClearsPreviouslyLoadedDetails() = runBlocking {
        fixture { vm, failure, _ ->
            vm.open("booking-a")
            loaded(vm)
            failure.set("bookings")
            vm.refresh()
            val failed = loaded(vm)
            assertEquals("booking-a", failed.booking?.id)
            assertNotNull(failed.error)
            assertFalse(failed.error!!.contains("private-server-detail"))
            failure.set("no-access")
            vm.refresh()
            val unavailable = loaded(vm)
            assertNull(unavailable.booking)
            assertTrue(unavailable.events.isEmpty())
            assertTrue(unavailable.messages.isEmpty())
            assertNull(unavailable.location)
        }
    }

    @Test
    fun refreshDuringAnExistingReadIsNotDropped() = runBlocking {
        fixture { vm, failure, reads ->
            failure.set("slow")
            vm.open("booking-a")
            vm.refresh()
            withTimeout(5_000) { vm.state.first { !it.extrasLoading && reads.get() >= 2 } }
            assertEquals(2, reads.get())
        }
    }

    @Test
    fun changingBookingCancelsOldReadAndDoesNotMixHistories() = runBlocking {
        fixture { vm, failure, _ ->
            failure.set("slow")
            vm.open("booking-a")
            vm.open("booking-b")
            val current = loaded(vm)
            assertEquals("booking-b", current.booking?.id)
            assertEquals("booking-b", current.events.single().bookingId)
        }
    }

    private suspend fun loaded(vm: BookingDetailViewModel) =
        withTimeout(5_000) { vm.state.first { !it.loading && !it.extrasLoading } }

    private suspend fun fixture(
        block: suspend (BookingDetailViewModel, AtomicReference<String>, AtomicInteger) -> Unit
    ) {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val failure = AtomicReference("")
        val reads = AtomicInteger()
        val client =
            createSupabaseClient("https://fixoom-bookings.invalid", "fixture-public-key") {
                httpEngine = MockEngine { request ->
                    val table = request.url.pathSegments.last()
                    if (table == "bookings") reads.incrementAndGet()
                    if (failure.get() == "slow") delay(150)
                    val key =
                        request.url.parameters[if (table == "bookings") "id" else "booking_id"]!!
                            .removePrefix("eq.")
                    val failed = failure.get() == table
                    val body =
                        when {
                            failed -> """{"code":"XX000","message":"private-server-detail"}"""
                            table == "bookings" && failure.get() == "no-access" -> "[]"
                            table == "bookings" ->
                                """[{"id":"$key","reference":"FIX-TEST","customer_id":"customer-test","provider_id":"provider-test","category_slug":"plumbing","scheduled_at":"2026-10-18T04:30:00Z","service_address":"Isolated test address","problem_description":"Isolated test request","status":"requested","created_at":"2026-10-17T00:00:00Z"}]"""
                            table == "booking_events" ->
                                """[{"id":"event-test","booking_id":"$key","kind":"requested","created_at":"2026-10-17T00:00:00Z"}]"""
                            else -> "[]"
                        }
                    respond(
                        body,
                    if (failed) HttpStatusCode.Forbidden else HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                install(Auth) {
                    autoLoadFromStorage = false
                    autoSaveToStorage = false
                    alwaysAutoRefresh = false
                    autoSetupPlatform = false
                    enableLifecycleCallbacks = false
                    sessionManager = MemorySessionManager()
                    codeVerifierCache = MemoryCodeVerifierCache()
                }
                install(Postgrest)
            }
        val store = ViewModelStore()
        try {
            client.auth.clearSession()
            val vm = BookingDetailViewModel(MarketplaceRepository(client))
            store.put("booking", vm)
            block(vm, failure, reads)
        } finally {
            store.clear()
            client.close()
            Dispatchers.resetMain()
        }
    }
}
