package com.fixoom.customer.ui.profile

import com.fixoom.core.model.AccountStatus
import com.fixoom.core.model.AppRole
import com.fixoom.core.model.Profile
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileAccountStateTest {
    private fun profile(
        role: AppRole = AppRole.CUSTOMER,
        status: AccountStatus = AccountStatus.ACTIVE,
        name: String? = "  Test Customer  ",
        phone: String? = "  +94770000000  "
    ) = Profile(id = "test-only", role = role, accountStatus = status, fullName = name, phone = phone)

    @Test fun `no session displays guest rather than an invented identity`() {
        assertEquals(ProfileAccountState.Guest, (null as Profile?).toCustomerAccountState())
    }

    @Test fun `active customer details are trimmed without changing phone contents`() {
        assertEquals(ProfileAccountState.SignedIn("Test Customer", "+94770000000"), profile().toCustomerAccountState())
    }

    @Test fun `missing details stay missing instead of being filled with samples`() {
        assertEquals(ProfileAccountState.SignedIn(null, null), profile(name = "  ", phone = null).toCustomerAccountState())
    }

    @Test fun `provider and admin sessions are not presented as customer accounts`() {
        assertEquals(ProfileAccountState.Unavailable, profile(role = AppRole.PROVIDER).toCustomerAccountState())
        assertEquals(ProfileAccountState.Unavailable, profile(role = AppRole.ADMIN).toCustomerAccountState())
    }

    @Test fun `restricted account states do not display an active account`() {
        listOf(AccountStatus.PENDING, AccountStatus.SUSPENDED, AccountStatus.REJECTED, AccountStatus.DELETED).forEach {
            assertEquals(ProfileAccountState.Unavailable, profile(status = it).toCustomerAccountState())
        }
    }

    @Test fun `initials handle whitespace and single names`() {
        assertEquals("TC", profileInitials("  test   customer  "))
        assertEquals("T", profileInitials("Test"))
        assertEquals("TC", profileInitials("Test Customer Person"))
    }

    @Test fun `initials support local scripts and accented names`() {
        assertEquals("අප", profileInitials("අමල් පෙරේරා"))
        assertEquals("ÉD", profileInitials("Élodie Dupont"))
    }

    @Test fun `missing or symbol only names use the person icon fallback`() {
        assertEquals("", profileInitials(null))
        assertEquals("", profileInitials("  "))
        assertEquals("", profileInitials("🔧 !"))
    }
}
