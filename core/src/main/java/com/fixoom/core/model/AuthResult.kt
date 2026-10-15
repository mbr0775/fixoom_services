package com.fixoom.core.model

sealed interface AuthResult {

    data class Authenticated(
        val profile: Profile
    ) : AuthResult

    data object SignedOut : AuthResult

    data object EmailVerificationRequired : AuthResult

    data object ReturnToSignIn : AuthResult

    data object PasswordResetEmailSent : AuthResult

    data object PasswordUpdated : AuthResult

    data object ProviderApprovalPending : AuthResult

    data object AccountSuspended : AuthResult

    data object AccountRejected : AuthResult

    data class WrongApplication(
        val requiredRole: AppRole,
        val actualRole: AppRole
    ) : AuthResult

    data class Error(
        val message: String
    ) : AuthResult
}
