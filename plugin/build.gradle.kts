import java.util.Properties

plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "io.github.dzirbel"
version = "1.0.0"

private val versions = providers.fileContents(layout.projectDirectory.file("src/main/resources/versions.properties"))
    .asText
    .map { text ->
        Properties().apply { load(text.byteInputStream()) }
    }
private val detektVersion = versions.map { it["detekt"] }

repositories {
    mavenCentral()
}

val kotlinPluginForTests = configurations.create("kotlinPluginForTests") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${detektVersion.get()}")
    implementation(project(":rules"))

    testImplementation("org.jetbrains.kotlin:kotlin-test")

    val kotlinGradlePlugin = kotlin("gradle-plugin")
    testImplementation(kotlinGradlePlugin)
    kotlinPluginForTests(kotlinGradlePlugin)
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
