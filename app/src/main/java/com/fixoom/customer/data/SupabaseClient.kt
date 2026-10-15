package com.fixoom.customer.data

import com.fixoom.core.data.remote.FixoomSupabase

/** Legacy callers use the same session as the connected customer workflow. */
object SupabaseClient {
    val client
        get() = FixoomSupabase.client
}
