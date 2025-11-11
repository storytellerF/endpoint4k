import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.report.ReportMergeTask

plugins {
    kotlin("jvm") version "2.1.21"
    alias(libs.plugins.detekt)
}

group = "com.storyteller_f.endpoint4k"
version = "1.0-SNAPSHOT"

val detektReportMergeSarif by tasks.registering(ReportMergeTask::class) {
    output = layout.buildDirectory.file("reports/detekt/merge.sarif")
}
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    detekt {
        // The directories where detekt looks for source files.
        // Defaults to `files("src/main/java", "src/test/java", "src/main/kotlin", "src/test/kotlin")`.
        source.setFrom(
            "src/main/kotlin",
            "src/test/kotlin",
            "src/commonMain/kotlin",
            "src/desktopMain/kotlin",
            "src/iosMain/kotlin",
            "src/wasmJsMain/kotlin",
            "src/androidMain/kotlin"
        )
        // Builds the AST in parallel. Rules are always executed in parallel.
        // Can lead to speedups in larger projects. `false` by default.
        parallel = true

        // Auto-correct issues which can be fixed automatically. Remaining findings should fail the build.
        autoCorrect = true
        ignoreFailures = false

        // Use the shared detekt configuration
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))

        // Android: Don't create tasks for the specified build types (e.g. "release")
        ignoredBuildTypes = listOf("release")

        // Specify the base path for file paths in the formatted reports.
        // If not set, all file paths reported will be absolute file path.
        basePath = projectDir.absolutePath

        buildUponDefaultConfig = true
    }

    dependencies {
        detektPlugins(rootProject.libs.detekt.formatting)
        detektPlugins(rootProject.libs.detekt.rules.libraries)
        detektPlugins(rootProject.libs.detekt.rules.ruleauthors)
    }

    tasks.withType<Detekt>().configureEach {
        reports {
            xml.required = true
            html.required = true
            txt.required = true
            sarif.required = true
            md.required = true
        }
        basePath = rootDir.absolutePath
        finalizedBy(detektReportMergeSarif)
    }

    // Ensure detekt runs as part of the standard build lifecycle when a 'check' task exists
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn("detekt")
    }

    detektReportMergeSarif {
        input.from(
            tasks.withType<Detekt>().map { it.sarifReportFile })
    }
}