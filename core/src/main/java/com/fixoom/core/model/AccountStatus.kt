package com.fixoom.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AccountStatus {

    @SerialName("pending")
    PENDING,

    @SerialName("active")
    ACTIVE,

    @SerialName("suspended")
    SUSPENDED,

    @SerialName("rejected")
    REJECTED
}