package io.github.dzirbel

import java.io.File
import kotlin.test.Test

class JvmCustomProjectTest {

    private val projectDir = File("src/test/resources/jvm-custom")
    private val sampleFile = projectDir.resolve("src/main/kotlin/io/github/dzirbel/Sample.kt")
    private val testFile = projectDir.resolve("src/test/kotlin/io/github/dzirbel/SampleTest.kt")
    private val integrationTestFile =
        projectDir.resolve("src/integrationTest/kotlin/io/github/dzirbel/SampleIntegrationTest.kt")

    @Test
    fun `detekt fails on custom compilation`() {
        val result = projectDir.gradle("detekt").buildAndFail()

        assertTaskPassed(result, ":jvm-custom:compileKotlin")
        assertTaskPassed(result, ":jvm-custom:compileTestKotlin")
        assertTaskPassed(result, ":jvm-custom:compileIntegrationTestKotlin")
        assertTaskNotRun(result, ":jvm-custom:detekt")
        assertFailedTasks(
            result,
            ":jvm-custom:detektMain",
            ":jvm-custom:detektTest",
            ":jvm-custom:detektIntegrationTest",
        )

        val mainOutput = assertTaskFailed(result, ":jvm-custom:detektMain")
        val testOutput = assertTaskFailed(result, ":jvm-custom:detektTest")
        val integrationOutput = assertTaskFailed(result, ":jvm-custom:detektIntegrationTest")
        assertSameContents(expectedWarnings(sampleFile), mainOutput)
        assertSameContents(expectedTestWarnings(testFile), testOutput)
        assertSameContents(expectedWarnings(integrationTestFile), integrationOutput)
    }
}
