package io.github.dzirbel

import java.io.File
import kotlin.test.Test

class JvmProjectTest {

    private val projectDir = File("src/test/resources/jvm")
    private val sampleFile = projectDir.resolve("src/main/kotlin/io/github/dzirbel/Sample.kt")
    private val testFile = projectDir.resolve("src/test/kotlin/io/github/dzirbel/SampleTest.kt")

    @Test
    fun `compilation succeeds`() {
        projectDir.gradle("assemble").build()
    }

    @Test
    fun `tests succeed`() {
        projectDir.gradle("test").build()
    }

    @Test
    fun `check fails`() {
        val result = projectDir.gradle("check").buildAndFail()

        assertTaskPassed(result, ":jvm:compileKotlin")
        assertTaskPassed(result, ":jvm:compileTestKotlin")
        assertTaskNotRun(result, ":jvm:check")
        assertFailedTasks(result, ":jvm:detektMain", ":jvm:detektTest")

        val mainOutput = assertTaskFailed(result, ":jvm:detektMain")
        val testOutput = assertTaskFailed(result, ":jvm:detektTest")
        assertSameContents(expectedWarnings(sampleFile), mainOutput)
        assertSameContents(expectedTestWarnings(testFile), testOutput)
    }

    @Test
    fun `detekt fails`() {
        val result = projectDir.gradle("detekt").buildAndFail()

        assertTaskPassed(result, ":jvm:compileKotlin")
        assertTaskPassed(result, ":jvm:compileTestKotlin")
        assertTaskNotRun(result, ":jvm:detekt")
        assertFailedTasks(result, ":jvm:detektMain", ":jvm:detektTest")

        val mainOutput = assertTaskFailed(result, ":jvm:detektMain")
        val testOutput = assertTaskFailed(result, ":jvm:detektTest")
        assertSameContents(expectedWarnings(sampleFile), mainOutput)
        assertSameContents(expectedTestWarnings(testFile), testOutput)
    }
}
