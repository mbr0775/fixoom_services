package com.fixoom.core.data.repository

import com.fixoom.core.data.remote.FixoomSupabase
import com.fixoom.core.model.AccountStatus
import com.fixoom.core.model.AppRole
import com.fixoom.core.model.AuthResult
import com.fixoom.core.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository(
    private val client: SupabaseClient = FixoomSupabase.client
) {

    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        role: AppRole
    ): AuthResult {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanPhone = phone.trim()

        validateRegistration(
            fullName = cleanName,
            email = cleanEmail,
            password = password,
            role = role
        )?.let { error ->
            return AuthResult.Error(error)
        }

        return try {
            client.auth.awaitInitialization()

            client.auth.signUpWith(Email) {
                this.email = cleanEmail
                this.password = password

                data = buildJsonObject {
                    put("full_name", cleanName)
                    put("role", role.databaseValue())

                    if (cleanPhone.isNotBlank()) {
                        put("phone", cleanPhone)
                    }
                }
            }

            val currentUser =
                client.auth.currentUserOrNull()

            if (currentUser == null) {
                AuthResult.EmailVerificationRequired
            } else {
                resolveAccountAccess(
                    userId = currentUser.id,
                    requiredRole = role
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AuthResult.Error(
                message = exception.message
                    ?: "Registration failed. Please try again."
            )
        }
    }

    suspend fun signIn(
        email: String,
        password: String,
        requiredRole: AppRole
    ): AuthResult {
        val cleanEmail = email.trim().lowercase()

        if (cleanEmail.isBlank()) {
            return AuthResult.Error(
                "Email address is required."
            )
        }

        if (password.isBlank()) {
            return AuthResult.Error(
                "Password is required."
            )
        }

        return try {
            client.auth.awaitInitialization()

            client.auth.signInWith(Email) {
                this.email = cleanEmail
                this.password = password
            }

            val currentUser =
                client.auth.currentUserOrNull()
                    ?: return AuthResult.Error(
                        "Unable to load the authenticated user."
                    )

            resolveAccountAccess(
                userId = currentUser.id,
                requiredRole = requiredRole
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AuthResult.Error(
                message = exception.message
                    ?: "Sign in failed. Check your credentials."
            )
        }
    }

    suspend fun restoreSession(
        requiredRole: AppRole
    ): AuthResult {
        return try {
            client.auth.awaitInitialization()

            val currentUser =
                client.auth.currentUserOrNull()
                    ?: return AuthResult.SignedOut

            resolveAccountAccess(
                userId = currentUser.id,
                requiredRole = requiredRole
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AuthResult.Error(
                message = exception.message
                    ?: "Unable to restore your session."
            )
        }
    }

    suspend fun requestPasswordReset(
        email: String,
        redirectUrl: String
    ): AuthResult {
        val cleanEmail = email.trim().lowercase()
        val cleanRedirectUrl = redirectUrl.trim()

        if (
            cleanEmail.isBlank() ||
            !cleanEmail.contains("@")
        ) {
            return AuthResult.Error(
                "Please enter a valid email address."
            )
        }

        if (cleanRedirectUrl.isBlank()) {
            return AuthResult.Error(
                "Password recovery address is missing."
            )
        }

        return try {
            client.auth.awaitInitialization()

            client.auth.resetPasswordForEmail(
                email = cleanEmail,
                redirectUrl = cleanRedirectUrl
            )

            AuthResult.PasswordResetEmailSent
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AuthResult.Error(
                message = exception.message
                    ?: "Unable to send the password reset email."
            )
        }
    }

    suspend fun updatePassword(
        newPassword: String
    ): AuthResult {
        if (newPassword.length < 8) {
            return AuthResult.Error(
                "Password must contain at least 8 characters."
            )
        }

        return try {
            client.auth.awaitInitialization()

            if (client.auth.currentSessionOrNull() == null) {
                return AuthResult.Error(
                    "Your password recovery session has expired."
                )
            }

            client.auth.updateUser {
                password = newPassword
            }

            AuthResult.PasswordUpdated
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AuthResult.Error(
                message = exception.message
                    ?: "Unable to update your password."
            )
        }
    }

    suspend fun getCurrentProfile(): Profile? {
        client.auth.awaitInitialization()

        val userId =
            client.auth.currentUserOrNull()?.id
                ?: return null

        return loadProfile(userId)
    }

    suspend fun signOut(): AuthResult {
        return try {
            client.auth.signOut()
            AuthResult.SignedOut
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AuthResult.Error(
                message = exception.message
                    ?: "Sign out failed. Please try again."
            )
        }
    }

    private suspend fun resolveAccountAccess(
        userId: String,
        requiredRole: AppRole
    ): AuthResult {
        val profile = loadProfile(userId)

        if (profile.role != requiredRole) {
            clearCurrentSession()

            return AuthResult.WrongApplication(
                requiredRole = requiredRole,
                actualRole = profile.role
            )
        }

        return when (profile.accountStatus) {
            AccountStatus.ACTIVE -> {
                AuthResult.Authenticated(profile)
            }

            AccountStatus.PENDING -> {
                AuthResult.ProviderApprovalPending
            }

            AccountStatus.SUSPENDED, AccountStatus.DELETED -> {
                clearCurrentSession()
                AuthResult.AccountSuspended
            }

            AccountStatus.REJECTED -> {
                clearCurrentSession()
                AuthResult.AccountRejected
            }
        }
    }

    private suspend fun loadProfile(
        userId: String
    ): Profile {
        return client
            .from("profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingle()
    }

    private suspend fun clearCurrentSession() {
        try {
            client.auth.signOut()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Database RLS still protects restricted data.
        }
    }

    private fun validateRegistration(
        fullName: String,
        email: String,
        password: String,
        role: AppRole
    ): String? {
        if (role == AppRole.ADMIN) {
            return "Admin accounts cannot be created from a mobile app."
        }

        if (fullName.length < 2) {
            return "Please enter your full name."
        }

        if (
            email.isBlank() ||
            !email.contains("@") ||
            !email.substringAfter("@").contains(".")
        ) {
            return "Please enter a valid email address."
        }

        if (password.length < 8) {
            return "Password must contain at least 8 characters."
        }

        return null
    }

    private fun AppRole.databaseValue(): String {
        return when (this) {
            AppRole.CUSTOMER -> "customer"
            AppRole.PROVIDER -> "provider"
            AppRole.ADMIN -> "admin"
        }
    }
}
