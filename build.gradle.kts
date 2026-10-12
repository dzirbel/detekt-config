import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoReportBase
import java.util.Properties

val versionsFile = layout.projectDirectory.file("plugin/src/main/resources/io/github/dzirbel/detekt-config/versions.properties")
val versions = providers.fileContents(versionsFile).asText
    .map { text ->
        Properties().apply { load(text.byteInputStream()) }
    }
    .get()
extra["versions"] = versions
val jacocoVersion = libs.versions.jacoco.get()

subprojects {
    apply(plugin = "jacoco")
    extensions.configure<JacocoPluginExtension> {
        toolVersion = jacocoVersion
    }

    plugins.withId("java") {
        tasks.withType<JacocoReportBase>().configureEach {
            dependsOn(tasks.named("test"))
            // Functional tests exercise packaged rules as well as the plugin itself.
            if (project.name == "rules") {
                dependsOn(":plugin:test")
                executionData(rootProject.layout.projectDirectory.file("plugin/build/jacoco/test.exec"))
            }
        }
        tasks.withType<JacocoReport>().configureEach {
            reports {
                xml.required = true
                html.required = true
            }
        }
        tasks.withType<JacocoCoverageVerification>().configureEach {
            violationRules {
                rule {
                    limit {
                        counter = "LINE"
                        minimum = if (project.name == "rules") "1.00".toBigDecimal() else "0.99".toBigDecimal()
                    }
                }
            }
        }
        tasks.named("check") {
            dependsOn("jacocoTestReport", "jacocoTestCoverageVerification")
        }
    }

    group = "io.github.dzirbel"

    version = versions.getProperty("rules")

    tasks.withType<Test>().configureEach {
        extensions.configure<JacocoTaskExtension> {
            includes = listOf("io.github.dzirbel.*")
        }
        testLogging {
            events(
                TestLogEvent.FAILED,
                TestLogEvent.SKIPPED,
                TestLogEvent.STANDARD_OUT,
                TestLogEvent.STANDARD_ERROR,
            )
            exceptionFormat = TestExceptionFormat.FULL
            showCauses = true
            showExceptions = true
            showStackTraces = true
            showStandardStreams = true
        }
    }
}
