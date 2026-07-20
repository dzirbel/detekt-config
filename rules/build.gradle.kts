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
    // detekt 2.0.0-alpha.5's Gradle metadata requests an unpublished detekt-api test-fixtures runtime variant.
    // The rule tests only use detekt-test's public lint helpers, so replace that broken transitive edge with detekt-api.
    testImplementation("dev.detekt:detekt-test:$detektVersion") {
        exclude(group = "dev.detekt", module = "detekt-api")
    }
    testImplementation("dev.detekt:detekt-api:$detektVersion")
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
