import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    // NOTE: no org.jetbrains.kotlin.android — AGP 9 compiles Kotlin built-in.
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    }
}

val secretEnvProps = Properties().apply {
    val f = rootProject.file("../secrets/api-keys.env")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    }
}

val openAiApiKey: String = (
    localProps.getProperty("OPENAI_API_KEY")
        ?: secretEnvProps.getProperty("OPENAI_API_KEY")
        ?: System.getenv("OPENAI_API_KEY")
        ?: ""
).trim().replace("\"", "\\\"")

android {
    namespace = "guide.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "guide.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "OPENAI_API_KEY", "\"$openAiApiKey\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            // Kept off until R8 rules are verified against Room + MapLibre
            // reflection; flip to true once a release build is smoke-tested.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":shared-core")) // SPEC §4.1 pure-Kotlin logic

    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("com.google.android.gms:play-services-location:21.4.0")
    implementation("com.google.android.gms:play-services-maps:20.1.0")
    implementation("org.maplibre.gl:android-sdk:2026.11.2-globe") // OSM offline tiles
    implementation("androidx.biometric:biometric:1.4.0-beta01")

    testImplementation(kotlin("test"))
}

tasks.withType<Test> { useJUnitPlatform() }
