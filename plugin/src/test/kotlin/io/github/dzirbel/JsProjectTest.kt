package io.github.dzirbel

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.Test

class JsProjectTest {

    private val projectDir = File("src/test/resources/js")
    private val sampleFile = projectDir.resolve("src/commonMain/kotlin/io/github/dzirbel/Sample.kt")
    private val commonTestFile = projectDir.resolve("src/commonTest/kotlin/io/github/dzirbel/SampleCommonTest.kt")
    private val jsTestFile = projectDir.resolve("src/jsTest/kotlin/io/github/dzirbel/SampleJsTest.kt")

    @Test
    fun `compilation succeeds`() {
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("assemble")
            .build()
    }

    @Test
    fun `check fails`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("check", "--continue")
            .buildAndFail()

        assertTaskPassed(result, ":js:compileKotlinJs")
        assertTaskPassed(result, ":js:compileTestKotlinJs")
        assertTaskNotRun(result, ":js:check")

        // tests fail because browsers are not installed
        assertFailedTasks(result, ":js:detektJsMain", ":js:detektJsTest", ":js:jsBrowserTest")

        val mainOutput = assertTaskFailed(result, ":js:detektJsMain")
        val testOutput = assertTaskFailed(result, ":js:detektJsTest")
        assertSameContents(expectedWarnings(sampleFile), mainOutput)
        assertSameContents(expectedTestWarnings(commonTestFile, jsTestFile), testOutput)
    }

    @Test
    fun `detekt fails`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("detekt", "--continue")
            .buildAndFail()

        assertTaskPassed(result, ":js:compileKotlinJs")
        assertTaskPassed(result, ":js:compileTestKotlinJs")
        assertTaskNotRun(result, ":js:detekt")
        assertFailedTasks(result, ":js:detektJsMain", ":js:detektJsTest")

        val mainOutput = assertTaskFailed(result, ":js:detektJsMain")
        val testOutput = assertTaskFailed(result, ":js:detektJsTest")
        assertSameContents(expectedWarnings(sampleFile), mainOutput)
        assertSameContents(expectedTestWarnings(commonTestFile, jsTestFile), testOutput)
    }
}
