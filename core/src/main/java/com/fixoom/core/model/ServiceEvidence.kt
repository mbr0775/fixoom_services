package com.fixoom.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServiceAttachment(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("object_path") val objectPath: String,
    @SerialName("media_type") val mediaType: String,
    @SerialName("byte_size") val byteSize: Long,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ServiceComplaint(
    val id: String,
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("booking_id") val bookingId: String? = null,
    val subject: String,
    val description: String,
    val status: String,
    @SerialName("admin_response") val adminResponse: String = "",
    @SerialName("created_at") val createdAt: String,
)
