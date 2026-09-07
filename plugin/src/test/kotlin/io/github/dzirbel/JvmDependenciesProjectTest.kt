package io.github.dzirbel

import kotlin.test.Test

class JvmDependenciesProjectTest : SampleProjectTest("jvm-dependencies") {

    private val sampleFile = projectDir.resolve("src/main/kotlin/io/github/dzirbel/Sample.kt")

    @Test
    fun `detekt resolves external dependencies`() {
        val result = projectDir.gradle("detekt").buildAndFail()

        assertTaskPassed(result, ":jvm-dependencies:compileKotlin")
        assertTaskNotRun(result, ":jvm-dependencies:detekt")

        val mainOutput = assertTaskFailed(result, ":jvm-dependencies:detektMain")
        val expected = expectedWarnings(sampleFile) + listOf(
            "${sampleFile.absolutePath}:6:24: The method `kotlinx.coroutines.runBlocking` has been forbidden: " +
                "runBlocking blocks threads. Use a suspend function instead. [ForbiddenMethodCall]",
            "${sampleFile.absolutePath}:6:5: Fully qualified function call 'kotlinx.coroutines.runBlocking' can be " +
                "replaced with an import. [UnnecessaryFullyQualifiedName]",
        )
        assertSameContents(expected, mainOutput)
    }
}
