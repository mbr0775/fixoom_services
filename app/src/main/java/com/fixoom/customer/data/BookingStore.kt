package com.fixoom.customer.data

import androidx.compose.runtime.mutableStateListOf

enum class BookingStatus {
    UPCOMING,
    COMPLETED,
    CANCELLED
}

enum class BookingAttachmentType {
    IMAGE,
    VIDEO
}

data class BookingAttachment(
    val uri: String,
    val type: BookingAttachmentType,
    val displayName: String
)

data class BookingRecord(
    val id: Long,
    val reference: String,

    val providerId: Int,
    val providerName: String,
    val profession: String,
    val category: String,
    val imageRes: Int,

    val serviceDate: String,
    val serviceTime: String,
    val serviceAddress: String,
    val problemDescription: String,

    val attachments: List<BookingAttachment>,

    val estimatedPrice: String,
    val status: BookingStatus,

    val createdAt: Long
)

object BookingStore {

    private val bookingRecords =
        mutableStateListOf<BookingRecord>()

    val bookings: List<BookingRecord>
        get() = bookingRecords

    fun addBooking(
        providerId: Int,
        providerName: String,
        profession: String,
        category: String,
        imageRes: Int,
        serviceDate: String,
        serviceTime: String,
        serviceAddress: String,
        problemDescription: String,
        estimatedPrice: String,
        attachments: List<BookingAttachment> = emptyList()
    ): BookingRecord {
        val currentTime = System.currentTimeMillis()

        val newBooking = BookingRecord(
            id = currentTime,
            reference = createBookingReference(currentTime),

            providerId = providerId,
            providerName = providerName,
            profession = profession,
            category = category,
            imageRes = imageRes,

            serviceDate = serviceDate,
            serviceTime = serviceTime,
            serviceAddress = serviceAddress.trim(),
            problemDescription = problemDescription.trim(),

            attachments = attachments.toList(),

            estimatedPrice = estimatedPrice,
            status = BookingStatus.UPCOMING,

            createdAt = currentTime
        )

        bookingRecords.add(
            index = 0,
            element = newBooking
        )

        return newBooking
    }

    fun getBookingById(
        bookingId: Long
    ): BookingRecord? {
        return bookingRecords.firstOrNull { booking ->
            booking.id == bookingId
        }
    }

    fun getBookingByReference(
        reference: String
    ): BookingRecord? {
        return bookingRecords.firstOrNull { booking ->
            booking.reference == reference
        }
    }

    fun updateBookingStatus(
        bookingId: Long,
        newStatus: BookingStatus
    ) {
        val bookingIndex =
            bookingRecords.indexOfFirst { booking ->
                booking.id == bookingId
            }

        if (bookingIndex == -1) {
            return
        }

        val existingBooking =
            bookingRecords[bookingIndex]

        bookingRecords[bookingIndex] =
            existingBooking.copy(
                status = newStatus
            )
    }

    fun cancelBooking(
        bookingId: Long
    ) {
        updateBookingStatus(
            bookingId = bookingId,
            newStatus = BookingStatus.CANCELLED
        )
    }

    fun completeBooking(
        bookingId: Long
    ) {
        updateBookingStatus(
            bookingId = bookingId,
            newStatus = BookingStatus.COMPLETED
        )
    }

    fun removeBooking(
        bookingId: Long
    ) {
        bookingRecords.removeAll { booking ->
            booking.id == bookingId
        }
    }

    fun clearBookings() {
        bookingRecords.clear()
    }

    private fun createBookingReference(
        timestamp: Long
    ): String {
        val finalDigits = timestamp
            .toString()
            .takeLast(8)

        return "FIX-$finalDigits"
    }
}