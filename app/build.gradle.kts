import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseVersion = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val signingValues = Properties().apply {
    val source = rootProject.file(".signing/signing.properties")
    if (source.exists()) source.inputStream().use { load(it) }
}
fun signingValue(name: String, environment: String): String? =
    System.getenv(environment) ?: signingValues.getProperty(name)
val releaseKeystore = signingValue("storeFile", "AYAME_KEYSTORE")

android {
    namespace = "dev.andrija.ayametv"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.andrija.ayametv"
        minSdk = 26
        targetSdk = 35
        versionCode = releaseVersion.getProperty("versionCode").toInt()
        versionName = releaseVersion.getProperty("versionName")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (releaseKeystore != null) create("distribution") {
            storeFile = rootProject.file(releaseKeystore)
            storePassword = signingValue("storePassword", "AYAME_STORE_PASSWORD")
            keyAlias = signingValue("keyAlias", "AYAME_KEY_ALIAS")
            keyPassword = signingValue("keyPassword", "AYAME_KEY_PASSWORD")
        }
    }
    buildTypes {
        debug { versionNameSuffix = "-dev" }
        release {
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("distribution")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
