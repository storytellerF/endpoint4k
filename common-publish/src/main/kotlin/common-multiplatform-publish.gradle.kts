plugins {
    `maven-publish`
}

// group 一直都有值，如果通过-P 指定是不需要通过property 获取的
group = group.takeIf { it.toString().count { it == '.' } >= 2 } ?: "com.storyteller_f.endpoint4k"
version = version.takeIf { it != "unspecified" } ?: "0.0.1-local"

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
