package io.github.dzirbel

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ConfigCacheProjectTest : SampleProjectTest("config-cache") {

    private val generatedConfig = projectDir.resolve("build/detekt/config.yml")
    private val arguments = arrayOf(
        "detektMain", "--configuration-cache", "--configuration-cache-problems=fail", "--build-cache",
    )
    private val source = projectDir.resolve("src/main/kotlin/io/github/dzirbel/ConfigProbe.kt")
    private val printWarning = "${source.absolutePath}:4:5: " +
        "The method `kotlin.io.println` has been forbidden: " +
        "println does not allow you to configure the output stream. Use a logger instead. [ForbiddenMethodCall]"
    private val magicWarning = "${source.absolutePath}:4:13: " +
        "This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]"

    @Test
    fun `config edits and extension changes invalidate analysis while unchanged output is reusable`() {
        projectDir.gradle("stageRelaxedConfig").build()

        // Disabling one rule must retain the unrelated, type-resolved println prohibition.
        val initial = projectDir.gradle(*arguments).buildAndFail()
        assertGenerated(initial, TaskOutcome.SUCCESS)
        assertFindings(initial, printWarning)

        val allowedArguments = arguments + "-PallowPrint"
        val allowed = projectDir.gradle(*allowedArguments).build()
        assertGenerated(allowed, TaskOutcome.SUCCESS)
        assertAnalysis(allowed, TaskOutcome.SUCCESS)
        val allowedConfig = generatedConfig.readText()

        val unchanged = projectDir.gradle(*allowedArguments).build()
        assertContains(unchanged.output, "Reusing configuration cache.")
        assertGenerated(unchanged, TaskOutcome.UP_TO_DATE)
        assertAnalysis(unchanged, TaskOutcome.UP_TO_DATE)

        projectDir.gradle("removeGeneratedConfig").build()
        val restored = projectDir.gradle(*allowedArguments).build()
        assertContains(restored.output, "Reusing configuration cache.")
        assertGenerated(restored, TaskOutcome.FROM_CACHE)
        assertAnalysis(restored, TaskOutcome.UP_TO_DATE)
        assertEquals(allowedConfig, generatedConfig.readText())

        projectDir.gradle("stageStrictConfig").build()
        val changed = projectDir.gradle(*allowedArguments).buildAndFail()
        assertContains(changed.output, "Reusing configuration cache.")
        assertGenerated(changed, TaskOutcome.SUCCESS)
        assertFindings(changed, magicWarning)
    }

    @Test
    fun `reordering unchanged override files invalidates cache and reversing order restores correct policy`() {
        val relaxedArguments = arguments + "-PoverrideOrder=strict,relaxed"
        val strictArguments = arguments + "-PoverrideOrder=relaxed,strict"

        val relaxed = projectDir.gradle(*relaxedArguments).build()
        assertGenerated(relaxed, TaskOutcome.SUCCESS)
        assertAnalysis(relaxed, TaskOutcome.SUCCESS)
        val relaxedConfig = generatedConfig.readText()

        val strict = projectDir.gradle(*strictArguments).buildAndFail()
        assertGenerated(strict, TaskOutcome.SUCCESS)
        assertFindings(strict, magicWarning)

        val restored = projectDir.gradle(*relaxedArguments).build()
        assertGenerated(restored, TaskOutcome.FROM_CACHE)
        assertAnalysis(restored, TaskOutcome.FROM_CACHE)
        assertEquals(relaxedConfig, generatedConfig.readText())
    }

    private fun assertGenerated(result: BuildResult, outcome: TaskOutcome) {
        assertTaskPassed(result, ":config-cache:generateDetektConfig")
        assertEquals(outcome, result.task(":config-cache:generateDetektConfig")?.outcome)
    }

    private fun assertAnalysis(result: BuildResult, outcome: TaskOutcome) {
        assertDetektTaskPassed(result, ":config-cache:detektMain")
        assertEquals(outcome, result.task(":config-cache:detektMain")?.outcome)
    }

    private fun assertFindings(result: BuildResult, vararg findings: String) {
        assertFailedTasks(result, ":config-cache:detektMain")
        assertSameContents(findings.toList(), assertTaskFailed(result, ":config-cache:detektMain"))
    }
}
