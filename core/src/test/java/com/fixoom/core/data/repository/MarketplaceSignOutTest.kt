package com.fixoom.core.data.repository

import androidx.lifecycle.ViewModelStore
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.*
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test

/** The real view-model must clear private cached state before a failing public reload. */
@OptIn(
    SupabaseInternal::class,
    ExperimentalCoroutinesApi::class,
    kotlin.time.ExperimentalTime::class,
)
class MarketplaceSignOutTest {
    @Test
    fun customerAccountDisappearsEvenWhenPostLogoutRefreshFails() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val offline = AtomicBoolean(false)
        val id = "10000000-0000-4000-8000-000000000004"
        val client =
            createSupabaseClient("https://fixoom-signout.invalid", "fixture-public-key") {
                httpEngine = MockEngine { request ->
                    val logout = request.url.encodedPath == "/auth/v1/logout"
                    val failed = !logout && offline.get()
                    val body =
                        when {
                            failed -> "{}"
                            logout -> "{}"
                            request.url.encodedPath == "/rest/v1/profiles" ->
                                """[{"id":"$id","role":"customer","account_status":"active","full_name":"Isolated Customer"}]"""
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
            client.auth.importSession(
                UserSession(
                    "fixture-access",
                    "fixture-refresh",
                    expiresIn = 3600,
                    tokenType = "bearer",
                    user = UserInfo(id = id, aud = "authenticated"),
                ),
                autoRefresh = false,
            )
            val repo = MarketplaceRepository(client)
            val vm = MarketplaceViewModel(repo, AuthRepository(client))
            store.put("market", vm)
            withTimeout(5_000) { vm.state.first { !it.loading && it.data.profile != null } }
            val oldSnapshot = vm.state.value.data
            offline.set(true)
            vm.signOut()
            val done =
                withTimeout(5_000) { vm.state.first { !it.saving && it.notice == "Signed out." } }
            assertNull(client.auth.currentSessionOrNull())
            assertNull(done.data.profile)
            assertTrue(done.data.bookings.isEmpty())
            assertTrue(done.data.notifications.isEmpty())
            assertNotNull(done.error) // Honest public refresh error, without restoring the account.
            assertFalse(repo.isCurrentAccount(oldSnapshot.profile!!.id))
        } finally {
            store.clear()
            client.close()
            Dispatchers.resetMain()
        }
    }
}
