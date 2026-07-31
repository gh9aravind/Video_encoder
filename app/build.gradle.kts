plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.videotranscoder"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.videotranscoder"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Include only common ABIs to reduce APK size
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    // Split APKs by ABI — drastically reduces download size for each device
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true // Also build a universal APK for sideloading
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
        // Handle duplicate native libs from FFmpegKit
        jniLibs {
            pickFirsts += "**/libavcodec.so"
            pickFirsts += "**/libavfilter.so"
            pickFirsts += "**/libavformat.so"
            pickFirsts += "**/libavutil.so"
            pickFirsts += "**/libswresample.so"
            pickFirsts += "**/libswscale.so"
        }
    }
}

dependencies {
    // ── Jetpack Compose BOM (manages all Compose library versions) ────────────
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // Extra icons

    // ── AndroidX Core & Activity ──────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")

    // ── Lifecycle & ViewModel ─────────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7") // collectAsStateWithLifecycle

    // ── Navigation Compose ────────────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // ── Kotlin Coroutines ─────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Previous FFmpegKit fork wasn't built for Android's new 16KB memory-page
    // requirement, causing an instant native crash on newer devices (like yours).
    // This fork is specifically rebuilt with NDK r27d for 16KB page-size support.
    implementation("io.github.jamaismagic.ffmpeg:ffmpeg-kit-lts-full-gpl-16kb:6.1.+")

    // ── Coil — video thumbnail loading ───────────────────────────────────────
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
