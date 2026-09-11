package com.fixoom.customer.data

import com.fixoom.customer.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json


object SupabaseClient {


    val client = createSupabaseClient(

        supabaseUrl = BuildConfig.SUPABASE_URL,

        supabaseKey = BuildConfig.SUPABASE_ANON_KEY

    ) {


        install(Auth)


        install(Postgrest)


        install(Storage)



        defaultSerializer = KotlinXSerializer(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }
        )

    }


}