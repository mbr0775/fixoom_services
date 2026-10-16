package com.fixoom.core.data.repository

import com.fixoom.core.model.*
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BookingAvailabilityTest {
    private val nine = Instant.parse("2026-10-17T03:30:00Z")
    private val busy = BusyTime(nine.toString(), nine.plusSeconds(3600).toString())

    @Test
    fun reservationsUseExclusiveEndAndCheckFullRequestedDuration() {
        assertTrue(busy.overlaps(nine, 60))
        assertTrue(busy.overlaps(nine.minusSeconds(1800), 60))
        assertFalse(busy.overlaps(nine.minusSeconds(3600), 60))
        assertFalse(busy.overlaps(nine.plusSeconds(3600), 60))
    }

    @Test
    fun nextTimeSkipsConsecutiveReservationsWithoutLosingDuration() {
        val second = BusyTime(nine.plusSeconds(3600).toString(), nine.plusSeconds(7200).toString())
        assertEquals(nine.plusSeconds(7200), nextUnreservedTime(nine, 60, listOf(second, busy)))
        assertEquals(
            nine.minusSeconds(3600),
            nextUnreservedTime(nine.minusSeconds(3600), 60, listOf(busy)),
        )
    }

    @Test
    fun busyBookingErrorIsSpecificAndUnknownDetailsStayPrivate() = runBlocking {
        val client =
            createSupabaseClient("https://booking-errors.invalid", "fixture-key") {
                httpEngine = MockEngine {
                    respond(
                        """{"code":"P0001","message":"This provider already has an accepted job during that time","details":null,"hint":null}""",
                        HttpStatusCode.BadRequest,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                install(Postgrest)
            }
        try {
            val error =
                runCatching {
                        MarketplaceRepository(client)
                            .createBooking(
                                "key",
                                "provider",
                                nine.toString(),
                                "Test address",
                                "Test service",
                                "plumbing",
                            )
                    }
                    .exceptionOrNull() as Exception
            assertTrue(marketplaceActionError(error).contains("already booked"))
            assertFalse(marketplaceActionError(error).contains("connection"))
            assertFalse(
                marketplaceActionError(Exception("private headers and database error"))
                    .contains("private headers")
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun scheduleRequestUsesChosenProviderAndSriLankaDate() = runBlocking {
        val client =
            createSupabaseClient("https://booking-schedule.invalid", "fixture-key") {
                httpEngine = MockEngine { request ->
                    assertTrue(request.url.encodedPath.endsWith("provider_busy_times"))
                    respond(
                        """[{"starts_at":"2026-10-17T03:30:00Z","ends_at":"2026-10-17T04:30:00Z"}]""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                install(Postgrest)
            }
        try {
            assertEquals(
                listOf(busy),
                MarketplaceRepository(client).busyTimes("provider", "2026-10-17"),
            )
        } finally {
            client.close()
        }
    }
}
