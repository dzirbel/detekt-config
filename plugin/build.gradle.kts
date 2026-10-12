import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import java.util.Properties

plugins {
    `kotlin-dsl`
    `maven-publish`
}

private val versions = rootProject.extra["versions"] as Properties
private val detektVersion = versions.getProperty("detekt")

// TestKit builds need the same agent as the test JVM, with output captured by the test task's build cache entry.
val testkitCoverageProperties = tasks.register<WriteProperties>("generateTestkitCoverageProperties") {
    destinationFile = layout.buildDirectory.file("testkit-coverage/testkit-gradle.properties")
    property("org.gradle.jvmargs", tasks.test.map { task ->
        val coverage = task.extensions.getByType<JacocoTaskExtension>()
        // Gradle uses a relative destination for its test JVM; fixture JVMs have different working directories.
        val agent = coverage.asJvmArg.replace(
            "destfile=${project.relativePath(coverage.destinationFile!!)}",
            "destfile=${coverage.destinationFile!!.absolutePath}",
        )
        "\"$agent\""
    })
}

dependencies {
    compileOnly(kotlin("gradle-plugin"))
    implementation("dev.detekt:detekt-gradle-plugin:$detektVersion")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("gradle-plugin"))
    testRuntimeOnly(files(testkitCoverageProperties.map { it.destinationFile.get().asFile.parentFile }))
    implementation("org.yaml:snakeyaml:${versions.getProperty("snakeyaml")}")
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
