package com.fixoom.core.data.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixoom.core.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class BookingDetailState(
    val booking: ServiceBooking? = null,
    val events: List<BookingEvent> = emptyList(),
    val messages: List<BookingMessage> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val location: JobLocation? = null,
)

class BookingDetailViewModel : ViewModel() {
    private val repository = MarketplaceRepository()
    private val mutable = MutableStateFlow(BookingDetailState())
    val state = mutable.asStateFlow()
    private var id: String? = null
    private var loading = false
    private var request: Job? = null

    fun open(value: String) {
        if (id != value) {
            request?.cancel()
            loading = false
            mutable.value = BookingDetailState()
        }
        id = value
        refresh()
    }

    fun refresh() {
        val key = id ?: return
        if (loading) return
        loading = true
        request = viewModelScope.launch {
            try {
                val next =
                    withTimeout(15_000) {
                        coroutineScope {
                            val booking = async { repository.booking(key) }
                            val events = async { repository.events(key) }
                            val messages = async { repository.messages(key) }
                            val location = async { repository.jobLocation(key) }
                            BookingDetailState(
                                booking.await(),
                                events.await(),
                                messages.await(),
                                false,
                                location = location.await(),
                            )
                        }
                    }
                if (id == key) mutable.value = next
            } catch (cancelled: CancellationException) {
                if (cancelled !is kotlinx.coroutines.TimeoutCancellationException) throw cancelled
                if (id == key)
                    mutable.value =
                        mutable.value.copy(
                            loading = false,
                            error = "Connection timed out. Please refresh.",
                        )
            } catch (_: Exception) {
                if (id == key)
                    mutable.value =
                        mutable.value.copy(
                            loading = false,
                            error =
                                "Unable to refresh this booking. Check your connection and account access.",
                        )
            } finally {
                if (id == key) loading = false
            }
        }
    }
}
