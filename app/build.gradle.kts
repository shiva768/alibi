plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.alibi"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.alibi"
        minSdk = 29 // ACTIVITY_RESUMED / ACTIVITY_PAUSED は API 29+
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // CI で app/debug.keystore が置かれたときだけ固定鍵で署名(上書きインストール用)
    val debugKeystore = file("debug.keystore")
    if (debugKeystore.exists()) {
        signingConfigs.getByName("debug") {
            storeFile = debugKeystore
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    testImplementation("junit:junit:4.13.2")
}
