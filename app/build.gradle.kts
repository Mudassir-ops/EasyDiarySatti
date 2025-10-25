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
        versionCode = 13
        versionName = "13.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        setProperty("archivesBaseName", "EasyDiary-v$versionCode($versionName)")
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
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    //in app update
    implementation(libs.play.app.update)
    implementation(libs.app.update.ktx)

}

kapt {
    correctErrorTypes = true
    useBuildCache = false
    generateStubs = true
}
ksp {
    arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
}