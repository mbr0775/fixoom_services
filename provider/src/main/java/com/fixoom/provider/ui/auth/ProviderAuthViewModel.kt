package com.fixoom.provider.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixoom.core.data.repository.AuthRepository
import com.fixoom.core.model.AppRole
import com.fixoom.core.model.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProviderAuthUiState(
    val isLoading: Boolean = true,
    val result: AuthResult? = null
)

class ProviderAuthViewModel(
    private val authRepository: AuthRepository =
        AuthRepository()
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(ProviderAuthUiState())

    val uiState: StateFlow<ProviderAuthUiState> =
        _uiState.asStateFlow()

    init {
        restoreSession()
    }

    fun restoreSession() {
        executeAuthRequest {
            authRepository.restoreSession(
                requiredRole = AppRole.PROVIDER
            )
        }
    }

    fun signIn(
        email: String,
        password: String
    ) {
        executeAuthRequest {
            authRepository.signIn(
                email = email,
                password = password,
                requiredRole = AppRole.PROVIDER
            )
        }
    }

    fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String
    ) {
        executeAuthRequest {
            authRepository.register(
                fullName = fullName,
                email = email,
                phone = phone,
                password = password,
                role = AppRole.PROVIDER
            )
        }
    }

    fun requestPasswordReset(
        email: String
    ) {
        executeAuthRequest {
            authRepository.requestPasswordReset(
                email = email,
                redirectUrl = PROVIDER_AUTH_REDIRECT
            )
        }
    }

    fun updatePassword(
        newPassword: String
    ) {
        executeAuthRequest {
            val result =
                authRepository.updatePassword(newPassword)

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

    fun clearResult() {
        _uiState.value =
            _uiState.value.copy(result = null)
    }

    private fun executeAuthRequest(
        request: suspend () -> AuthResult
    ) {
        viewModelScope.launch {
            _uiState.value =
                ProviderAuthUiState(
                    isLoading = true,
                    result = null
                )

            val result = request()

            _uiState.value =
                ProviderAuthUiState(
                    isLoading = false,
                    result = result
                )
        }
    }

    private companion object {
        const val PROVIDER_AUTH_REDIRECT =
            "fixoom-provider://auth"
    }
}