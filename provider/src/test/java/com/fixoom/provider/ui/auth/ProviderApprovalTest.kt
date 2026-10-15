package com.fixoom.provider.ui.auth

import androidx.lifecycle.ViewModelStore
import com.fixoom.core.data.repository.AuthRepository
import com.fixoom.core.model.AuthResult
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.*
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

/** Isolated HTTP responses exercise the actual repository and approval ViewModel. */
@OptIn(
    SupabaseInternal::class,
    ExperimentalCoroutinesApi::class,
    kotlin.time.ExperimentalTime::class,
)
class ProviderApprovalTest {
    @get:Rule val timeout: Timeout = Timeout.seconds(15)

    @Test
    fun pendingCheckHasFeedbackAndApprovedCheckOpensWorkspace() = runBlocking {
        fixture { vm, status ->
            vm.checkApprovalStatus()
            val pending = withTimeout(5_000) { vm.approvalStatus.first { it.message != null } }
            assertFalse(pending.checking)
            assertTrue(pending.message!!.contains("still awaiting"))
            assertEquals(AuthResult.ProviderApprovalPending, vm.uiState.value.result)
            status.set("active")
            vm.checkApprovalStatus()
            withTimeout(5_000) { vm.uiState.first { it.result is AuthResult.Authenticated } }
            assertNull(vm.approvalStatus.value.error)
        }
    }

    @Test
    fun failedCheckShowsRetryableErrorWithoutApprovingAccount() = runBlocking {
        fixture { vm, status ->
            status.set("offline")
            vm.checkApprovalStatus()
            val failed = withTimeout(5_000) { vm.approvalStatus.first { it.error != null } }
            assertFalse(failed.checking)
            assertEquals(AuthResult.ProviderApprovalPending, vm.uiState.value.result)
            status.set("pending")
            vm.checkApprovalStatus()
            withTimeout(5_000) { vm.approvalStatus.first { it.message != null } }
            assertNull(vm.approvalStatus.value.error)
        }
    }

    @Test
    fun confirmationHandoffReturnsToSignIn() = runBlocking {
        fixture { vm, _ ->
            vm.returnToSignIn()
            withTimeout(5_000) { vm.uiState.first { it.result == AuthResult.ReturnToSignIn } }
        }
    }

    private suspend fun fixture(
        block: suspend (ProviderAuthViewModel, AtomicReference<String>) -> Unit
    ) {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val status = AtomicReference("pending")
        val id = "10000000-0000-4000-8000-000000000003"
        val client =
            createSupabaseClient("https://fixoom-approval.invalid", "fixture-public-key") {
                httpEngine = MockEngine { request ->
                    check(request.url.encodedPath == "/rest/v1/profiles")
                    if (status.get() == "offline")
                        respond(
                            "{}",
                            HttpStatusCode.Forbidden,
                            headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    else
                        respond(
                            """[{"id":"$id","role":"provider","account_status":"${status.get()}","full_name":"Fixture Professional"}]""",
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
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
                    "fixture-access-token",
                    "fixture-refresh-token",
                    expiresIn = 3600,
                    tokenType = "bearer",
                    user =
                        UserInfo(
                            aud = "authenticated",
                            id = id,
                            email = "provider@fixoom-approval.invalid",
                        ),
                ),
                autoRefresh = false,
            )
            val vm = ProviderAuthViewModel(AuthRepository(client))
            store.put("approval", vm)
            withTimeout(5_000) { vm.uiState.first { !it.isLoading } }
            assertEquals(AuthResult.ProviderApprovalPending, vm.uiState.value.result)
            block(vm, status)
        } finally {
            store.clear()
            client.close()
            Dispatchers.resetMain()
        }
    }
}
