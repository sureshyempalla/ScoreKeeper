import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            // Cross-platform Firebase Auth (email/password + shared session state).
            // Phone-number auth is handled per-platform (see androidMain/iosMain
            // AuthRepository actuals) since Firebase's phone flow relies on
            // platform-specific app-verification (Play Integrity vs APNs).
            implementation(libs.gitlive.firebase.auth)
            implementation(libs.gitlive.firebase.common)
            // Sync (EventSyncRepository) and single-device-session enforcement
            // (SessionGuard) both store their data in Firestore.
            implementation(libs.gitlive.firebase.firestore)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            // Native SDK, used directly (not via GitLive) only for phone-number
            // auth on Android — see the note on the commonMain dependencies above.
            // Pinned explicitly (rather than via the firebase-bom platform()
            // import) because the Kotlin Multiplatform dependency DSL can't
            // resolve a version-catalog Provider through platform().
            implementation(libs.firebase.auth.ktx)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.scorekeeper.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("ScoreKeeperDatabase") {
            packageName.set("com.scorekeeper.db")
        }
    }
}
