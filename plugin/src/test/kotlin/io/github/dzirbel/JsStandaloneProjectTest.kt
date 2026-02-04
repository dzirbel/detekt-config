package io.github.dzirbel

import java.io.File
import kotlin.test.Test

class JsStandaloneProjectTest {

    private val projectDir = File("src/test/resources/js-standalone")
    private val sampleFile = projectDir.resolve("src/main/kotlin/io/github/dzirbel/Sample.kt")
    private val testFile = projectDir.resolve("src/test/kotlin/io/github/dzirbel/SampleTest.kt")

    @Test
    fun `detekt fails`() {
        val result = projectDir.gradle("detekt").buildAndFail()

        assertTaskPassed(result, ":js-standalone:compileKotlinJs")
        assertTaskPassed(result, ":js-standalone:compileTestKotlinJs")
        assertTaskNotRun(result, ":js-standalone:detekt")
        assertFailedTasks(result, ":js-standalone:detektMain", ":js-standalone:detektTest")

        val mainOutput = assertTaskFailed(result, ":js-standalone:detektMain")
        val testOutput = assertTaskFailed(result, ":js-standalone:detektTest")
        assertSameContents(expectedWarnings(sampleFile), mainOutput)
        assertSameContents(expectedTestWarnings(testFile, compilerErrors = 4), testOutput)
    }
}
