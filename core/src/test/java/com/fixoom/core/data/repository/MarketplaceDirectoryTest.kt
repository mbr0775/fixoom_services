package com.fixoom.core.data.repository

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

/** SDK transport fixtures only: no records are written to the real database. */
@OptIn(SupabaseInternal::class)
class MarketplaceDirectoryTest {
    @get:Rule val timeout: Timeout = Timeout.seconds(20)

    @Test
    fun emptyDirectoryStaysEmpty() = runBlocking {
        withDirectory("[]") { snapshot, requests ->
            assertTrue(snapshot.professionals.isEmpty())
            assertEquals(listOf("plumbing"), snapshot.categories.map { it.slug })
            assertTrue(requests.any { it.contains("is_available=eq.true") })
        }
    }

    @Test
    fun directoryUsesReturnedIdentityAndExcludesUnavailableOrIncompleteProfiles() = runBlocking {
        val rows =
            """[
            {"id":"real-id","display_name":"Returned professional","category_slug":"plumbing","approved_categories":["plumbing"],"service_area":"Colombo","bio":"Registered introduction","is_available":true},
            {"id":"hidden-category","display_name":"Inactive service","category_slug":"painting","approved_categories":["painting"],"service_area":"Colombo","is_available":true},
            {"id":"offline","display_name":"Unavailable","category_slug":"plumbing","approved_categories":["plumbing"],"service_area":"Colombo","is_available":false},
            {"id":"no-name","display_name":"  ","category_slug":"plumbing","approved_categories":["plumbing"],"service_area":"Colombo","is_available":true},
            {"id":"no-area","display_name":"Incomplete","category_slug":"plumbing","approved_categories":["plumbing"],"service_area":"  ","is_available":true}
        ]"""
        withDirectory(rows) { snapshot, _ ->
            val professional = snapshot.professionals.single()
            assertEquals("real-id", professional.id)
            assertEquals("Returned professional", professional.displayName)
            assertEquals("Registered introduction", professional.bio)
            assertEquals("Colombo", professional.serviceArea)
        }
    }

    @Test
    fun unreviewedPrimaryIsHiddenAndApprovedSecondaryIsDiscoverable() = runBlocking {
        withDirectory(
            """[
          {"id":"unreviewed","display_name":"Pending skill","category_slug":"plumbing","service_area":"Colombo","is_available":true},
          {"id":"secondary","display_name":"Approved secondary","category_slug":"painting","approved_categories":["painting","plumbing"],"service_area":"Colombo","is_available":true,"provider_skills":[{"provider_id":"secondary","category_slug":"plumbing","description":"Approved service details","status":"approved"}]}
        ]"""
        ) { snapshot, _ ->
            assertEquals(listOf("secondary"), snapshot.professionals.map { it.id })
            assertTrue(snapshot.professionals.single().offers("plumbing"))
            assertEquals("Approved service details", snapshot.skills.single().description)
        }
    }

    @Test
    fun backendFailureCannotBecomeASampleDirectory() = runBlocking {
        var failed = false
        try {
            withDirectory("""{"message":"directory unavailable"}""", HttpStatusCode.Forbidden) {
                _,
                _ ->
                fail("A failed directory request must not return a substitute snapshot")
            }
        } catch (_: io.github.jan.supabase.exceptions.RestException) {
            failed = true
        }
        assertTrue(failed)
    }

    private suspend fun withDirectory(
        providers: String,
        providerStatus: HttpStatusCode = HttpStatusCode.OK,
        block: suspend (MarketplaceSnapshot, List<String>) -> Unit,
    ) {
        val requests = CopyOnWriteArrayList<String>()
        val engine = MockEngine { request ->
            requests.add(request.url.toString())
            val headers = headersOf(HttpHeaders.ContentType, "application/json")
            when (request.url.encodedPath) {
                "/rest/v1/provider_profiles" -> respond(providers, providerStatus, headers)
                "/rest/v1/service_categories" ->
                    respond(
                        """[{"slug":"plumbing","name":"Plumbing","is_active":true},{"slug":"painting","name":"Painting","is_active":false}]""",
                        HttpStatusCode.OK,
                        headers,
                    )
                else -> error("Unexpected SDK request: ${request.url.encodedPath}")
            }
        }
        val client =
            createSupabaseClient("https://fixoom-directory.invalid", "fixture-public-key") {
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
            }
        try {
            client.auth.clearSession()
            block(MarketplaceRepository(client).snapshot(), requests)
        } finally {
            client.close()
        }
    }
}
