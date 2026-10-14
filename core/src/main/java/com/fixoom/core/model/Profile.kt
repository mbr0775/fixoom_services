package com.fixoom.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,

    val role: AppRole,

    @SerialName("account_status")
    val accountStatus: AccountStatus,

    @SerialName("full_name")
    val fullName: String? = null,

    val phone: String? = null,

    @SerialName("avatar_path")
    val avatarUrl: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)
