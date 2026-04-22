plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.joshmermelstein.timesince"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.joshmermelstein.timesince"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            //isMinifyEnabled = true
            //proguardFiles(
            //    getDefaultProguardFile("proguard-android-optimize.txt"),
            //    "proguard-rules.pro"
            //)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
    compileSdkMinor = 1
    buildToolsVersion = "36.1.0"
}

kotlin {
    jvmToolchain(25)
}

@Suppress("DEPRECATION")
dependencies {
    implementation(project(":shared"))
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.ui)
    implementation(compose.foundation)
    implementation(compose.runtime)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.core.ktx)
    implementation(libs.room.runtime)
    implementation(libs.work.runtime.ktx)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.play.services.auth)
}
