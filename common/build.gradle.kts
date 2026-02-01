plugins {
    alias(libs.plugins.kotlin.multiplatform)
    `maven-publish`
    id("common-multiplatform-publish")
}

kotlin {
    jvm()
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
