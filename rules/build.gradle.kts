import java.util.Properties

plugins {
    kotlin("jvm") version libs.versions.kotlin
    `java-library`
    `maven-publish`
}

group = "io.github.dzirbel"

repositories {
    mavenCentral()
}

val versions = Properties().apply {
    val versionsFile = rootProject.file("plugin/src/main/resources/versions.properties")
    check(versionsFile.exists()) { "Missing versions.properties at ${versionsFile.path}" }
    versionsFile.inputStream().use { load(it) }
}
val detektVersion = versions["detekt"] as String
val rulesVersion = versions["detekt-config-rules"] as String

version = rulesVersion

dependencies {
    compileOnly("io.gitlab.arturbosch.detekt:detekt-api:$detektVersion")
    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:$detektVersion")
    testImplementation("io.gitlab.arturbosch.detekt:detekt-api:$detektVersion")
    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("rules") {
            artifactId = "detekt-config-rules"
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
