package com.fixoom.core.data.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixoom.core.model.BusyTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BookingAvailabilityState(
    val day: String? = null,
    val times: List<BusyTime> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

class BookingAvailabilityViewModel : ViewModel() {
    private val repository = MarketplaceRepository()
    private val mutable = MutableStateFlow(BookingAvailabilityState())
    val state = mutable.asStateFlow()
    private var request: Job? = null

    fun load(provider: String, day: String) {
        request?.cancel()
        mutable.value = BookingAvailabilityState(day = day, loading = true)
        request = viewModelScope.launch {
            try {
                val times = withTimeout(10_000) { repository.busyTimes(provider, day) }
                times.forEach { require(java.time.Instant.parse(it.endsAt).isAfter(java.time.Instant.parse(it.startsAt))) }
                mutable.value = BookingAvailabilityState(day, times)
            } catch (e: CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                mutable.value =
                    BookingAvailabilityState(
                        day,
                        error = "Availability check timed out. Please retry.",
                    )
            } catch (_: Exception) {
                mutable.value =
                    BookingAvailabilityState(
                        day,
                        error = "Unable to check this provider's schedule. Please retry.",
                    )
            }
        }
    }
}
