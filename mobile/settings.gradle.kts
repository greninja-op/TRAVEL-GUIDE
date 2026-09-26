pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "travel-guide"
include(":app")
// Pure-Kotlin logic lives at travel-guide/shared-core (SPEC §4.1), one level
// above this Gradle root — wire it in as a module so :app can depend on it.
include(":shared-core")
project(":shared-core").projectDir = file("../shared-core")
