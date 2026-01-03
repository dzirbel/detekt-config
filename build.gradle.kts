import java.util.Properties

subprojects {
    group = "io.github.dzirbel"

    val versionsFile = rootProject.layout.projectDirectory.file("plugin/src/main/resources/versions.properties")
    val versions = providers.fileContents(versionsFile).asText
        .map { text ->
            Properties().apply { load(text.byteInputStream()) }
        }
    version = versions.map { it["detekt-config-rules"] }.get()

    tasks.withType<Test>().configureEach {
        testLogging {
            showCauses = true
            showExceptions = true
            showStackTraces = true
        }
    }
}
