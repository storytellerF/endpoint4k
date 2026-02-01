plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    id("common-publish")
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}