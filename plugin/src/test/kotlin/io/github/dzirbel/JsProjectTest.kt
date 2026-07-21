package io.github.dzirbel

import kotlin.test.Test

class JsProjectTest : SampleProjectTest("js") {

    private val sampleFile = projectDir.resolve("src/commonMain/kotlin/io/github/dzirbel/Sample.kt")
    private val commonTestFile = projectDir.resolve("src/commonTest/kotlin/io/github/dzirbel/SampleCommonTest.kt")
    private val jsTestFile = projectDir.resolve("src/jsTest/kotlin/io/github/dzirbel/SampleJsTest.kt")

    @Test
    fun `compilation succeeds`() {
        projectDir.gradle("assemble").build()
    }

    @Test
    fun `tests succeed`() {
        val result = projectDir.gradle("jsNodeTest").build()

        assertTaskPassed(result, ":js:jsNodeTest")
        assertTestsExecuted(projectDir, "jsNodeTest")
    }

    @Test
    fun `check fails`() {
        val result = projectDir.gradle("check").buildAndFail()

        assertTaskPassed(result, ":js:compileKotlinJs")
        assertTaskPassed(result, ":js:compileTestKotlinJs")
        assertTaskNotRun(result, ":js:check")
        assertFailedTasks(result, ":js:detektMainJs", ":js:detektTestJs")

        val mainOutput = assertTaskFailed(result, ":js:detektMainJs")
        val testOutput = assertTaskFailed(result, ":js:detektTestJs")
        assertSameContents(expectedWarnings(sampleFile) + expectedExternalDependencyWarning(sampleFile), mainOutput)
        assertSameContents(
            expectedTestWarnings(commonTestFile, jsTestFile) + expectedExternalDependencyWarning(jsTestFile),
            testOutput,
        )
    }

    @Test
    fun `detekt fails`() {
        val result = projectDir.gradle("detekt").buildAndFail()

        assertTaskPassed(result, ":js:compileKotlinJs")
        assertTaskPassed(result, ":js:compileTestKotlinJs")
        assertTaskNotRun(result, ":js:detekt")
        assertFailedTasks(result, ":js:detektMainJs", ":js:detektTestJs")

        val mainOutput = assertTaskFailed(result, ":js:detektMainJs")
        val testOutput = assertTaskFailed(result, ":js:detektTestJs")
        assertSameContents(expectedWarnings(sampleFile) + expectedExternalDependencyWarning(sampleFile), mainOutput)
        assertSameContents(
            expectedTestWarnings(commonTestFile, jsTestFile) + expectedExternalDependencyWarning(jsTestFile),
            testOutput,
        )
    }
}
