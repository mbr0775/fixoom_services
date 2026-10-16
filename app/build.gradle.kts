import java.util.Properties

plugins {
    alias(libs.plugins.android.application)

    // Do not enable kotlin.android because this project
    // already uses Android's integrated Kotlin support.
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst { check(file("google-services.json").exists()) { "Download the Fixoom Customer Firebase configuration before building a release." } }
}

android {
    namespace = "com.fixoom.customer"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.fixoom.customer"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

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
        compose = true
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
    implementation(project(":marketplace-ui"))
    // Shared code used by Customer and Provider apps
    implementation(
        project(":core")
    )

    // Customer app: Android core
    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(
        "androidx.datastore:" +
                "datastore-preferences:1.2.1"
    )

    implementation(
        libs.androidx.compose.material.icons.extended
    )

    implementation(
        libs.androidx.activity.compose
    )

    // Customer app: Compose
    implementation(
        platform(libs.androidx.compose.bom)
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.graphics
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )

    implementation(
        libs.androidx.compose.material3
    )

    implementation(
        libs.androidx.compose.material.icons.extended
    )

    // Customer app: Navigation
    implementation(
        libs.androidx.navigation.compose
    )

    // Customer app: Supabase
    //
    // These remain temporarily until SupabaseClient.kt
    // is moved safely into the shared core module.
    implementation(
        platform(libs.supabase.bom)
    )

    implementation(
        "io.github.jan-tennert.supabase:auth-kt"
    )

    implementation(
        libs.supabase.postgrest
    )

    implementation(
        "io.github.jan-tennert.supabase:storage-kt"
    )

    implementation(
        libs.ktor.client.android
    )

    // Customer app: Testing
    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )
}
