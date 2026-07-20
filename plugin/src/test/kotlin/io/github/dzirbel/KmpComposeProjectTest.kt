package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertEquals

class KmpComposeProjectTest : SampleProjectTest("kmp-compose") {

    private val composeFile = projectDir.resolve("src/commonMain/kotlin/io/github/dzirbel/SampleCompose.kt")

    @Test
    fun `compilation succeeds`() {
        projectDir.gradle("assemble").build()
    }

    @Test
    fun `check fails`() {
        val result = projectDir.gradle("check").buildAndFail()

        assertTaskPassed(result, ":kmp-compose:compileKotlinJvm")
        assertTaskNoSource(result, ":kmp-compose:compileTestKotlinJvm")
        assertTaskNotRun(result, ":kmp-compose:check")
        assertFailedTasks(result, ":kmp-compose:detektJvmMain")
        assertTaskNoSource(result, ":kmp-compose:detektJvmTest")
        assertComposeOutput(assertTaskFailed(result, ":kmp-compose:detektJvmMain"))
    }

    @Test
    fun `detekt applies compose rules`() {
        val result = projectDir.gradle("detekt").buildAndFail()

        assertTaskPassed(result, ":kmp-compose:compileKotlinJvm")
        assertTaskNotRun(result, ":kmp-compose:detekt")
        assertFailedTasks(result, ":kmp-compose:detektJvmMain")
        assertTaskNoSource(result, ":kmp-compose:detektJvmTest")
        assertComposeOutput(assertTaskFailed(result, ":kmp-compose:detektJvmMain"))
    }

    private fun assertComposeOutput(output: List<String>) {
        assertEquals(
            listOf(
                "${composeFile.absolutePath}:7:14: This @Composable function has a modifier parameter but it doesn't have a default value.",
                "See https://mrmans0n.github.io/compose-rules/rules/#modifiers-should-have-default-parameters for more information. [ModifierWithoutDefault]",
                "${composeFile.absolutePath}:12:1: This @Composable declaration does not use composition and should not be marked @Composable.",
                "See https://mrmans0n.github.io/compose-rules/rules/#do-not-mark-functions-as-composable-when-they-dont-need-it for more information. [UnnecessaryComposable]",
            ),
            output,
        )
    }
}
