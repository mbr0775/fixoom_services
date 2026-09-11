package com.fixoom.customer.ui.profile

import com.fixoom.core.model.AccountStatus
import com.fixoom.core.model.AppRole
import com.fixoom.core.model.Profile
import java.util.Locale

internal sealed interface ProfileAccountState {
    data object Loading : ProfileAccountState
    data object Guest : ProfileAccountState
    data class SignedIn(val fullName: String?, val phone: String?) : ProfileAccountState
    data object Unavailable : ProfileAccountState
}

/** Read-only presentation: never invent an account or treat another role as a customer. */
internal fun Profile?.toCustomerAccountState(): ProfileAccountState = when {
    this == null -> ProfileAccountState.Guest
    role != AppRole.CUSTOMER || accountStatus != AccountStatus.ACTIVE -> ProfileAccountState.Unavailable
    else -> ProfileAccountState.SignedIn(
        fullName = fullName?.trim()?.takeIf(String::isNotBlank),
        phone = phone?.trim()?.takeIf(String::isNotBlank)
    )
}

internal fun profileInitials(name: String?): String = name.orEmpty()
    .trim()
    .split(Regex("\\s+"))
    .mapNotNull { word -> word.firstOrNull(Char::isLetter) }
    .take(2)
    .joinToString("")
    .uppercase(Locale.ROOT)

