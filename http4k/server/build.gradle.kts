plugins {
    kotlin("jvm") version "2.1.21"
}

dependencies {
    implementation(project(":common"))
    implementation("org.http4k:http4k-core:5.30.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}