import java.util.Properties

plugins {
    `kotlin-dsl`
    `maven-publish`
}

private val versions = rootProject.extra["versions"] as Properties
private val detektVersion = versions.getProperty("detekt")

dependencies {
    compileOnly(kotlin("gradle-plugin"))
    implementation("dev.detekt:detekt-gradle-plugin:$detektVersion")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("gradle-plugin"))
}

gradlePlugin {
    website = "https://github.com/dzirbel/detekt-config"
    vcsUrl = "https://github.com/dzirbel/detekt-config"

    plugins {
        register("detektConfig") {
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

tasks.processTestResources {
    exclude("**/.gradle/**", "**/.kotlin/**", "**/build/**")
}

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
