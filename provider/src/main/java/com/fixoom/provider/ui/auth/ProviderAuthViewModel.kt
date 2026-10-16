package com.fixoom.provider.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixoom.core.data.repository.AuthRepository
import com.fixoom.core.data.repository.MarketplaceRepository
import com.fixoom.core.model.AppRole
import com.fixoom.core.model.AuthResult
import com.fixoom.core.model.ServiceCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class ProviderAuthUiState(
    val isLoading: Boolean = true,
    val result: AuthResult? = null,
)

data class ApprovalStatusState(
    val checking: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class ProviderAuthViewModel(private val authRepository: AuthRepository = AuthRepository()) :
    ViewModel() {

    private val _uiState = MutableStateFlow(ProviderAuthUiState())

    val uiState: StateFlow<ProviderAuthUiState> = _uiState.asStateFlow()

    private val _approvalStatus = MutableStateFlow(ApprovalStatusState())
    val approvalStatus = _approvalStatus.asStateFlow()
    private var approvalJob: Job? = null
    private var authJob: Job? = null
    private val _categories = MutableStateFlow<List<ServiceCategory>>(emptyList())
    val categories = _categories.asStateFlow()
    private val _categoryError = MutableStateFlow<String?>(null)
    val categoryError = _categoryError.asStateFlow()

    fun loadCategories() {
        viewModelScope.launch {
            _categoryError.value = null
            try {
                _categories.value = withTimeout(15_000) { MarketplaceRepository().categories() }
                if (_categories.value.isEmpty())
                    _categoryError.value =
                        "No service categories are currently available. Contact Fixoom support or retry."
            } catch (e: CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                _categoryError.value = "Service categories timed out. Try again."
            } catch (_: Exception) {
                _categoryError.value =
                    "Could not load service categories. Check your connection and retry."
            }
        }
    }

    fun checkApprovalStatus() {
        if (_approvalStatus.value.checking) return
        _approvalStatus.value = ApprovalStatusState(checking = true)
        approvalJob = viewModelScope.launch {
            try {
                when (
                    val result =
                        withTimeout(20_000) {
                            authRepository.restoreSession(AppRole.PROVIDER)
                        }
                ) {
                    AuthResult.ProviderApprovalPending ->
                        _approvalStatus.value =
                            ApprovalStatusState(
                                message =
                                    "Status checked: your application is still awaiting administrator approval."
                            )
                    is AuthResult.Error ->
                        _approvalStatus.value = ApprovalStatusState(error = result.message)
                    else -> {
                        _approvalStatus.value = ApprovalStatusState()
                        _uiState.value = ProviderAuthUiState(isLoading = false, result = result)
                    }
                }
            } catch (cancelled: CancellationException) {
                if (cancelled !is TimeoutCancellationException) throw cancelled
                _approvalStatus.value =
                    ApprovalStatusState(
                        error = "Status check timed out. Check your connection and try again."
                    )
            } catch (_: Exception) {
                _approvalStatus.value =
                    ApprovalStatusState(
                        error = "Could not check approval. Check your connection and try again."
                    )
            }
        }
    }

    init {
        restoreSession()
    }

    fun restoreSession() {
        executeAuthRequest {
            authRepository.restoreSession(requiredRole = AppRole.PROVIDER)
        }
    }

    fun signIn(
        email: String,
        password: String,
    ) {
        executeAuthRequest {
            authRepository.signIn(
                email = email,
                password = password,
                requiredRole = AppRole.PROVIDER,
            )
        }
    }

    fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        skills: List<String>,
    ) {
        executeAuthRequest {
            authRepository.register(
                fullName = fullName,
                email = email,
                phone = phone,
                password = password,
                role = AppRole.PROVIDER,
                skills = skills,
            )
        }
    }

    fun requestPasswordReset(email: String) {
        executeAuthRequest {
            authRepository.requestPasswordReset(
                email = email,
                redirectUrl = PROVIDER_AUTH_REDIRECT,
            )
        }
    }

    fun updatePassword(newPassword: String) {
        executeAuthRequest {
            val result = authRepository.updatePassword(newPassword)

            if (result is AuthResult.PasswordUpdated) {
                authRepository.signOut()
                result
            } else {
                result
            }
        }
    }

    fun signOut() {
        executeAuthRequest {
            authRepository.signOut()
        }
    }

    fun returnToSignIn() {
        executeAuthRequest { authRepository.returnToSignIn() }
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(result = null)
    }

    private fun executeAuthRequest(request: suspend () -> AuthResult) {
        approvalJob?.cancel()
        _approvalStatus.value = ApprovalStatusState()
        authJob?.cancel()
        authJob = viewModelScope.launch {
            _uiState.value =
                ProviderAuthUiState(
                    isLoading = true,
                    result = null,
                )

            val result =
                try {
                    withTimeout(20_000) { request() }
                } catch (cancelled: CancellationException) {
                    if (cancelled !is TimeoutCancellationException) throw cancelled
                    AuthResult.Error("Connection timed out. Please try again.")
                } catch (_: Exception) {
                    AuthResult.Error("Unable to connect. Please try again.")
                }

            _uiState.value =
                ProviderAuthUiState(
                    isLoading = false,
                    result = result,
                )
        }
    }

    private companion object {
        const val PROVIDER_AUTH_REDIRECT = "fixoom-provider://auth"
    }
}
