import java.util.Properties

plugins {
    `kotlin-dsl`
    `maven-publish`
}

private val versions = rootProject.extra["versions"] as Provider<Properties>
private val detektVersion = versions.map { it["detekt"] }

val kotlinPluginForTests = configurations.create("kotlinPluginForTests") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${detektVersion.get()}")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("gradle-plugin"))
    kotlinPluginForTests(kotlin("gradle-plugin"))
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

tasks.withType<PluginUnderTestMetadata>().configureEach {
    pluginClasspath.from(kotlinPluginForTests)
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
