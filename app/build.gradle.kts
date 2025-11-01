import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dagger.hilt.plugins)
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.example.easydiarysatti"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dailydiary.privatejournal.lockednotes"
        minSdk = 24
        targetSdk = 36
        versionCode = 14
        versionName = "14.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        setProperty("archivesBaseName", "EasyDiary-v$versionCode($versionName)")
    }

    buildTypes {
        debug {
            // App Ad Id - Satti
            resValue(
                type = "string",
                name = "admob_app_id",
                value = "ca-app-pub-6929888913467755~1203974256"
            )

// App Open Ad - Satti
            resValue(
                type = "string",
                name = "admob_app_open_id",
                value = "ca-app-pub-3940256099942544/9257395921"
            )

// Banner Ad - Satti
            resValue(
                type = "string",
                name = "admob_banner_home_id",
                value = "ca-app-pub-3940256099942544/2014213617"
            )

// Rewarded Ads - Satti
            resValue(
                type = "string",
                name = "admob_rewarded_ai_feature_id",
                value = "ca-app-pub-3940256099942544/5224354917"
            )
            resValue(
                type = "string",
                name = "admob_rewarded_inter_ai_feature_id",
                value = "ca-app-pub-3940256099942544/5354046379"
            )

// Interstitial Ads - Satti
            resValue(
                type = "string",
                name = "admob_inter_splash_id",
                value = "ca-app-pub-3940256099942544/1033173712"
            )
            resValue(
                type = "string",
                name = "admob_inter_on_boarding_id",
                value = "ca-app-pub-3940256099942544/1033173712"
            )

// Native Ads - Satti
            resValue(
                type = "string",
                name = "admob_native_language_id",
                value = "ca-app-pub-3940256099942544/2247696110"
            )
            resValue(
                type = "string",
                name = "admob_native_on_boarding_id",
                value = "ca-app-pub-3940256099942544/2247696110"
            )
            resValue(
                type = "string",
                name = "admob_native_home_id",
                value = "ca-app-pub-3940256099942544/2247696110"
            )
            resValue(
                type = "string",
                name = "admob_native_full_screen_id",
                value = "ca-app-pub-3940256099942544/2247696110"
            )
            resValue(
                type = "string",
                name = "admob_native_settings_id",
                value = "ca-app-pub-3940256099942544/2247696110"
            )

            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable=true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable=false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true

    }
    buildFeatures {
        viewBinding = true
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.legacy.support.v4)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)


    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler.v2481)

    //Glide
    implementation(libs.glide)
    //ksp(libs.ksp)

    //dimens
    implementation(libs.dimens.sdp)
    implementation(libs.dimens.ssp)

    //room database
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    //lottie
    implementation(libs.lottie)

    //shimmer
    implementation(libs.shimmer)

    implementation(libs.view)
    implementation(libs.flexbox.layout)
    implementation(libs.gson.v2110)

    implementation(libs.image.cropper)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.colorpickerview)

    //-Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.google.firebase.analytics)
    implementation(libs.google.firebase.crashlytics)

    //in app update
    implementation(libs.play.app.update)
    implementation(libs.app.update.ktx)

    implementation(libs.play.services.ads)
}

kapt {
    correctErrorTypes = true
    useBuildCache = false
    generateStubs = true
}
ksp {
    arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
}