import java.util.Properties

plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "io.github.dzirbel"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    val versions = file("src/main/resources/versions.properties").inputStream().use {
        Properties().apply { load(it) }
    }

    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${versions["detekt"]}")
}

kotlin {
    jvmToolchain(jdkVersion = 11)
}

gradlePlugin {
    vcsUrl = "https://github.com/dzirbel/detekt-config"

    plugins {
        register(name) {
            id = "$group.detekt-config"
            implementationClass = "$group.DetektConfigPlugin"
        }
    }
}

tasks.validatePlugins {
    enableStricterValidation = true
    failOnWarning = true
    ignoreFailures = false
}

// run check every time :plugin is built, so it is checked on every use from the root project
tasks.jar.configure { finalizedBy(tasks.check) }

publishing {
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
