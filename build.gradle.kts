// AGP 9.0 ships a runtime dependency on the Kotlin Gradle plugin (KGP), defaulting to 2.2.10.
// Force a newer KGP (2.4.21) so the built-in Kotlin compiler, the Compose compiler plugin
// and KSP all share one Kotlin version. See:
// https://developer.android.com/build/releases/agp-9-0-0-release-notes
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    id("com.android.application") version "9.4.0" apply false
    id("com.android.library") version "9.4.0" apply false
    id("com.google.devtools.ksp") version "2.3.13" apply false
    id("com.google.dagger.hilt.android") version "2.60.1" apply false
}
