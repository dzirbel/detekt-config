import java.util.Properties

plugins {
    kotlin("jvm") version libs.versions.kotlin
    `maven-publish`
}

private val versions = rootProject.extra["versions"] as Provider<Properties>
private val detektVersion = versions.map { it["detekt"] }

dependencies {
    compileOnly("dev.detekt:detekt-api:${detektVersion.get()}")

    testImplementation(kotlin("test"))
    testImplementation("dev.detekt:detekt-test:${detektVersion.get()}")
    testImplementation("dev.detekt:detekt-test-assertj:${detektVersion.get()}")
    testImplementation(libs.assertj)
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
