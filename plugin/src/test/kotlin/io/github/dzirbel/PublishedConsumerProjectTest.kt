package io.github.dzirbel

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.Test

class PublishedConsumerProjectTest : SampleProjectTest("published-consumer") {
    @Test
    fun `published plugin resolves and discovers packaged rules in an isolated JVM consumer`() {
        val repository = projectDir.resolve("build/repository").toURI().toString()
        val publication = GradleRunner.create()
            .withProjectDir(File("..").canonicalFile)
            .withArguments(
                ":plugin:publishAllPublicationsToConsumerTestRepository",
                ":rules:publishAllPublicationsToConsumerTestRepository",
                "--init-script", projectDir.resolve("publish.gradle").absolutePath,
                "-DconsumerTestRepository=$repository",
                "--no-configuration-cache", "--console=plain",
            )
            .build()
        assertTaskPassed(publication, ":plugin:publishDetektConfigPluginMarkerMavenPublicationToConsumerTestRepository")
        assertTaskPassed(publication, ":plugin:publishPluginMavenPublicationToConsumerTestRepository")
        assertTaskPassed(publication, ":rules:publishRulesPublicationToConsumerTestRepository")

        val source = projectDir.resolve("src/main/kotlin/sample/Sample.kt")
        val failing = projectDir.gradle("detekt").buildAndFail()
        assertTaskPassed(failing, ":compileKotlin")
        assertTaskPassed(failing, ":generateDetektConfig")
        assertFailedTasks(failing, ":detektMain")
        assertSameContents(
            listOf(
                "${source.absolutePath}:5:19: Constructor parameters should be in alphabetical order: " +
                    "apple, zebra. [InjectConstructorParameterOrder]",
            ),
            assertTaskFailed(failing, ":detektMain"),
        )

        val corrected = projectDir.gradle("detekt", "-Pcorrected").build()
        assertDetektTaskPassed(corrected, ":detektMain")
        assertTaskNoSource(corrected, ":detekt")
    }
}
