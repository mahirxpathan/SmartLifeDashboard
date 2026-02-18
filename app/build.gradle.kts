plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.smartlifedashboard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.smartlifedashboard"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    
    // Firebase BoM ensures compatible versions
    implementation(platform(libs.firebase.bom))
    
    // Core Firebase SDKs
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    
    // Google Sign-In
    implementation(libs.play.services.auth)
    
    // Lifecycle utilities for Compose
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // ViewModel utilities for Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // WorkManager for periodic tasks
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.play.services.location)
    implementation(libs.compose.permission)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.glance)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)
    implementation(libs.foundation)
    ksp(libs.dagger.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}