package com.fixoom.core.data.repository

import com.fixoom.core.data.remote.FixoomSupabase
import com.fixoom.core.model.AccountStatus
import com.fixoom.core.model.AppRole
import com.fixoom.core.model.AuthResult
import com.fixoom.core.model.ContactPhone
import com.fixoom.core.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository(private val client: SupabaseClient = FixoomSupabase.client) {

    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        role: AppRole,
        skills: List<String> = emptyList(),
    ): AuthResult {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanPhone = ContactPhone.normalize(phone)
        if (phone.isNotBlank() && cleanPhone == null)
            return AuthResult.Error(
                "Enter a valid phone number, such as 0771234567 or +94771234567."
            )

        validateRegistration(
                fullName = cleanName,
                email = cleanEmail,
                password = password,
                role = role,
            )
            ?.let { error ->
                return AuthResult.Error(error)
            }

        return try {
            client.auth.awaitInitialization()

            // A confirmation-only signup does not replace an existing SDK session.
            // Never resolve the new account through a stale, unrelated identity.
            client.auth.clearSession()
            val registeredUser =
                client.auth.signUpWith(
                    Email,
                    redirectUrl =
                        if (role == AppRole.PROVIDER) "fixoom-provider://auth/confirmed"
                        else "fixoom-customer://auth/confirmed",
                ) {
                    this.email = cleanEmail
                    this.password = password

                    data = buildJsonObject {
                        put("full_name", cleanName)
                        put("role", role.databaseValue())
                        if (role == AppRole.PROVIDER)
                            put(
                                "skill_categories",
                                JsonArray(skills.distinct().take(12).map(::JsonPrimitive)),
                            )

                        if (cleanPhone != null) {
                            put("phone", cleanPhone)
                        }
                    }
                }

            val currentUser = client.auth.currentUserOrNull()

            if (
                currentUser == null ||
                    !currentUser.email.equals(cleanEmail, ignoreCase = true) ||
                    (registeredUser != null && registeredUser.id != currentUser.id)
            ) {
                client.auth.clearSession()
                AuthResult.EmailVerificationRequired
            } else {
                resolveAccountAccess(
                    userId = currentUser.id,
                    requiredRole = role,
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AuthResult.Error(
                message =
                    authErrorMessage(
                        exception,
                        "Registration could not be completed. Check your connection and try again. If you already registered, verify your email and sign in.",
                    )
            )
        }
    }

    suspend fun signIn(
        email: String,
        password: String,
        requiredRole: AppRole,
    ): AuthResult {
        val cleanEmail = email.trim().lowercase()

        if (cleanEmail.isBlank()) {
            return AuthResult.Error("Email address is required.")
        }

        if (password.isBlank()) {
            return AuthResult.Error("Password is required.")
        }

        return try {
            client.auth.awaitInitialization()

            client.auth.signInWith(Email) {
                this.email = cleanEmail
                this.password = password
            }

            val currentUser =
                client.auth.currentUserOrNull()
                    ?: return AuthResult.Error("Unable to load the authenticated user.")

            resolveAccountAccess(
                userId = currentUser.id,
                requiredRole = requiredRole,
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            if (
                exception is AuthRestException &&
                    exception.errorCode == AuthErrorCode.EmailNotConfirmed
            )
                AuthResult.EmailVerificationRequired
            else
                AuthResult.Error(
                    authErrorMessage(
                        exception,
                        "Sign in failed. Check your email, password and connection, then try again.",
                    )
                )
        }
    }

    suspend fun restoreSession(requiredRole: AppRole): AuthResult {
        return try {
            client.auth.awaitInitialization()

            val currentUser = client.auth.currentUserOrNull() ?: return AuthResult.SignedOut

            resolveAccountAccess(
                userId = currentUser.id,
                requiredRole = requiredRole,
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AuthResult.Error(
                message =
                    authErrorMessage(
                        exception,
                        "Unable to restore your session. Check your connection and sign in again.",
                    )
            )
        }
    }

    suspend fun requestPasswordReset(
        email: String,
        redirectUrl: String,
    ): AuthResult {
        val cleanEmail = email.trim().lowercase()
        val cleanRedirectUrl = redirectUrl.trim()

        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return AuthResult.Error("Please enter a valid email address.")
        }

        if (cleanRedirectUrl.isBlank()) {
            return AuthResult.Error("Password recovery address is missing.")
        }

        return try {
            client.auth.awaitInitialization()

            client.auth.resetPasswordForEmail(
                email = cleanEmail,
                redirectUrl = cleanRedirectUrl,
            )

            AuthResult.PasswordResetEmailSent
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AuthResult.Error(
                message =
                    authErrorMessage(
                        exception,
                        "Unable to send the password reset email. Check your connection and try again.",
                    )
            )
        }
    }

    suspend fun updatePassword(newPassword: String): AuthResult {
        if (newPassword.length < 8) {
            return AuthResult.Error("Password must contain at least 8 characters.")
        }

        return try {
            client.auth.awaitInitialization()

            if (client.auth.currentSessionOrNull() == null) {
                return AuthResult.Error("Your password recovery session has expired.")
            }

            client.auth.updateUser {
                password = newPassword
            }

            AuthResult.PasswordUpdated
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AuthResult.Error(
                message =
                    authErrorMessage(
                        exception,
                        "Unable to update your password. Check your connection or request a new recovery link.",
                    )
            )
        }
    }

    suspend fun getCurrentProfile(): Profile? {
        client.auth.awaitInitialization()

        val userId = client.auth.currentUserOrNull()?.id ?: return null

        return loadProfile(userId)
    }

    suspend fun signOut(): AuthResult {
        return try {
            com.fixoom.core.data.notifications.PushMessaging.beforeSignOut()
            client.auth.signOut()
            AuthResult.SignedOut
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AuthResult.Error(
                message =
                    authErrorMessage(
                        exception,
                        "Sign out failed. Check your connection and try again.",
                    )
            )
        }
    }

    /** Confirmation links return to login; they do not bypass an explicit sign-in. */
    suspend fun returnToSignIn(): AuthResult {
        client.auth.awaitInitialization()
        client.auth.clearSession()
        return AuthResult.ReturnToSignIn
    }

    private suspend fun resolveAccountAccess(
        userId: String,
        requiredRole: AppRole,
    ): AuthResult {
        val profile =
            loadProfile(userId)
                ?: run {
                    client.auth.clearSession()
                    return AuthResult.Error(
                        "Your account profile is unavailable. Please sign in again. If this continues, contact fixoomservices@gmail.com."
                    )
                }

        if (profile.role != requiredRole) {
            clearCurrentSession()

            return AuthResult.WrongApplication(
                requiredRole = requiredRole,
                actualRole = profile.role,
            )
        }

        return when (profile.accountStatus) {
            AccountStatus.ACTIVE -> {
                AuthResult.Authenticated(profile)
            }

            AccountStatus.PENDING -> {
                AuthResult.ProviderApprovalPending
            }

            AccountStatus.SUSPENDED,
            AccountStatus.DELETED -> {
                clearCurrentSession()
                AuthResult.AccountSuspended
            }

            AccountStatus.REJECTED -> {
                clearCurrentSession()
                AuthResult.AccountRejected
            }
        }
    }

    private suspend fun loadProfile(userId: String): Profile? {
        return client
            .from("profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeList<Profile>()
            .singleOrNull()
    }

    private fun authErrorMessage(exception: Exception, fallback: String): String {
        // SDK exception text can contain internal responses and request details.
        return when ((exception as? AuthRestException)?.errorCode) {
            AuthErrorCode.InvalidCredentials ->
                "The email or password is incorrect. Please try again."
            AuthErrorCode.EmailNotConfirmed ->
                "Verify your email before signing in. Check your inbox and spam folder."
            AuthErrorCode.EmailExists,
            AuthErrorCode.UserAlreadyExists ->
                "Use Sign in if you already registered, or choose a different email address."
            AuthErrorCode.WeakPassword -> "Choose a stronger password with at least 8 characters."
            AuthErrorCode.OverRequestRateLimit,
            AuthErrorCode.OverEmailSendRateLimit ->
                "Too many requests. Please wait a few minutes before trying again."
            AuthErrorCode.SignupDisabled ->
                "New registrations are currently unavailable. Contact fixoomservices@gmail.com."
            else -> fallback
        }
    }

    private suspend fun clearCurrentSession() {
        try {
            com.fixoom.core.data.notifications.PushMessaging.beforeSignOut()
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
        role: AppRole,
    ): String? {
        if (role == AppRole.ADMIN) {
            return "Admin accounts cannot be created from a mobile app."
        }

        if (fullName.length < 2) {
            return "Please enter your full name."
        }

        if (email.isBlank() || !email.contains("@") || !email.substringAfter("@").contains(".")) {
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
