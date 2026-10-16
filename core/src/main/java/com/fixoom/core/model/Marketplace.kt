package com.fixoom.core.model

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServiceCategory(
    val slug: String,
    val name: String,
    val description: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class ServiceProfessional(
    val id: String,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("category_slug") val categorySlug: String? = null,
    @SerialName("service_area") val serviceArea: String = "",
    val bio: String = "",
    @SerialName("is_available") val isAvailable: Boolean = false,
    @SerialName("avatar_path") val avatarPath: String? = null,
)

@Serializable
data class ServiceBooking(
    val id: String,
    val reference: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("provider_id") val providerId: String,
    @SerialName("category_slug") val categorySlug: String,
    @SerialName("scheduled_at") val scheduledAt: String,
    @SerialName("service_address") val serviceAddress: String,
    @SerialName("problem_description") val problemDescription: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("provider_name") val providerName: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("category_name") val categoryName: String = "",
    @SerialName("final_amount") val finalAmount: Double? = null,
    val currency: String = "LKR",
    @SerialName("direct_payment_received") val directPaymentReceived: Boolean = false,
) {
    val isClosed
        get() = status in setOf("completed", "cancelled", "rejected")

    val customerCanCancel
        get() = status in setOf("requested", "accepted")
}

@Serializable
data class BookingEvent(
    val id: String,
    @SerialName("booking_id") val bookingId: String,
    val kind: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class BookingMessage(
    val id: String,
    @SerialName("booking_id") val bookingId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String,
)

fun serviceTime(value: String): String = runCatching {
    OffsetDateTime.parse(value)
        .atZoneSameInstant(ZoneId.of("Asia/Colombo"))
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a"))
}
    .getOrDefault(value)

fun serviceStatus(value: String) = value.replace('_', ' ').replaceFirstChar { it.uppercaseChar() }
