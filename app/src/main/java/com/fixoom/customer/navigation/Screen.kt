package com.fixoom.customer.navigation

import android.net.Uri

sealed class Screen(
    val route: String
) {
    object Home : Screen(
        route = "home"
    )

    object Explore : Screen(
        route = "explore"
    )

    object Bookings : Screen(
        route = "bookings"
    )

    object Messages : Screen(
        route = "messages"
    )

    object Alerts : Screen(
        route = "alerts"
    )

    object Profile : Screen(
        route = "profile"
    )

    object ProviderList : Screen(
        route = "providers/{category}"
    ) {
        const val ARG_CATEGORY = "category"

        fun createRoute(
            category: String
        ): String {
            return "providers/${
                Uri.encode(category.trim())
            }"
        }
    }

    object ProviderDetails : Screen(
        route = "provider/{providerId}"
    ) {
        const val ARG_PROVIDER_ID = "providerId"

        fun createRoute(
            providerId: Int
        ): String {
            return "provider/$providerId"
        }
    }

    object ServiceBooking : Screen(
        route = "booking/{providerId}"
    ) {
        const val ARG_PROVIDER_ID = "providerId"

        fun createRoute(
            providerId: Int
        ): String {
            return "booking/$providerId"
        }
    }

    object BookingConfirmation : Screen(
        route = "booking_confirmation/{bookingId}"
    ) {
        const val ARG_BOOKING_ID = "bookingId"

        fun createRoute(
            bookingId: Long
        ): String {
            return "booking_confirmation/$bookingId"
        }
    }

    object BookingDetails : Screen(
        route = "booking_details/{bookingId}"
    ) {
        const val ARG_BOOKING_ID = "bookingId"

        fun createRoute(
            bookingId: Long
        ): String {
            return "booking_details/$bookingId"
        }
    }
}