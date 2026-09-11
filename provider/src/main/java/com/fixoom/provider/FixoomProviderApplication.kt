package com.fixoom.provider

import android.app.Application
import com.fixoom.core.data.remote.FixoomSupabase

class FixoomProviderApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FixoomSupabase.initialize(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
            authScheme = "fixoom-provider",
            authHost = "auth"
        )
    }
}