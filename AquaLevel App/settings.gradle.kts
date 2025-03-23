pluginManagement {
    repositories {
        google()  // Removed the content filtering for Hilt to work properly
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

rootProject.name = "AquaLevel"
include(":app")