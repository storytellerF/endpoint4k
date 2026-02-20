plugins {
    alias(libs.plugins.kotlin.jvm)
    id("common-publish")
}

dependencies {
    implementation(project(":common"))
    implementation(libs.ktor.server.core)
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
