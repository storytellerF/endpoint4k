plugins {
    kotlin("jvm") version "2.1.21"
}

dependencies {
    implementation(project(":common"))
    implementation(libs.http4k.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}