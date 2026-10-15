package com.fixoom.core.data.repository

import com.fixoom.core.model.AppRole
import com.fixoom.core.model.AuthResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

/** HTTP fixtures are isolated to this unit test; no production service is contacted. */
@OptIn(kotlin.time.ExperimentalTime::class)
class AuthRepositoryTest {
    @get:Rule val timeout: Timeout = Timeout.seconds(15)

    @Test
    fun unconfirmedSignupDoesNotReadPrivateProfile() = runBlocking {
        fixture().useClient { client, requests ->
            assertEquals(AuthResult.EmailVerificationRequired, register(client))
            assertNull(client.auth.currentSessionOrNull())
            assertEquals(listOf("/auth/v1/signup"), requests)
        }
    }

    @Test
    fun unconfirmedSignupCannotReuseAnOlderSession() = runBlocking {
        fixture().useClient { client, requests ->
            client.auth.importSession(oldSession(), autoRefresh = false)
            assertEquals(AuthResult.EmailVerificationRequired, register(client))
            assertNull(client.auth.currentSessionOrNull())
            assertFalse(requests.any { it.contains("/profiles") })
        }
    }

    @Test
    fun confirmedSignupUsesNewSessionAndRequiresProviderApproval() = runBlocking {
        fixture(signupSession = true).useClient { client, requests ->
            assertEquals(AuthResult.ProviderApprovalPending, register(client))
            assertEquals(USER_ID, client.auth.currentUserOrNull()?.id)
            assertTrue(requests.contains("/rest/v1/profiles"))
        }
    }

    @Test
    fun missingProfileFailsClosedWithRecoveryMessage() = runBlocking {
        fixture(emptyProfile = true).useClient { client, _ ->
            val result = AuthRepository(client).signIn(EMAIL, PASSWORD, AppRole.PROVIDER)
            assertTrue(result is AuthResult.Error)
            val message = (result as AuthResult.Error).message
            assertTrue(message.contains("profile", ignoreCase = true))
            assertTrue(message.contains("sign in again", ignoreCase = true))
            assertFalse(message.contains("List is empty"))
            assertNull(client.auth.currentSessionOrNull())
        }
    }

    @Test
    fun staleRestoredProfileIsNotTreatedAsAnApprovedAccount() = runBlocking {
        fixture(emptyProfile = true).useClient { client, _ ->
            client.auth.importSession(oldSession(), autoRefresh = false)
            val result = AuthRepository(client).restoreSession(AppRole.PROVIDER)
            assertTrue(result is AuthResult.Error)
            assertNull(client.auth.currentSessionOrNull())
        }
    }

    @Test
    fun unconfirmedSignInExplainsVerificationWithoutRawServerText() = runBlocking {
        fixture(signInError = "email_not_confirmed").useClient { client, _ ->
            val result = AuthRepository(client).signIn(EMAIL, PASSWORD, AppRole.PROVIDER)
            assertEquals(AuthResult.EmailVerificationRequired, result)
        }
    }

    @Test
    fun failedSignupDoesNotReportVerificationOrExposeServerDetails() = runBlocking {
        fixture(signupError = true).useClient { client, requests ->
            val result = register(client)
            assertTrue(result is AuthResult.Error)
            assertFalse((result as AuthResult.Error).message.contains("private-server-detail"))
            assertFalse(requests.any { it.contains("/profiles") })
        }
    }

    @Test
    fun customerSessionCannotEnterProviderApplication() = runBlocking {
        fixture(profileRole = "customer").useClient { client, _ ->
            val result = AuthRepository(client).signIn(EMAIL, PASSWORD, AppRole.PROVIDER)
            assertTrue(result is AuthResult.WrongApplication)
            assertNull(client.auth.currentSessionOrNull())
        }
    }

    private suspend fun register(client: SupabaseClient) =
        AuthRepository(client)
            .register("Fixture Professional", EMAIL, "", PASSWORD, AppRole.PROVIDER)

    private fun oldSession() =
        UserSession(
            accessToken = "fixture-old-access-token",
            refreshToken = "fixture-old-refresh-token",
            expiresIn = 3600,
            tokenType = "bearer",
            user = UserInfo(id = OLD_ID, aud = "authenticated", email = "old@fixoom-auth.invalid"),
        )

    @OptIn(SupabaseInternal::class)
    private fun fixture(
        signupSession: Boolean = false,
        emptyProfile: Boolean = false,
        signInError: String? = null,
        signupError: Boolean = false,
        profileRole: String = "provider",
    ): Fixture {
        val requests = mutableListOf<String>()
        val user =
            """{"id":"$USER_ID","aud":"authenticated","email":"$EMAIL","role":"authenticated","email_confirmed_at":"2026-10-15T00:00:00Z"}"""
        val session =
            """{"access_token":"fixture-access-token","refresh_token":"fixture-refresh-token","expires_in":3600,"token_type":"bearer","user":$user}"""
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            requests.add(path)
            var status = HttpStatusCode.OK
            val response =
                when (path) {
                    "/auth/v1/signup" ->
                        when {
                            signupError -> {
                                status = HttpStatusCode.InternalServerError
                                """{"code":500,"error_code":"unexpected_failure","msg":"private-server-detail"}"""
                            }
                            signupSession -> session
                            else ->
                                """{"id":"$USER_ID","aud":"authenticated","email":"$EMAIL","confirmation_sent_at":"2026-10-15T00:00:00Z"}"""
                        }
                    "/auth/v1/token" ->
                        if (signInError == null) session
                        else {
                            status = HttpStatusCode.BadRequest
                            """{"code":400,"error_code":"$signInError","msg":"private-server-detail"}"""
                        }
                    "/rest/v1/profiles" ->
                        if (emptyProfile) "[]"
                        else
                            """[{"id":"$USER_ID","role":"$profileRole","account_status":"pending","full_name":"Fixture Professional"}]"""
                    "/auth/v1/logout" -> "{}"
                    else -> error("Unexpected test request: $path")
                }
            respond(response, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client =
            createSupabaseClient("https://fixoom-auth.invalid", "fixture-public-key") {
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
        return Fixture(client, requests)
    }

    private data class Fixture(val client: SupabaseClient, val requests: List<String>) {
        suspend fun useClient(block: suspend (SupabaseClient, List<String>) -> Unit) {
            // With platform/storage setup disabled, initialize the SDK's auth state explicitly.
            client.auth.clearSession()
            try {
                block(client, requests)
            } finally {
                client.close()
            }
        }
    }

    private companion object {
        const val USER_ID = "10000000-0000-4000-8000-000000000001"
        const val OLD_ID = "10000000-0000-4000-8000-000000000002"
        const val EMAIL = "provider@fixoom-auth.invalid"
        const val PASSWORD = "fixture-password"
    }
}
