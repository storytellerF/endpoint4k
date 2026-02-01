@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidLibrary)
    `maven-publish`
    id("common-multiplatform-publish")
}

kotlin {
    jvm()
    android {
        namespace = "com.storyteller_f.route4k.common"
        compileSdk = 35
        minSdk = 24
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    wasmJs {
        browser()
    }
    js {
        browser()
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain {
            dependencies {
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
