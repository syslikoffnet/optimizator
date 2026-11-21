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
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "duchamp"

include(":app")
include(":core:root")
include(":core:device")
include(":core:data")
include(":designsystem")
include(":feature:gamemode")
include(":feature:monitor")
include(":feature:sessions")
include(":feature:health")
include(":feature:system")
include(":feature:network")
include(":feature:onboarding")
