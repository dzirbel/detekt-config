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

// Hack(?): create a configuration to add the Kotlin gradle plugin to the test classpath, otherwise Gradle projects
// created within tests don't appear to have it. This is a deep solution which I don't fully understand, but I haven't
// found another way.
val kotlinPluginForTests = configurations.create("kotlinPluginForTests") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${detektVersion.get()}")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    kotlinPluginForTests("org.jetbrains.kotlin:kotlin-gradle-plugin")
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
