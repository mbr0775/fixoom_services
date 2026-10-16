package com.fixoom.core.data.repository

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

/** Captures SDK requests in memory; no production records or credentials. */
class ProviderSkillsRepositoryTest {
    @Test
    fun requestsNeverSendAnApprovalClaimAndBookingSpecifiesCategoryAndDuration() = runBlocking {
        val requests = mutableListOf<Pair<String, JsonObject>>()
        val client =
            createSupabaseClient("https://fixoom-skills.invalid", "fixture-public-key") {
                httpEngine = MockEngine { request ->
                    requests +=
                        request.url.encodedPath.substringAfterLast('/') to
                            Json.parseToJsonElement(
                                    (request.body as OutgoingContent.ByteArrayContent)
                                        .bytes()
                                        .decodeToString()
                                )
                                .jsonObject
                    respond(
                        if (request.url.encodedPath.endsWith("create_service_booking_for_skill"))
                            """{"id":"booking","reference":"TEST","customer_id":"customer","provider_id":"provider","category_slug":"carpentry","scheduled_at":"2026-11-01T09:00:00Z","service_address":"Test area","problem_description":"Test service","status":"requested","created_at":"2026-10-16T09:00:00Z","duration_minutes":120}"""
                        else "null",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                install(Postgrest)
            }
        try {
            val repo = MarketplaceRepository(client)
            repo.requestSkill("carpentry", "I offer furniture repair services.")
            assertEquals("request_provider_skill", requests[0].first)
            assertEquals(setOf("p_category_slug", "p_description"), requests[0].second.keys)
            repo.withdrawSkill("carpentry")
            assertEquals("withdraw_provider_skill", requests[1].first)
            val booking =
                repo.createBooking(
                    "key",
                    "provider",
                    "2026-11-01T09:00:00Z",
                    "Test area",
                    "Test service",
                    "carpentry",
                    120,
                )
            assertEquals("create_service_booking_for_skill", requests[2].first)
            assertEquals("carpentry", requests[2].second["p_category_slug"]!!.jsonPrimitive.content)
            assertEquals(120, requests[2].second["p_duration_minutes"]!!.jsonPrimitive.int)
            assertEquals(120, booking.durationMinutes)
        } finally {
            client.close()
        }
    }
}
