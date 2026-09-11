package com.fixoom.customer.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fixoom.customer.data.BookingStore
import com.fixoom.customer.ui.screens.AlertsScreen
import com.fixoom.customer.ui.screens.BookingConfirmationScreen
import com.fixoom.customer.ui.screens.BookingDetailsScreen
import com.fixoom.customer.ui.screens.BookingsScreen
import com.fixoom.customer.ui.screens.ExploreScreen
import com.fixoom.customer.ui.screens.HomeScreen
import com.fixoom.customer.ui.screens.MessagesScreen
import com.fixoom.customer.ui.screens.ProfileScreen
import com.fixoom.customer.ui.screens.ProviderDetailsScreen
import com.fixoom.customer.ui.screens.ProviderListScreen
import com.fixoom.customer.ui.screens.ServiceBookingScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // Home
        composable(
            route = Screen.Home.route
        ) {
            HomeScreen(
                onServiceClick = { category ->
                    navController.navigate(
                        Screen.ProviderList.createRoute(
                            category
                        )
                    )
                },
                onNotificationsClick = {
                    navController.navigate(Screen.Alerts.route)
                }
            )
        }

        // Explore
        composable(
            route = Screen.Explore.route
        ) {
            ExploreScreen(
                onServiceClick = { category ->
                    navController.navigate(
                        Screen.ProviderList.createRoute(
                            category
                        )
                    )
                }
            )
        }

        // Bookings
        composable(
            route = Screen.Bookings.route
        ) {
            BookingsScreen(
                navController = navController
            )
        }

        // Alerts
        composable(
            route = Screen.Alerts.route
        ) {
            AlertsScreen()
        }

        // Profile
        composable(
            route = Screen.Profile.route
        ) {
            ProfileScreen(
                isDarkMode = isDarkMode,
                onDarkModeChange = onDarkModeChange,
                onBookingsClick = {
                    navController.navigate(Screen.Bookings.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onExploreClick = {
                    navController.navigate(Screen.Explore.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // Provider list
        composable(
            route = Screen.ProviderList.route,
            arguments = listOf(
                navArgument(
                    Screen.ProviderList.ARG_CATEGORY
                ) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val encodedCategory =
                backStackEntry.arguments
                    ?.getString(
                        Screen.ProviderList.ARG_CATEGORY
                    )
                    .orEmpty()

            val category =
                Uri.decode(encodedCategory)

            ProviderListScreen(
                category = category,
                onBackClick = {
                    navController.popBackStack()
                },
                onProviderClick = { providerId ->
                    navController.navigate(
                        Screen.ProviderDetails.createRoute(
                            providerId
                        )
                    )
                }
            )
        }

        // Provider details
        composable(
            route = Screen.ProviderDetails.route,
            arguments = listOf(
                navArgument(
                    Screen.ProviderDetails.ARG_PROVIDER_ID
                ) {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val providerId =
                backStackEntry.arguments
                    ?.getInt(
                        Screen.ProviderDetails.ARG_PROVIDER_ID
                    )
                    ?: 0

            ProviderDetailsScreen(
                providerId = providerId,
                onBackClick = {
                    navController.popBackStack()
                },
                onBookNowClick = { selectedProviderId ->
                    navController.navigate(
                        Screen.ServiceBooking.createRoute(
                            selectedProviderId
                        )
                    )
                }
            )
        }

        // Service booking form
        composable(
            route = Screen.ServiceBooking.route,
            arguments = listOf(
                navArgument(
                    Screen.ServiceBooking.ARG_PROVIDER_ID
                ) {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val providerId =
                backStackEntry.arguments
                    ?.getInt(
                        Screen.ServiceBooking.ARG_PROVIDER_ID
                    )
                    ?: 0

            ServiceBookingScreen(
                providerId = providerId,
                onBackClick = {
                    navController.popBackStack()
                },
                onBookingConfirmed = { bookingId ->
                    navController.navigate(
                        Screen.BookingConfirmation.createRoute(
                            bookingId
                        )
                    ) {
                        popUpTo(
                            Screen.ServiceBooking.route
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Booking confirmation
        composable(
            route = Screen.BookingConfirmation.route,
            arguments = listOf(
                navArgument(
                    Screen.BookingConfirmation.ARG_BOOKING_ID
                ) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val bookingId =
                backStackEntry.arguments
                    ?.getLong(
                        Screen.BookingConfirmation.ARG_BOOKING_ID
                    )
                    ?: 0L

            BookingConfirmationScreen(
                bookingId = bookingId,
                onViewBookingsClick = {
                    navController.navigate(
                        Screen.Bookings.route
                    ) {
                        popUpTo(Screen.Home.route)
                        launchSingleTop = true
                    }
                },
                onHomeClick = {
                    navController.navigate(
                        Screen.Home.route
                    ) {
                        popUpTo(Screen.Home.route) {
                            inclusive = false
                        }

                        launchSingleTop = true
                    }
                }
            )
        }

        // Messages
        composable(
            route = Screen.Messages.route
        ) {
            MessagesScreen()
        }

        // Booking details
        composable(
            route = Screen.BookingDetails.route,
            arguments = listOf(
                navArgument(
                    Screen.BookingDetails.ARG_BOOKING_ID
                ) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val bookingId =
                backStackEntry.arguments
                    ?.getLong(
                        Screen.BookingDetails.ARG_BOOKING_ID
                    )
                    ?: 0L

            BookingDetailsScreen(
                bookingId = bookingId,
                onBackClick = {
                    navController.popBackStack()
                },
                onCancelClick = {
                    BookingStore.cancelBooking(bookingId)
                    navController.popBackStack()
                }
            )
        }
    }
}
