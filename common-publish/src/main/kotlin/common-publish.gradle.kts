plugins {
    `java-library`
    `maven-publish`
}

println("group: $group, version: $version")

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/storytellerF/endpoint4k")
            credentials {
                username = project.findProperty("gpr.user") as String
                password = project.findProperty("gpr.key") as String
            }
        }
    }
    publications {
        // 如果不添加下面的代码，无法发布出来artifact
        create<MavenPublication>("mavenJava") {
            // 包含java kotlin，选择kotlin 也能发布成功，但是无法绑定source jar
            from(components["java"])
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}