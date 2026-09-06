package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.gradle.testkit.runner.TaskOutcome

class JvmCleanProjectTest : SampleProjectTest("jvm-clean") {

    @Test
    fun `project overrides track edits and reuse configuration and build caches`() {
        projectDir.resolve("build.gradle.kts").appendText(
            "\ndetektConfig { config.from(\"project-detekt.yml\") }\n",
        )
        val config = projectDir.resolve("project-detekt.yml")
        config.writeText("style:\n  MagicNumber:\n    active: false\n")
        projectDir.resolve("src/main/kotlin/io/github/dzirbel/Sample.kt").appendText(
            "\nfun magicNumber(value: Int): Int = value + 42\n",
        )
        val arguments = arrayOf("detekt", "--configuration-cache", "--configuration-cache-problems=fail", "--build-cache")
        val first = projectDir.gradle(*arguments).build()
        assertDetektTaskPassed(first, ":jvm-clean:detektMain")
        val second = projectDir.gradle(*arguments).build()
        assertContains(second.output, "Reusing configuration cache.")
        assertEquals(TaskOutcome.UP_TO_DATE, second.task(":jvm-clean:generateDetektConfig")?.outcome)
        projectDir.resolve("build/detekt/config.yml").delete()
        val restored = projectDir.gradle(*arguments).build()
        assertEquals(TaskOutcome.FROM_CACHE, restored.task(":jvm-clean:generateDetektConfig")?.outcome)
        projectDir.resolve("build.gradle.kts").appendText(
            "\ndetektConfig { forbiddenMethodCalls.set(emptyList()) }\n",
        )
        val extensionChanged = projectDir.gradle(*arguments).build()
        assertEquals(TaskOutcome.SUCCESS, extensionChanged.task(":jvm-clean:generateDetektConfig")?.outcome)
        config.writeText("style:\n  MagicNumber:\n    active: true\n")
        val changed = projectDir.gradle(*arguments).buildAndFail()
        assertEquals(TaskOutcome.SUCCESS, changed.task(":jvm-clean:generateDetektConfig")?.outcome)
        assertContains(changed.output, "MagicNumber")
    }

    @Test
    fun `compilation succeeds`() {
        projectDir.gradle("assemble").build()
    }

    @Test
    fun `tests succeed`() {
        val result = projectDir.gradle("test").build()

        assertTaskPassed(result, ":jvm-clean:test")
        assertTestsExecuted(projectDir, "test")
    }

    @Test
    fun `check succeeds`() {
        val result = projectDir.gradle("check").build()

        assertTaskPassed(result, ":jvm-clean:compileKotlin")
        assertTaskPassed(result, ":jvm-clean:compileTestKotlin")
        assertDetektTaskPassed(result, ":jvm-clean:detektMain")
        assertDetektTaskPassed(result, ":jvm-clean:detektTest")
        assertTaskPassed(result, ":jvm-clean:check")
    }

    @Test
    fun `detekt succeeds`() {
        val result = projectDir.gradle("detekt").build()

        assertTaskPassed(result, ":jvm-clean:compileKotlin")
        assertTaskPassed(result, ":jvm-clean:compileTestKotlin")
        assertDetektTaskPassed(result, ":jvm-clean:detektMain")
        assertDetektTaskPassed(result, ":jvm-clean:detektTest")
        assertTaskNoSource(result, ":jvm-clean:detekt")
    }

    @Test
    fun `detekt respects Kotlin source set exclusions`() {
        projectDir.resolve("build.gradle.kts").appendText(
            "\nkotlin.sourceSets.named(\"main\") { kotlin.exclude(\"**/Excluded.kt\") }\n",
        )
        projectDir.resolve("src/main/kotlin/io/github/dzirbel/Excluded.kt").writeText(
            """
            package io.github.dzirbel

            fun excluded() {
                println("This file is excluded from the compilation.")
            }
            """.trimIndent() + "\n",
        )

        val result = projectDir.gradle("detekt").build()

        assertTaskPassed(result, ":jvm-clean:compileKotlin")
        assertDetektTaskPassed(result, ":jvm-clean:detektMain")
    }

    @Test
    fun `generate config creates an editable project file`() {
        val result = projectDir.gradle("detektGenerateConfig").build()

        assertTaskPassed(result, ":jvm-clean:detektGenerateConfig")
        assertContains(projectDir.parentFile.resolve("config/detekt/detekt.yml").readText(), "MagicNumber:")
    }
}
