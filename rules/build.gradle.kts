import java.util.Properties

plugins {
    kotlin("jvm") version libs.versions.kotlin
    `maven-publish`
}

private val versions = rootProject.extra["versions"] as Properties
private val detektVersion = versions.getProperty("detekt")

dependencies {
    compileOnly("dev.detekt:detekt-api:$detektVersion")

    testImplementation(kotlin("test"))
    testImplementation("dev.detekt:detekt-test:$detektVersion")
    testImplementation("dev.detekt:detekt-test-assertj:$detektVersion")
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
