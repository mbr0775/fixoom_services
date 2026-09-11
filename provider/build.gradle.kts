import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.fixoom.provider"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.fixoom.provider"
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
            localPropertiesFile.inputStream().use { input ->
                localProperties.load(input)
            }
        }

        val supabaseUrl =
            localProperties.getProperty("SUPABASE_URL").orEmpty()

        val supabaseAnonKey =
            localProperties.getProperty("SUPABASE_ANON_KEY").orEmpty()

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        )
    }
}

dependencies {
    // Shared Fixoom module
    implementation(project(":core"))

    // Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Persistent settings
    implementation(
        "androidx.datastore:datastore-preferences:1.2.1"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0"
    )

    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.10.0"
    )

    // Jetpack Compose
    implementation(
        platform(libs.androidx.compose.bom)
    )
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(
        libs.androidx.compose.ui.tooling.preview
    )
    implementation(libs.androidx.compose.material3)
    implementation(
        libs.androidx.compose.material.icons.extended
    )

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Testing
    testImplementation(libs.junit)

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