package io.github.dzirbel

import java.io.File
import kotlin.test.Test

class JvmDependenciesProjectTest {

    private val projectDir = File("src/test/resources/jvm-deps")
    private val sampleFile = projectDir.resolve("src/main/kotlin/io/github/dzirbel/Sample.kt")

    @Test
    fun `detekt resolves external dependencies`() {
        val result = projectDir.gradle("detekt").buildAndFail()

        assertTaskPassed(result, ":jvm-deps:compileKotlin")
        assertTaskNotRun(result, ":jvm-deps:detekt")

        val mainOutput = assertTaskFailed(result, ":jvm-deps:detektMain")
        val expected = expectedWarnings(sampleFile) + listOf(
            "${sampleFile.absolutePath}:6:24: The method `kotlinx.coroutines.runBlocking` has been forbidden: " +
                "runBlocking blocks threads. Use a suspend function instead. [ForbiddenMethodCall]",
        )
        assertSameContents(expected, mainOutput)
    }
}
