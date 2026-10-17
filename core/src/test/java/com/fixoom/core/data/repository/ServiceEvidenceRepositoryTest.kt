package com.fixoom.core.data.repository

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.*
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.resumable.MemoryResumableCache
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(SupabaseInternal::class, kotlin.time.ExperimentalTime::class)
class ServiceEvidenceRepositoryTest {
    private val uid = "10000000-0000-4000-8000-000000000001"
    private val context = "20000000-0000-4000-8000-000000000002"
    private val file = "30000000-0000-4000-8000-000000000003"
    private val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1, 0xff.toByte(), 0xd9.toByte())

    @Test
    fun invalidFileNeverContactsBackend() = runBlocking {
        withClient { repo, calls ->
            try {
                repo.upload(context, false, file, byteArrayOf(1, 2, 3), false)
                fail("Invalid file accepted")
            } catch (_: IllegalArgumentException) {}
            assertTrue(calls.isEmpty())
        }
    }

    @Test
    fun bookingUploadPublishesOnlyAfterStorageAndScopesTheBooking() = runBlocking {
        withClient { repo, calls ->
            val saved = repo.upload(context, false, file, jpeg, false)
            assertEquals(file, saved.id)
            assertEquals(listOf("upload", "publish"), calls.map { it.first })
            val payload = calls.last().second!!.jsonObject
            assertEquals(context, payload["p_booking_id"]!!.jsonPrimitive.content)
            assertEquals(JsonNull, payload["p_complaint_id"])
            assertEquals("$uid/$context/$file.jpg", payload["p_path"]!!.jsonPrimitive.content)
        }
    }

    @Test
    fun complaintUploadNeverAddsItToTheBookingConversation() = runBlocking {
        withClient { repo, calls ->
            repo.upload(context, true, file, jpeg, false)
            val payload = calls.last().second!!.jsonObject
            assertEquals(context, payload["p_complaint_id"]!!.jsonPrimitive.content)
            assertEquals(JsonNull, payload["p_booking_id"])
        }
    }

    @Test
    fun failedPublicationDoesNotDeletePotentiallyCommittedEvidence() = runBlocking {
        withClient(failPublish = true) { repo, calls ->
            try {
                repo.upload(context, true, file, jpeg, false)
                fail("Denied publication succeeded")
            } catch (_: io.github.jan.supabase.exceptions.RestException) {}
            assertEquals(listOf("upload", "publish"), calls.map { it.first })
        }
    }

    @Test
    fun mediaValidationRejectsWrongContainerAndOversizedFiles() {
        assertFalse(ServiceEvidenceRepository.validMedia("not a video".toByteArray(), true))
        assertFalse(
            ServiceEvidenceRepository.validMedia(
                ByteArray(ServiceEvidenceRepository.MAX_BYTES + 1),
                true,
            )
        )
        assertTrue(
            ServiceEvidenceRepository.validMedia(
                byteArrayOf(0, 0, 0, 12) + "ftypisom".toByteArray(),
                true,
            )
        )
    }

    @Test
    fun interruptedUploadsRecoverThroughValidatedPublicationForBothDuplicateStatuses() =
        runBlocking {
            for (status in listOf(400, 409)) withClient(duplicateStatus = status) { repo, calls ->
                assertEquals(file, repo.upload(context, false, file, jpeg, false).id)
                assertEquals(listOf("upload", "publish"), calls.map { it.first })
            }
        }

    @Test
    fun complaintsUseStableRequestKeysAndListsAreScopedToCurrentReporter() = runBlocking {
        withClient { repo, calls ->
            repo.submit(
                file,
                context,
                "Private report",
                "A real concern described for isolated SDK testing.",
            )
            repo.submit(
                file,
                context,
                "Private report",
                "A real concern described for isolated SDK testing.",
            )
            assertEquals(calls[0].second, calls[1].second)
            repo.complaints(context)
            assertEquals(
                "eq.$uid",
                calls.last().second!!.jsonObject["reporter_id"]!!.jsonPrimitive.content,
            )
            assertEquals(
                "eq.$context",
                calls.last().second!!.jsonObject["booking_id"]!!.jsonPrimitive.content,
            )
        }
    }

    @Test
    fun malformedComplaintNeverContactsBackend() = runBlocking {
        withClient { repo, calls ->
            try {
                repo.submit(file, null, "bad", "short")
                fail("Invalid complaint accepted")
            } catch (_: IllegalArgumentException) {}
            assertTrue(calls.isEmpty())
        }
    }

    private suspend fun withClient(
        failPublish: Boolean = false,
        duplicateStatus: Int? = null,
        block: suspend (ServiceEvidenceRepository, MutableList<Pair<String, JsonElement?>>) -> Unit,
    ) {
        val calls = mutableListOf<Pair<String, JsonElement?>>()
        val engine = MockEngine { request ->
            val headers = headersOf(HttpHeaders.ContentType, "application/json")
            val path = request.url.encodedPath
            when {
                request.method == HttpMethod.Post &&
                    path.startsWith("/storage/v1/object/service-evidence/$uid/") -> {
                    calls.add("upload" to null)
                    respond(
                        if (duplicateStatus == null) """{"Key":"uploaded","Id":"$file"}"""
                        else
                            """{"statusCode":"$duplicateStatus","error":"Duplicate","message":"The resource already exists"}""",
                        duplicateStatus?.let(HttpStatusCode::fromValue) ?: HttpStatusCode.OK,
                        headers,
                    )
                }
                path == "/rest/v1/rpc/submit_complaint" -> {
                    calls.add(
                        "submit" to
                            Json.parseToJsonElement(
                                (request.body as OutgoingContent.ByteArrayContent)
                                    .bytes()
                                    .decodeToString()
                            )
                    )
                    respond(
                        """{"id":"$file","reporter_id":"$uid","booking_id":"$context","subject":"Private report","description":"SDK test only","status":"open","admin_response":"","created_at":"2026-10-17T10:00:00Z"}""",
                        HttpStatusCode.OK,
                        headers,
                    )
                }
                path == "/rest/v1/complaints" -> {
                    calls.add(
                        "list" to
                            buildJsonObject {
                                request.url.parameters.entries().forEach { (key, value) ->
                                    put(key, value.first())
                                }
                            }
                    )
                    respond("[]", HttpStatusCode.OK, headers)
                }
                path == "/rest/v1/rpc/publish_service_attachment" -> {
                    calls.add(
                        "publish" to
                            Json.parseToJsonElement(
                                (request.body as OutgoingContent.ByteArrayContent)
                                    .bytes()
                                    .decodeToString()
                            )
                    )
                    respond(
                        if (failPublish) """{"message":"permission denied","code":"42501"}"""
                        else
                            """{"id":"$file","owner_id":"$uid","object_path":"$uid/$context/$file.jpg","media_type":"image/jpeg","byte_size":5,"created_at":"2026-10-17T10:00:00Z"}""",
                        if (failPublish) HttpStatusCode.Forbidden else HttpStatusCode.OK,
                        headers,
                    )
                }
                else -> error("Unexpected SDK request: ${request.method} $path")
            }
        }
        val client =
            createSupabaseClient("https://fixoom-evidence.invalid", "fixture-public-key") {
                httpEngine = engine
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
                install(Storage) { resumable { cache = MemoryResumableCache() } }
            }
        try {
            client.auth.importSession(
                UserSession(
                    accessToken = "fixture-token",
                    refreshToken = "fixture-refresh",
                    expiresIn = 3600,
                    tokenType = "bearer",
                    user = UserInfo(id = uid, aud = "authenticated"),
                ),
                autoRefresh = false,
            )
            block(ServiceEvidenceRepository(client), calls)
        } finally {
            client.close()
        }
    }
}
