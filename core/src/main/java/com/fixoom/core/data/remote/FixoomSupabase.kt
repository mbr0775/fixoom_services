package com.fixoom.core.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json

object FixoomSupabase {

    @Volatile private var initializedClient: SupabaseClient? = null

    val client: SupabaseClient
        get() =
            checkNotNull(initializedClient) {
                "FixoomSupabase has not been initialized."
            }

    fun initialize(
        supabaseUrl: String,
        supabaseAnonKey: String,
        authScheme: String? = null,
        authHost: String? = null,
    ) {
        val cleanUrl = supabaseUrl.trim()
        val cleanAnonKey = supabaseAnonKey.trim()
        val cleanScheme = authScheme?.trim()?.takeIf { it.isNotBlank() }
        val cleanHost = authHost?.trim()?.takeIf { it.isNotBlank() }

        require(cleanUrl.isNotBlank()) {
            "Supabase URL cannot be empty."
        }

        require(cleanAnonKey.isNotBlank()) {
            "Supabase anonymous key cannot be empty."
        }

        require((cleanScheme == null) == (cleanHost == null)) {
            "Authentication scheme and host must both be provided."
        }

        if (initializedClient != null) return

        synchronized(this) {
            if (initializedClient == null) {
                initializedClient =
                    createSupabaseClient(
                        supabaseUrl = cleanUrl,
                        supabaseKey = cleanAnonKey,
                    ) {
                        defaultSerializer =
                            KotlinXSerializer(
                                Json {
                                    ignoreUnknownKeys = true
                                    explicitNulls = false
                                    encodeDefaults = true
                                }
                            )

                        install(Auth) {
                            if (cleanScheme != null && cleanHost != null) {
                                scheme = cleanScheme
                                host = cleanHost
                            }
                        }

                        httpEngine = OkHttp.create()
                        install(Realtime)
                        install(Postgrest)
                        install(Storage)
                    }
            }
        }
    }
}
