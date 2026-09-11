package com.fixoom.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AppRole {

    @SerialName("customer")
    CUSTOMER,

    @SerialName("provider")
    PROVIDER,

    @SerialName("admin")
    ADMIN
}