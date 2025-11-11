plugins {
    `maven-publish`
}

group = property("group") ?: "com.storyteller_f.endpoint4k"
version = property("version") ?: "1.0-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.displayName.substringAfter("project ").removeSurrounding("'").removePrefix(":")
                .replace(":", "-")
            from(components["java"])
        }
    }
}