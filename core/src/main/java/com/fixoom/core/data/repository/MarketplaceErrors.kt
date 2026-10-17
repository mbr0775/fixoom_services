package com.fixoom.core.data.repository

import io.github.jan.supabase.exceptions.RestException

fun bookingReadError(error: Exception): String =
    when ((error as? RestException)?.statusCode) {
        401 -> "Your session has expired. Sign in again to refresh this booking."
        403 ->
            "Your account cannot access this booking. Sign in with its customer or assigned provider account."
        else ->
            "This booking could not be refreshed. Any details shown are from the last successful load. Tap Refresh to retry."
    }

/** Allowlisted explanations only. Never render raw SDK messages, headers or database details. */
fun marketplaceActionError(error: Exception): String {
    val remote = error as? RestException
    return when (remote?.error?.trim()) {
        "This provider already has an accepted job during that time" ->
            "This provider is already booked for that time. Choose another time and try again."
        "This provider is not available for this service",
        "This service is not approved for this provider" ->
            "This provider is no longer available for the selected service. Refresh and choose an available service."
        "Choose a future service time" -> "Choose a future date and time for this booking."
        "This request key was used for a different booking" ->
            "This request was already submitted with different details. Open Your bookings to check it before starting a new request."
        "An active customer account is required" ->
            "Sign in with an active customer account to book this service."
        else ->
            if (remote?.statusCode == 401) "Your session has expired. Sign in again and retry."
            else if (remote?.statusCode == 403)
                "Your account cannot make this change. Check your account status and refresh."
            else
                "The change could not be saved. Refresh to check its current status, then try again."
    }
}
