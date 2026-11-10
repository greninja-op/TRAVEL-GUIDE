// Top-level build — see app/build.gradle.kts for deps.
// AGP 9.5.1 pairs with Gradle 9.9 (wrapper) and Kotlin 2.4.21.
// AGP 9 has built-in Kotlin support, so the org.jetbrains.kotlin.android
// plugin must NOT be applied here or in :app (see
// developer.android.com/build/migrate-to-built-in-kotlin).
plugins {
    id("com.android.application") version "9.5.1" apply false
    id("org.jetbrains.kotlin.jvm") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("com.google.devtools.ksp") version "2.3.13" apply false
}
