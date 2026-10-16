package com.fixoom.customer

import android.app.Application
import com.fixoom.core.data.notifications.PushMessaging
import com.fixoom.core.data.remote.FixoomSupabase

class FixoomCustomerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FixoomSupabase.initialize(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
            authScheme = "fixoom-customer",
            authHost = "auth",
        )
        PushMessaging.initialize(this)
    }
}
