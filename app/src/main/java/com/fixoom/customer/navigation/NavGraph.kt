package com.fixoom.customer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fixoom.core.data.repository.MarketplaceViewModel
import com.fixoom.customer.ui.screens.ExploreScreen
import com.fixoom.customer.ui.screens.HomeScreen
import com.fixoom.customer.ui.screens.ProfileScreen
import com.fixoom.marketplace.ui.*

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
) {
    val market: MarketplaceViewModel = viewModel()
    val state by market.state.collectAsStateWithLifecycle()
    ObserveMarketplace(market)
    val openBooking: (String) -> Unit = {
        navController.navigate(Screen.BookingDetails.createRoute(it))
    }
    val openAccount: () -> Unit = { navController.navigate("account") }
    val back: () -> Unit = { navController.popBackStack() }
    NavHost(navController, Screen.Home.route, modifier) {
        composable(Screen.Home.route) {
            HomeScreen(
                onServiceClick = { navController.navigate(Screen.ProviderList.createRoute(it)) },
                onNotificationsClick = { navController.navigate(Screen.Alerts.route) },
                categories = state.data.categories,
                professionals = state.data.professionals,
                onProviderClick = {
                    navController.navigate(Screen.ProviderDetails.createRoute(it))
                },
                loading = state.loading,
                dataError = state.error,
                onRefresh = market::refresh,
            )
        }
        composable(Screen.Explore.route) {
            ExploreScreen(
                onServiceClick = { navController.navigate(Screen.ProviderList.createRoute(it)) },
                categories = state.data.categories,
                professionals = state.data.professionals,
                loading = state.loading,
                dataError = state.error,
                onRefresh = market::refresh,
            )
        }
        composable(Screen.Bookings.route) {
            ConnectedBookings(market, onOpen = openBooking, onAccount = openAccount)
        }
        composable(Screen.Messages.route) {
            ConnectedBookings(
                market,
                mode = "messages",
                onOpen = openBooking,
                onAccount = openAccount,
            )
        }
        composable(Screen.Alerts.route) {
            ConnectedBookings(
                market,
                mode = "activity",
                onOpen = openBooking,
                onAccount = openAccount,
                onBack = back,
            )
        }
        composable("account") { ConnectedAccount(market, back) }
        composable(Screen.Profile.route) {
            ProfileScreen(
                isDarkMode,
                onDarkModeChange,
                { navController.navigate(Screen.Bookings.route) },
                { navController.navigate(Screen.Explore.route) },
                onManageAccount = openAccount,
            )
        }
        composable(
            Screen.ProviderList.route,
            arguments =
                listOf(navArgument(Screen.ProviderList.ARG_CATEGORY) { type = NavType.StringType }),
        ) { entry ->
            ConnectedProviderList(
                entry.arguments?.getString(Screen.ProviderList.ARG_CATEGORY).orEmpty(),
                market,
                { navController.navigate(Screen.ProviderDetails.createRoute(it)) },
                back,
            )
        }
        composable(
            Screen.ProviderDetails.route,
            arguments =
                listOf(
                    navArgument(Screen.ProviderDetails.ARG_PROVIDER_ID) {
                        type = NavType.StringType
                    }
                ),
        ) { entry ->
            ConnectedProviderDetails(
                entry.arguments?.getString(Screen.ProviderDetails.ARG_PROVIDER_ID).orEmpty(),
                market,
                back,
            ) {
                navController.navigate(Screen.ServiceBooking.createRoute(it))
            }
        }
        composable(
            Screen.ServiceBooking.route,
            arguments =
                listOf(
                    navArgument(Screen.ServiceBooking.ARG_PROVIDER_ID) { type = NavType.StringType }
                ),
        ) { entry ->
            ConnectedBookingForm(
                entry.arguments?.getString(Screen.ServiceBooking.ARG_PROVIDER_ID).orEmpty(),
                market,
                back,
                openAccount,
            ) { id ->
                if (navController.currentDestination?.route == Screen.ServiceBooking.route)
                    navController.navigate(Screen.BookingConfirmation.createRoute(id)) {
                        popUpTo(Screen.ServiceBooking.route) { inclusive = true }
                    }
            }
        }
        composable(
            Screen.BookingConfirmation.route,
            arguments =
                listOf(
                    navArgument(Screen.BookingConfirmation.ARG_BOOKING_ID) {
                        type = NavType.StringType
                    }
                ),
        ) { entry ->
            ConnectedBookingDetails(
                entry.arguments?.getString(Screen.BookingConfirmation.ARG_BOOKING_ID).orEmpty(),
                market,
                onBack = {
                    navController.navigate(Screen.Bookings.route) {
                        popUpTo(Screen.Home.route)
                        launchSingleTop = true
                    }
                },
                confirmation = true,
            )
        }
        composable(
            Screen.BookingDetails.route,
            arguments =
                listOf(
                    navArgument(Screen.BookingDetails.ARG_BOOKING_ID) { type = NavType.StringType }
                ),
        ) { entry ->
            ConnectedBookingDetails(
                entry.arguments?.getString(Screen.BookingDetails.ARG_BOOKING_ID).orEmpty(),
                market,
                onBack = back,
            )
        }
    }
}
