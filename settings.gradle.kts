pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    includeBuild("common-publish")
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "route4k"
include(":common")
include(":ktor:client")
include(":ktor:server")
include(":ktor")
include(":okhttp")
include(":okhttp:client")
include(":http4k:client")
include(":http4k")
include(":http4k:server")