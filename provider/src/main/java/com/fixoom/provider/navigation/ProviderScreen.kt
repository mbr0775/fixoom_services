package com.fixoom.provider.navigation

sealed class ProviderScreen(
    val route: String
) {

    data object Splash : ProviderScreen(
        route = "provider_splash"
    )

    data object Welcome : ProviderScreen(
        route = "provider_welcome"
    )

    data object Login : ProviderScreen(
        route = "provider_login"
    )

    data object Register : ProviderScreen(
        route = "provider_register"
    )

    data object ForgotPassword : ProviderScreen(
        route = "provider_forgot_password"
    )

    data object ResetPassword : ProviderScreen(
        route = "provider_reset_password"
    )

    data object EmailVerification : ProviderScreen(
        route = "provider_email_verification"
    )

    data object PendingApproval : ProviderScreen(
        route = "provider_pending_approval"
    )

    data object Dashboard : ProviderScreen(
        route = "provider_dashboard"
    )

    data object AccountSuspended : ProviderScreen(
        route = "provider_account_suspended"
    )

    data object AccountRejected : ProviderScreen(
        route = "provider_account_rejected"
    )
}