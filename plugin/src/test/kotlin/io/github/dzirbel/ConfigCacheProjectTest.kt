package io.github.dzirbel

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigCacheProjectTest : SampleProjectTest("jvm-clean") {

    private val buildFile = projectDir.resolve("build.gradle.kts")
    private val generatedConfig = projectDir.resolve("build/detekt/config.yml")
    private val arguments = arrayOf(
        "detektMain", "--configuration-cache", "--configuration-cache-problems=fail", "--build-cache",
    )
    private val source = projectDir.resolve("src/main/kotlin/io/github/dzirbel/ConfigProbe.kt").apply {
        writeText("package io.github.dzirbel\n\nfun configProbe() {\n    println(42)\n}\n")
    }
    private val printWarning = "${source.absolutePath}:4:5: " +
        "The method `kotlin.io.println` has been forbidden: " +
        "println does not allow you to configure the output stream. Use a logger instead. [ForbiddenMethodCall]"
    private val magicWarning = "${source.absolutePath}:4:13: " +
        "This expression contains a magic number. Consider defining it to a well named constant. [MagicNumber]"

    init {
        // Make cache assertions independent of earlier tests and previous invocations of the suite.
        projectDir.parentFile.resolve("settings.gradle.kts").appendText(
            "\nbuildCache { local { directory = file(\"test-build-cache\") } }\n",
        )
    }

    @Test
    fun `config edits and extension changes invalidate analysis while unchanged output is reusable`() {
        buildFile.appendText("\ndetektConfig { config.from(\"project-detekt.yml\") }\n")
        val config = projectDir.resolve("project-detekt.yml").apply {
            writeText("style:\n  MagicNumber:\n    active: false\n")
        }

        // Disabling one rule must retain the unrelated, type-resolved println prohibition.
        val initial = projectDir.gradle(*arguments).buildAndFail()
        assertGenerated(initial, TaskOutcome.SUCCESS)
        assertFindings(initial, printWarning)

        buildFile.appendText("\ndetektConfig { forbiddenMethodCalls.set(emptyList()) }\n")
        val allowed = projectDir.gradle(*arguments).build()
        assertGenerated(allowed, TaskOutcome.SUCCESS)
        assertAnalysis(allowed, TaskOutcome.SUCCESS)
        val allowedConfig = generatedConfig.readText()

        val unchanged = projectDir.gradle(*arguments).build()
        assertContains(unchanged.output, "Reusing configuration cache.")
        assertGenerated(unchanged, TaskOutcome.UP_TO_DATE)
        assertAnalysis(unchanged, TaskOutcome.UP_TO_DATE)

        assertTrue(generatedConfig.delete())
        val restored = projectDir.gradle(*arguments).build()
        assertContains(restored.output, "Reusing configuration cache.")
        assertGenerated(restored, TaskOutcome.FROM_CACHE)
        assertAnalysis(restored, TaskOutcome.UP_TO_DATE)
        assertEquals(allowedConfig, generatedConfig.readText())

        config.writeText("style:\n  MagicNumber:\n    active: true\n")
        val changed = projectDir.gradle(*arguments).buildAndFail()
        assertContains(changed.output, "Reusing configuration cache.")
        assertGenerated(changed, TaskOutcome.SUCCESS)
        assertFindings(changed, magicWarning)
    }

    @Test
    fun `reordering unchanged override files invalidates cache and reversing order restores correct policy`() {
        projectDir.resolve("strict.yml").writeText("style:\n  ForbiddenMethodCall:\n    methods: []\n")
        projectDir.resolve("relaxed.yml").writeText("style:\n  MagicNumber:\n    active: false\n")
        // Both files override the same scalar; only the later file should win.
        projectDir.resolve("strict.yml").appendText("  MagicNumber:\n    active: true\n")
        val originalBuild = buildFile.readText()
        fun setOrder(first: String, second: String) {
            buildFile.writeText(originalBuild + "\ndetektConfig { config.from(\"$first.yml\", \"$second.yml\") }\n")
        }

        setOrder("strict", "relaxed")
        val relaxed = projectDir.gradle(*arguments).build()
        assertGenerated(relaxed, TaskOutcome.SUCCESS)
        assertAnalysis(relaxed, TaskOutcome.SUCCESS)
        val relaxedConfig = generatedConfig.readText()

        setOrder("relaxed", "strict")
        val strict = projectDir.gradle(*arguments).buildAndFail()
        assertGenerated(strict, TaskOutcome.SUCCESS)
        assertFindings(strict, magicWarning)

        setOrder("strict", "relaxed")
        val restored = projectDir.gradle(*arguments).build()
        assertGenerated(restored, TaskOutcome.FROM_CACHE)
        assertAnalysis(restored, TaskOutcome.FROM_CACHE)
        assertEquals(relaxedConfig, generatedConfig.readText())
    }

    private fun assertGenerated(result: BuildResult, outcome: TaskOutcome) {
        assertTaskPassed(result, ":jvm-clean:generateDetektConfig")
        assertEquals(outcome, result.task(":jvm-clean:generateDetektConfig")?.outcome)
    }

    private fun assertAnalysis(result: BuildResult, outcome: TaskOutcome) {
        assertDetektTaskPassed(result, ":jvm-clean:detektMain")
        assertEquals(outcome, result.task(":jvm-clean:detektMain")?.outcome)
    }

    private fun assertFindings(result: BuildResult, vararg findings: String) {
        assertFailedTasks(result, ":jvm-clean:detektMain")
        assertSameContents(findings.toList(), assertTaskFailed(result, ":jvm-clean:detektMain"))
    }
}
