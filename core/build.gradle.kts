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

    // Android HTTP engine used by Supabase
    implementation(
        libs.ktor.client.android
    )

    // Testing
    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )
}