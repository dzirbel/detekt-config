package io.github.dzirbel

import org.gradle.testkit.runner.BuildResult
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class KmpComposeProjectTest : SampleProjectTest("kmp-compose") {
    private val targets = listOf("Jvm", "Js", "WasmJs")
    private val composeFile = projectDir.resolve("src/commonMain/kotlin/io/github/dzirbel/SampleCompose.kt")
    private val composeTestFile = projectDir.resolve("src/commonTest/kotlin/io/github/dzirbel/SampleComposeTest.kt")
    private val modifierExplanation =
        "See https://mrmans0n.github.io/compose-rules/rules/#modifiers-should-have-default-parameters " +
            "for more information. [ModifierWithoutDefault]"

    @Test
    fun `Compose Multiplatform sources compile for JVM JS and Wasm`() {
        val result = projectDir.gradle("assemble").build()

        targets.forEach { target -> assertTaskPassed(result, ":kmp-compose:compileKotlin$target") }
    }

    @Test
    fun `check applies Compose rules to every main and test compilation`() {
        // Browser runners are excluded because this fixture verifies compilation and static analysis.
        val result = projectDir.gradle("check", "-x", "jsBrowserTest", "-x", "wasmJsBrowserTest").buildAndFail()

        assertComposeFindings(result)
        assertTaskPassed(result, ":kmp-compose:jvmTest")
        assertTestsExecuted(projectDir, "jvmTest")
        assertTaskNotRun(result, ":kmp-compose:check")
    }

    @Test
    fun `detekt applies Compose rules to every main and test compilation`() {
        val result = projectDir.gradle("detekt").buildAndFail()

        assertComposeFindings(result)
        assertTaskNotRun(result, ":kmp-compose:detekt")
    }

    @Test
    fun `both Compose plugins add the Compose rules dependency exactly once`() {
        val result = projectDir.gradle("dependencies", "--configuration", "detektPlugins").build()

        assertContains(result.output, "io.nlopez.compose.rules:detekt:")
        assertEquals(1, Regex("io\\.nlopez\\.compose\\.rules:detekt:").findAll(result.output).count())
    }

    @Test
    fun `corrected Compose sources pass check on every target`() {
        // Browser test sources are still compiled and analyzed by the detekt tasks.
        val result = projectDir.gradle("check", "-Pcorrected", "-x", "jsBrowserTest", "-x", "wasmJsBrowserTest").build()

        assertCleanAnalysis(result)
        assertTaskPassed(result, ":kmp-compose:jvmTest")
        assertTestsExecuted(projectDir, "jvmTest")
        assertTaskPassed(result, ":kmp-compose:check")
    }

    @Test
    fun `Compose Multiplatform analysis reuses the configuration cache`() {
        val arguments = arrayOf("detekt", "-Pcorrected", "--configuration-cache", "--configuration-cache-problems=fail")

        val first = projectDir.gradle(*arguments).build()
        assertContains(first.output, "Configuration cache entry stored.")
        assertCleanAnalysis(first)

        val cached = projectDir.gradle(*arguments).build()
        assertContains(cached.output, "Reusing configuration cache.")
        assertCleanAnalysis(cached)
    }

    private fun assertComposeFindings(result: BuildResult) {
        assertFailedTasks(
            result,
            *targets.flatMap { target ->
                listOf(":kmp-compose:detektMain$target", ":kmp-compose:detektTest$target")
            }.toTypedArray(),
        )
        targets.forEach { target ->
            assertTaskPassed(result, ":kmp-compose:compileKotlin$target")
            assertTaskPassed(result, ":kmp-compose:compileTestKotlin$target")
            assertEquals(
                listOf(
                    "${composeFile.absolutePath}:7:14: This @Composable function has a modifier parameter " +
                        "but it doesn't have a default value.",
                    modifierExplanation,
                    "${composeFile.absolutePath}:12:1: This @Composable declaration does not use composition " +
                        "and should not be marked @Composable.",
                    "See https://mrmans0n.github.io/compose-rules/rules/" +
                        "#do-not-mark-functions-as-composable-when-they-dont-need-it " +
                        "for more information. [UnnecessaryComposable]",
                ),
                assertTaskFailed(result, ":kmp-compose:detektMain$target"),
            )
            assertEquals(
                listOf(
                    "${composeTestFile.absolutePath}:9:18: This @Composable function has a modifier parameter " +
                        "but it doesn't have a default value.",
                    modifierExplanation,
                ),
                assertTaskFailed(result, ":kmp-compose:detektTest$target"),
            )
        }
    }

    private fun assertCleanAnalysis(result: BuildResult) {
        targets.forEach { target ->
            assertTaskPassed(result, ":kmp-compose:compileKotlin$target")
            assertTaskPassed(result, ":kmp-compose:compileTestKotlin$target")
            assertDetektTaskPassed(result, ":kmp-compose:detektMain$target")
            assertDetektTaskPassed(result, ":kmp-compose:detektTest$target")
        }
        assertTaskNoSource(result, ":kmp-compose:detekt")
    }
}
