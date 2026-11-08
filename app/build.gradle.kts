plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.zyad.crashwatch"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.zyad.crashwatch"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
