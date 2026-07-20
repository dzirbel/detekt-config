import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.util.Properties

val versionsFile = layout.projectDirectory.file("plugin/src/main/resources/versions.properties")
val versions = providers.fileContents(versionsFile).asText
    .map { text ->
        Properties().apply { load(text.byteInputStream()) }
    }
    .get()
extra["versions"] = versions

subprojects {
    group = "io.github.dzirbel"

    version = versions.getProperty("rules")

    tasks.withType<Test>().configureEach {
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
