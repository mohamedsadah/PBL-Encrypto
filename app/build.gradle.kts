plugins {
    alias(libs.plugins.android.application)
//    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.encrypto"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.encrypto"
        minSdk = 23
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // UI & AndroidX
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.annotation)

    // Lifecycle (Java version)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)

    // Navigation (Java)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)          // REQUIRED
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Location / Geofencing
    implementation(libs.play.services.location)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)

    // WebSocket
    implementation(libs.java.websocket)

    // Glide (Java)
    implementation(libs.glide)
    implementation(libs.lifecycle.livedata.core)
    annotationProcessor(libs.glide.compiler)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.security.crypto)

    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
}
