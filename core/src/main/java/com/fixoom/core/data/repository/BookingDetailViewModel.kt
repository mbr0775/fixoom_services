package com.fixoom.core.data.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixoom.core.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout

data class BookingDetailState(
    val booking: ServiceBooking? = null,
    val events: List<BookingEvent> = emptyList(),
    val messages: List<BookingMessage> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val location: JobLocation? = null,
    val historyError: String? = null,
    val messagesError: String? = null,
    val locationError: String? = null,
    val extrasLoading: Boolean = false,
)

class BookingDetailViewModel(
    private val repository: MarketplaceRepository = MarketplaceRepository()
) : ViewModel() {
    private val mutable = MutableStateFlow(BookingDetailState())
    val state = mutable.asStateFlow()
    private var id: String? = null
    private var request: Job? = null
    private var refreshAgain = false
    private var generation = 0

    fun open(value: String) {
        if (id != value) {
            request?.cancel()
            request = null
            generation++
            refreshAgain = false
            mutable.value = BookingDetailState()
        }
        id = value
        refresh()
    }

    fun refresh() {
        val key = id ?: return
        if (request?.isActive == true) {
            refreshAgain = true
            return
        }
        val version = generation
        request = viewModelScope.launch {
            do {
                refreshAgain = false
                load(key, version)
            } while (refreshAgain && version == generation)
        }
    }

    private suspend fun load(key: String, version: Int) {
        if (version != generation) return
        mutable.value =
            mutable.value.copy(
                loading = mutable.value.booking == null,
                error = null,
                extrasLoading = true,
            )
        try {
            val next = supervisorScope {
                val booking = async { withTimeout(15_000) { repository.booking(key) } }
                val events = async { section { repository.events(key) } }
                val messages = async { section { repository.messages(key) } }
                val location = async { section { repository.jobLocation(key) } }
                val saved = booking.await()
                if (saved == null) {
                    events.cancel()
                    messages.cancel()
                    location.cancel()
                    return@supervisorScope BookingDetailState(loading = false)
                }
                // A secondary read must never discard a successfully loaded booking.
                if (version == generation)
                    mutable.value =
                        mutable.value.copy(booking = saved, loading = false, error = null)
                val history = events.await()
                val conversation = messages.await()
                val position = location.await()
                BookingDetailState(
                    booking = saved,
                    events = history.getOrElse { mutable.value.events },
                    messages = conversation.getOrElse { mutable.value.messages },
                    loading = false,
                    // Never present an unrefreshed location as a current position.
                    location = position.getOrNull(),
                    historyError =
                        if (history.isFailure)
                            "Booking history could not be refreshed. Refresh to retry; any entries shown are from the last successful load."
                        else null,
                    messagesError =
                        if (conversation.isFailure)
                            "Messages could not be refreshed. Refresh to retry; any messages shown are from the last successful load."
                        else null,
                    locationError =
                        if (position.isFailure)
                            "Live location could not be refreshed. Refresh to retry."
                        else null,
                )
            }
            if (version == generation) mutable.value = next
        } catch (cancelled: CancellationException) {
            if (cancelled !is kotlinx.coroutines.TimeoutCancellationException) throw cancelled
            if (version == generation)
                mutable.value =
                    mutable.value.copy(
                        loading = false,
                        error = "Connection timed out. Please refresh.",
                        location = null,
                        extrasLoading = false,
                    )
        } catch (error: Exception) {
            if (version == generation)
                mutable.value =
                    mutable.value.copy(
                        loading = false,
                        error = bookingReadError(error),
                        location = null,
                        extrasLoading = false,
                    )
        }
    }

    private suspend fun <T> section(block: suspend () -> T): Result<T> =
        try {
            Result.success(withTimeout(15_000) { block() })
        } catch (error: CancellationException) {
            if (error !is kotlinx.coroutines.TimeoutCancellationException) throw error
            Result.failure(error)
        } catch (error: Exception) {
            Result.failure(error)
        }
}
