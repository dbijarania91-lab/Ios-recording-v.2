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
        // CRITICAL: Required to download the Shizuku API
        maven { url = java.net.URI("https://api.xposed.info/") } 
    }
}

rootProject.name = "HexRecorder"
include(":app")
