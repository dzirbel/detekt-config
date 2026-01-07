import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.util.Properties

subprojects {
    group = "io.github.dzirbel"

    val versionsFile = rootProject.layout.projectDirectory.file("plugin/src/main/resources/versions.properties")
    val versions = providers.fileContents(versionsFile).asText
        .map { text ->
            Properties().apply { load(text.byteInputStream()) }
        }
    version = versions.map { it["rules"] }.get()

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
