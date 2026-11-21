val runNumber: Int = (project.findProperty("RUN_NUMBER") as String?)?.toIntOrNull() ?: 1

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose)
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

android {
    namespace = "com.duchamp"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.duchamp"
        minSdk = 34
        targetSdk = 34
        versionCode = runNumber
        versionName = "0.1.$runNumber"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Stable signing so a new build always installs over the previous one (no data loss).
    // Priority: CI secrets (RELEASE_STORE_FILE / RELEASE_STORE_PASSWORD / RELEASE_KEY_ALIAS /
    // RELEASE_KEY_PASSWORD) -> committed dev keystore (signing/dev.keystore, passwords in
    // gradle.properties). For a public repo, prefer the secrets route (see README).
    signingConfigs {
        create("dev") {
            storeFile = rootProject.file(rootProject.property("duchamp.dev.storeFile") as String)
            storePassword = rootProject.property("duchamp.dev.storePassword") as String
            keyAlias = rootProject.property("duchamp.dev.keyAlias") as String
            keyPassword = rootProject.property("duchamp.dev.keyPassword") as String
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
        if (project.hasProperty("RELEASE_STORE_FILE")) {
            create("release") {
                storeFile = file(project.property("RELEASE_STORE_FILE") as String)
                storePassword = project.property("RELEASE_STORE_PASSWORD") as String
                keyAlias = project.property("RELEASE_KEY_ALIAS") as String
                keyPassword = project.property("RELEASE_KEY_PASSWORD") as String
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        } else {
            create("release") {
                storeFile = rootProject.file(rootProject.property("duchamp.dev.storeFile") as String)
                storePassword = rootProject.property("duchamp.dev.storePassword") as String
                keyAlias = rootProject.property("duchamp.dev.keyAlias") as String
                keyPassword = rootProject.property("duchamp.dev.keyPassword") as String
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("dev")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// APK file name: duchamp-<versionName>.apk
androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName = "duchamp-${variant.versionName.get()}.apk"
        }
    }
}

dependencies {
    implementation(project(":core:root"))
    implementation(project(":core:device"))
    implementation(project(":core:data"))
    implementation(project(":designsystem"))
    implementation(project(":feature:gamemode"))
    implementation(project(":feature:monitor"))
    implementation(project(":feature:sessions"))
    implementation(project(":feature:health"))
    implementation(project(":feature:system"))
    implementation(project(":feature:network"))
    implementation(project(":feature:onboarding"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.annotation)

    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
