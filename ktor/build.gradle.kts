plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.2.0"
}

group = "com.storyteller_f.route4k"
version = "1.0-SNAPSHOT"

dependencies {
    testImplementation(project(":common"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(project(":ktor:client"))
    testImplementation(project(":ktor:server"))
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.content.negotiation)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.logback.classic)
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