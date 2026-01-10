package io.github.dzirbel

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.Test

class JsProjectTest {

    private val projectDir = File("src/test/resources/js")
    private val sampleFile = projectDir.resolve("src/commonMain/kotlin/io/github/dzirbel/Sample.kt")

    @Test
    fun `check fails`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("check")
            .buildAndFail()

        assertTaskPassed(result, ":js:compileKotlinJs")
        assertTaskNotRun(result, ":js:check")

        val output = assertTaskFailed(result, ":js:detektJsMain")
        assertSameContents(expectedWarnings(sampleFile), output)
    }

    @Test
    fun `detekt fails`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("detekt")
            .buildAndFail()

        assertTaskPassed(result, ":js:compileKotlinJs")
        assertTaskNotRun(result, ":js:detekt")

        val output = assertTaskFailed(result, ":js:detektJsMain")
        assertSameContents(expectedWarnings(sampleFile), output)
    }
}
