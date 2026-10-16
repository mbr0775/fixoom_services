package com.fixoom.marketplace.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.fixoom.core.data.repository.BookingAvailabilityState
import com.fixoom.core.model.*

@Composable
fun BookingAvailabilityPanel(
    state: BookingAvailabilityState,
    conflict: Boolean,
    onRefresh: () -> Unit,
    onNextTime: () -> Unit,
) {
    val error = state.error
    InfoCard(
        "Provider's schedule",
        "Confirmed reservations for your selected date, in Sri Lanka time.",
    ) {
        when {
            state.loading || state.day == null -> Text("Checking availability…")
            error != null -> {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onRefresh) { Text("Retry availability check") }
            }
            else -> {
                if (state.times.isEmpty()) Text("No confirmed reservations on this date yet.")
                else
                    state.times.forEach {
                        Text("Reserved: ${serviceTime(it.startsAt)} – ${serviceTime(it.endsAt)}")
                    }
                if (conflict) {
                    Text(
                        "This time overlaps a confirmed job. Choose another time.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    FilledTonalButton(onNextTime) { Text("Use next unreserved time") }
                }
                Text(
                    "Availability can change until the provider accepts your request.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onRefresh) { Text("Refresh availability") }
            }
        }
    }
}
