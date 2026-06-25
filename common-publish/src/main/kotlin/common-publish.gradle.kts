plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    pom {
        name.set("endpoint4k")
        description.set("Type-safe routing library for Ktor, Http4k, and OkHttp")
        url.set("https://github.com/storytellerF/endpoint4k")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("storytellerF")
                name.set("storytellerF")
                email.set("placeholder@example.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/storytellerF/endpoint4k.git")
            developerConnection.set("scm:git:ssh://github.com/storytellerF/endpoint4k.git")
            url.set("https://github.com/storytellerF/endpoint4k")
        }
    }
}
