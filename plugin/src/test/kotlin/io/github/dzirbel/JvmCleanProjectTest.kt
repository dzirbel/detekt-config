package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertContains

class JvmCleanProjectTest : SampleProjectTest("jvm-clean") {

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
