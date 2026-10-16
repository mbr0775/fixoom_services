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
class ProviderPhotoRepositoryTest {
    private val id = "10000000-0000-4000-8000-000000000001"
    private val old = "$id/20000000-0000-4000-8000-000000000002.jpg"
    private val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1, 0xff.toByte(), 0xd9.toByte())

    @Test
    fun replacementUploadsBeforePublishingAndOnlyThenDeletesTheOldPhoto() = runBlocking {
        withClient { repository, events, published ->
            val path = repository.save(jpeg)
            assertTrue(path!!.startsWith("$id/"))
            assertNotEquals(old, path)
            assertEquals(path, published.single())
            assertEquals(listOf("profile", "upload", "publish", "delete"), events)
        }
    }

    @Test
    fun removalClearsThePublishedPathBeforeDeletingStorage() = runBlocking {
        withClient { repository, events, published ->
            assertNull(repository.save(null))
            assertNull(published.single())
            assertEquals(listOf("profile", "publish", "delete"), events)
        }
    }

    @Test
    fun failedPublicationDoesNotDeleteEitherPotentiallyReferencedObject() = runBlocking {
        withClient(failPublish = true) { repository, events, _ ->
            try {
                repository.save(jpeg)
                fail("Publication should fail")
            } catch (_: io.github.jan.supabase.exceptions.RestException) {}
            assertEquals(listOf("profile", "upload", "publish"), events)
        }
    }

    @Test
    fun invalidOrOversizedDataNeverContactsStorage() = runBlocking {
        withClient { repository, events, _ ->
            for (bytes in
                listOf(byteArrayOf(1, 2, 3), ByteArray(ProviderPhotoRepository.MAX_BYTES + 1))) {
                try {
                    repository.save(bytes)
                    fail("Invalid photo accepted")
                } catch (_: IllegalArgumentException) {}
            }
            assertTrue(events.isEmpty())
        }
    }

    private suspend fun withClient(
        failPublish: Boolean = false,
        test: suspend (ProviderPhotoRepository, MutableList<String>, MutableList<String?>) -> Unit,
    ) {
        val events = mutableListOf<String>()
        val published = mutableListOf<String?>()
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            val headers = headersOf(HttpHeaders.ContentType, "application/json")
            when {
                path == "/rest/v1/provider_profiles" -> {
                    events.add("profile")
                    respond("""[{"id":"$id","avatar_path":"$old"}]""", HttpStatusCode.OK, headers)
                }
                path == "/rest/v1/rpc/set_provider_photo" -> {
                    events.add("publish")
                    val data =
                        (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
                    published.add(
                        Json.parseToJsonElement(data)
                            .jsonObject["p_avatar_path"]!!
                            .jsonPrimitive
                            .contentOrNull
                    )
                    respond(
                        if (failPublish) """{"message":"publication denied"}""" else "null",
                        if (failPublish) HttpStatusCode.Forbidden else HttpStatusCode.OK,
                        headers,
                    )
                }
                request.method == HttpMethod.Post &&
                    path.startsWith("/storage/v1/object/provider-photos/$id/") -> {
                    events.add("upload")
                    respond("""{"Key":"uploaded","Id":"30000000-0000-4000-8000-000000000003"}""", HttpStatusCode.OK, headers)
                }
                request.method == HttpMethod.Delete &&
                    path == "/storage/v1/object/provider-photos" -> {
                    events.add("delete")
                    respond("[]", HttpStatusCode.OK, headers)
                }
                else -> error("Unexpected SDK request: ${request.method} $path")
            }
        }
        val client =
            createSupabaseClient("https://fixoom-photo.invalid", "fixture-public-key") {
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
            client.auth.clearSession()
            client.auth.importSession(
                UserSession(
                    accessToken = "fixture-token",
                    refreshToken = "fixture-refresh",
                    expiresIn = 3600,
                    tokenType = "bearer",
                    user = UserInfo(id = id, aud = "authenticated"),
                ),
                autoRefresh = false,
            )
            test(ProviderPhotoRepository(client), events, published)
        } finally {
            client.close()
        }
    }
}
