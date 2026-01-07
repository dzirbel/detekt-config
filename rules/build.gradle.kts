import java.util.Properties

plugins {
    kotlin("jvm") version libs.versions.kotlin
    `maven-publish`
}

private val versionsFile = rootProject.layout.projectDirectory.file("plugin/src/main/resources/versions.properties")
private val versions = providers.fileContents(versionsFile).asText
    .map { text ->
        Properties().apply { load(text.byteInputStream()) }
    }
private val detektVersion = versions.map { it["detekt"] }

dependencies {
    compileOnly("io.gitlab.arturbosch.detekt:detekt-api:${detektVersion.get()}")

    testImplementation(kotlin("test"))
    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:${detektVersion.get()}")
}

publishing {
    publications {
        create<MavenPublication>("rules") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/dzirbel/detekt-config")
            credentials {
                username = "dzirbel"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
