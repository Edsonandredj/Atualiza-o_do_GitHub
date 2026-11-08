plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Número do build: o GitHub Actions incrementa sozinho a cada execução
val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.edson.ghwatch"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.edson.ghwatch"
        minSdk = 26
        targetSdk = 34
        versionCode = runNumber
        versionName = "1.0.$runNumber"
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
