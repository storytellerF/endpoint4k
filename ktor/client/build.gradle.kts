@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidLibrary)
    id("common-multiplatform-publish")
}

kotlin {
    jvm()
    android {
        namespace = "com.storyteller_f.route4k.ktor.client"
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
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":common"))
                implementation(libs.ktor.client.core)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

