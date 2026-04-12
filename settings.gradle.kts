pluginManagement {
    repositories {
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/") {
            content {

            }
        }
        google()
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/") {
            content {

            }
        }
        google()
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "endpoint4k"
include(":common")
include(":ktor:ktor-client")
include(":ktor:ktor-server")
include(":ktor")
include(":okhttp:okhttp-client")
include(":okhttp")
include(":http4k:http4k-client")
include(":http4k:http4k-server")
include(":http4k")
includeBuild("common-publish")
