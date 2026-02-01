plugins {
    `maven-publish`
}

group = property("group") ?: "com.storyteller_f.endpoint4k"
version = property("version") ?: "1.0-SNAPSHOT"

println("group: $group, version: $version")

publishing {
    publications {
        // 如果不添加下面的代码，无法发布出来artifact
        create<MavenPublication>("mavenJava") {
            artifactId = project.displayName.substringAfter("project ").removeSurrounding("'").removePrefix(":")
                .replace(":", "-")
            // 包含java kotlin，选择kotlin 也能发布成功
            from(components["kotlin"])
        }
    }
}