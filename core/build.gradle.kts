import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.fixoom.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        consumerProguardFiles(
            "consumer-rules.pro"
        )

        val localProperties = Properties()

        val localPropertiesFile =
            rootProject.file("local.properties")

        if (localPropertiesFile.exists()) {
            localProperties.load(
                localPropertiesFile.inputStream()
            )
        }

        val supabaseUrl =
            localProperties.getProperty(
                "SUPABASE_URL"
            ) ?: ""

        val supabaseAnonKey =
            localProperties.getProperty(
                "SUPABASE_ANON_KEY"
            ) ?: ""

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"$supabaseUrl\""
        )

        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"$supabaseAnonKey\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl
                .JvmTarget
                .JVM_17
        )
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:35.0.1"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    api("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    // Shared Android utilities
    implementation(
        libs.androidx.core.ktx
    )

    // Shared persistent preferences
    implementation(
        "androidx.datastore:" +
                "datastore-preferences:1.2.1"
    )

    // Shared Supabase SDK
    api(
        platform(libs.supabase.bom)
    )

    api(
        "io.github.jan-tennert.supabase:auth-kt"
    )

    api(
        libs.supabase.postgrest
    )

    api(
        "io.github.jan-tennert.supabase:storage-kt"
    )

    api("io.github.jan-tennert.supabase:realtime-kt")

    // OkHttp supports the authenticated Realtime WebSocket.
    implementation(
        "io.ktor:ktor-client-okhttp:${libs.versions.ktor.get()}"
    )

    // Testing
    testImplementation(
        libs.junit
    )
    testImplementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )
}
