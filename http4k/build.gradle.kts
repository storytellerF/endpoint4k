plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    testImplementation(project(":http4k:client"))
    testImplementation(project(":http4k:server"))
    testImplementation(project(":common"))
    testImplementation(libs.http4k.core)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.coroutines.core)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}
