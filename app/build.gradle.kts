plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.nothingrecorder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.nothingrecorder"
        minSdk = 29 // Android 10 minimum for Internal Audio Capture
        targetSdk = 34
        versionCode = 1
        versionName = "1.0-HexEngine"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // The Zero-Lag Shell API (Shizuku) - PROVIDER DELETED
    val shizuku_version = "13.1.5"
    implementation("dev.rikka.shizuku:api:$shizuku_version")

    // Coroutines for the Thermal Governor and background tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
