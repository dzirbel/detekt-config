package io.github.dzirbel

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.Test

class JvmProjectTest {

    private val projectDir = File("src/test/resources/jvm")
    private val sampleFile = projectDir.resolve("src/main/kotlin/io/github/dzirbel/Sample.kt")

    @Test
    fun `check fails`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("check")
            .buildAndFail()

        assertTaskPassed(result, ":jvm:compileKotlin")
        assertTaskNotRun(result, ":jvm:check")

        val output = assertTaskFailed(result, ":jvm:detektMain")
        assertSameContents(expectedWarnings(sampleFile), output)
    }

    @Test
    fun `detekt fails`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("detekt")
            .buildAndFail()

        assertTaskPassed(result, ":jvm:compileKotlin")
        assertTaskNotRun(result, ":jvm:detekt")

        val output = assertTaskFailed(result, ":jvm:detektMain")
        assertSameContents(expectedWarnings(sampleFile), output)
    }
}
