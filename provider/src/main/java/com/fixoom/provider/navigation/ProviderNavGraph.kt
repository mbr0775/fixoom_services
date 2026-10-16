package com.fixoom.provider.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fixoom.core.model.AuthResult
import com.fixoom.provider.ui.auth.ProviderAuthViewModel
import com.fixoom.provider.ui.components.rememberProviderMotionAllowed
import com.fixoom.provider.ui.screens.ProviderWorkspaceScreen
import com.fixoom.provider.ui.screens.auth.ProviderEmailVerificationScreen
import com.fixoom.provider.ui.screens.auth.ProviderForgotPasswordScreen
import com.fixoom.provider.ui.screens.auth.ProviderLoginScreen
import com.fixoom.provider.ui.screens.auth.ProviderRegisterScreen
import com.fixoom.provider.ui.screens.auth.ProviderRejectedScreen
import com.fixoom.provider.ui.screens.auth.ProviderResetPasswordScreen
import com.fixoom.provider.ui.screens.auth.ProviderSplashScreen
import com.fixoom.provider.ui.screens.auth.ProviderSuspendedScreen
import com.fixoom.provider.ui.screens.auth.ProviderWelcomeScreen

@Composable
fun ProviderNavGraph(
    passwordRecoveryLinkOpened: Boolean,
    onPasswordRecoveryLinkConsumed: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onContactSupport: () -> Unit,
    navController: NavHostController = rememberNavController(),
    authViewModel: ProviderAuthViewModel = viewModel(),
    notificationBooking: String? = null,
    onNotificationConsumed: () -> Unit = {},
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    val motionAllowed = rememberProviderMotionAllowed()

    var recoveryMode by rememberSaveable {
        mutableStateOf(passwordRecoveryLinkOpened)
    }

    LaunchedEffect(passwordRecoveryLinkOpened) {
        if (passwordRecoveryLinkOpened) {
            recoveryMode = true
            authViewModel.clearResult()

            navController.navigateAndClear(ProviderScreen.ResetPassword.route)

            onPasswordRecoveryLinkConsumed()
        }
    }

    LaunchedEffect(
        uiState.result,
        recoveryMode,
        passwordRecoveryLinkOpened,
    ) {
        if (recoveryMode || passwordRecoveryLinkOpened) {
            return@LaunchedEffect
        }

        when (val result = uiState.result) {
            is AuthResult.Authenticated -> {
                navController.navigateAndClear(ProviderScreen.Dashboard.route)
            }

            AuthResult.SignedOut -> {
                navController.navigateAndClear(ProviderScreen.Welcome.route)
            }

            AuthResult.ReturnToSignIn -> {
                navController.navigateAndClear(ProviderScreen.Login.route)
            }

            AuthResult.EmailVerificationRequired -> {
                navController.navigateAndClear(ProviderScreen.EmailVerification.route)
            }

            AuthResult.ProviderApprovalPending -> {
                navController.navigateAndClear(ProviderScreen.PendingApproval.route)
            }

            AuthResult.AccountSuspended -> {
                navController.navigateAndClear(ProviderScreen.AccountSuspended.route)
            }

            AuthResult.AccountRejected -> {
                navController.navigateAndClear(ProviderScreen.AccountRejected.route)
            }

            is AuthResult.WrongApplication -> {
                if (navController.currentDestination?.route != ProviderScreen.Login.route) {
                    navController.navigateAndClear(ProviderScreen.Login.route)
                }
            }

            is AuthResult.Error -> {
                if (navController.currentDestination?.route == ProviderScreen.Splash.route) {
                    navController.navigateAndClear(ProviderScreen.Welcome.route)
                }
            }

            AuthResult.PasswordResetEmailSent,
            AuthResult.PasswordUpdated,
            null -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = ProviderScreen.Splash.route,
        enterTransition = {
            if (motionAllowed) fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 }
            else EnterTransition.None
        },
        exitTransition = { if (motionAllowed) fadeOut(tween(140)) else ExitTransition.None },
        popEnterTransition = {
            if (motionAllowed) fadeIn(tween(180)) + slideInHorizontally(tween(220)) { -it / 12 }
            else EnterTransition.None
        },
        popExitTransition = {
            if (motionAllowed) fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { it / 12 }
            else ExitTransition.None
        },
    ) {
        composable(route = ProviderScreen.Splash.route) {
            ProviderSplashScreen()
        }

        composable(route = ProviderScreen.Welcome.route) {
            ProviderWelcomeScreen(
                onCreateAccount = {
                    authViewModel.clearResult()

                    navController.navigate(ProviderScreen.Register.route)
                },
                onSignIn = {
                    authViewModel.clearResult()

                    navController.navigate(ProviderScreen.Login.route)
                },
            )
        }

        composable(route = ProviderScreen.Login.route) {
            ProviderLoginScreen(
                uiState = uiState,
                onSignIn = authViewModel::signIn,
                onCreateAccount = {
                    authViewModel.clearResult()

                    navController.navigate(ProviderScreen.Register.route)
                },
                onForgotPassword = {
                    authViewModel.clearResult()

                    navController.navigate(ProviderScreen.ForgotPassword.route)
                },
                onBack = {
                    authViewModel.clearResult()

                    if (!navController.popBackStack()) {
                        navController.navigateAndClear(ProviderScreen.Welcome.route)
                    }
                },
            )
        }

        composable(route = ProviderScreen.Register.route) {
            val categories by authViewModel.categories.collectAsStateWithLifecycle()
            val categoryError by authViewModel.categoryError.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { authViewModel.loadCategories() }
            ProviderRegisterScreen(
                uiState = uiState,
                onRegister = authViewModel::register,
                categories = categories,
                categoryError = categoryError,
                onRetryCategories = authViewModel::loadCategories,
                onSignIn = {
                    authViewModel.clearResult()

                    navController.navigateAndClear(ProviderScreen.Login.route)
                },
                onOpenTerms = onOpenTerms,
                onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                onBack = {
                    authViewModel.clearResult()

                    if (!navController.popBackStack()) {
                        navController.navigateAndClear(ProviderScreen.Welcome.route)
                    }
                },
            )
        }

        composable(route = ProviderScreen.ForgotPassword.route) {
            ProviderForgotPasswordScreen(
                uiState = uiState,
                onRequestReset = authViewModel::requestPasswordReset,
                onBackToSignIn = {
                    authViewModel.clearResult()

                    navController.navigateAndClear(ProviderScreen.Login.route)
                },
                onBack = {
                    authViewModel.clearResult()
                    navController.popBackStack()
                },
            )
        }

        composable(route = ProviderScreen.ResetPassword.route) {
            ProviderResetPasswordScreen(
                uiState = uiState,
                onUpdatePassword = authViewModel::updatePassword,
                onBackToSignIn = {
                    recoveryMode = false
                    authViewModel.clearResult()

                    navController.navigateAndClear(ProviderScreen.Login.route)
                },
            )
        }

        composable(route = ProviderScreen.EmailVerification.route) {
            ProviderEmailVerificationScreen(
                onBackToSignIn = {
                    authViewModel.clearResult()

                    navController.navigateAndClear(ProviderScreen.Login.route)
                }
            )
        }

        composable(route = ProviderScreen.PendingApproval.route) {
            ProviderWorkspaceScreen(authViewModel, pending = true)
        }

        composable(route = ProviderScreen.AccountSuspended.route) {
            ProviderSuspendedScreen(
                onContactSupport = onContactSupport,
                onBackToSignIn = {
                    authViewModel.clearResult()

                    navController.navigateAndClear(ProviderScreen.Login.route)
                },
            )
        }

        composable(route = ProviderScreen.AccountRejected.route) {
            ProviderRejectedScreen(
                onContactSupport = onContactSupport,
                onBackToSignIn = {
                    authViewModel.clearResult()

                    navController.navigateAndClear(ProviderScreen.Login.route)
                },
            )
        }

        composable(route = ProviderScreen.Dashboard.route) {
            val authenticated = uiState.result as? AuthResult.Authenticated

            if (authenticated != null) {
                ProviderWorkspaceScreen(
                    authViewModel,
                    notificationBooking = notificationBooking,
                    onNotificationConsumed = onNotificationConsumed,
                )
            } else {
                ProviderSplashScreen()

                LaunchedEffect(Unit) {
                    authViewModel.restoreSession()
                }
            }
        }
    }
}

private fun NavHostController.navigateAndClear(route: String) {
    navigate(route) {
        popUpTo(graph.id) {
            inclusive = true
        }

        launchSingleTop = true
    }
}
