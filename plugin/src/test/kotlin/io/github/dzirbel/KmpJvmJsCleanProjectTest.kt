package io.github.dzirbel

import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KmpJvmJsCleanProjectTest : SampleProjectTest("kmp-jvm-js-clean") {

    @Test
    fun `compilation succeeds`() {
        projectDir.gradle("assemble").build()
    }

    @Test
    fun `jvm tests succeed`() {
        val result = projectDir.gradle("jvmTest").build()

        assertTaskPassed(result, ":kmp-jvm-js-clean:jvmTest")
        assertTestsExecuted(projectDir, "jvmTest")
    }

    @Test
    fun `js tests succeed`() {
        val result = projectDir.gradle("jsNodeTest").build()

        assertTaskPassed(result, ":kmp-jvm-js-clean:jsNodeTest")
        assertTestsExecuted(projectDir, "jsNodeTest")
    }

    @Test
    fun `check succeeds`() {
        val result = projectDir.gradle("check").build()

        assertTaskPassed(result, ":kmp-jvm-js-clean:compileKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileKotlinJs")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileTestKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileTestKotlinJs")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileIntegrationTestKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileIntegrationTestKotlinJs")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektMainJvm")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektMainJs")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektTestJvm")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektTestJs")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektIntegrationTestJvm")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektIntegrationTestJs")
        assertTaskNoSource(result, ":kmp-jvm-js-clean:detekt")
        assertTaskPassed(result, ":kmp-jvm-js-clean:check")
    }

    @Test
    fun `detekt succeeds`() {
        val result = projectDir.gradle("detekt").build()

        assertTaskPassed(result, ":kmp-jvm-js-clean:compileKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileKotlinJs")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileTestKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileTestKotlinJs")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileIntegrationTestKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileIntegrationTestKotlinJs")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektMainJvm")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektMainJs")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektTestJvm")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektTestJs")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektIntegrationTestJvm")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektIntegrationTestJs")
        assertTaskNoSource(result, ":kmp-jvm-js-clean:detekt")
    }

    @Test
    fun `detekt reuses the configuration cache`() {
        val arguments = arrayOf("detekt", "--configuration-cache", "--configuration-cache-problems=fail")

        val firstResult = projectDir.gradle(*arguments).build()
        assertContains(firstResult.output, "Configuration cache entry stored.")

        val secondResult = projectDir.gradle(*arguments).build()
        assertContains(secondResult.output, "Reusing configuration cache.")
        assertTaskNoSource(secondResult, ":kmp-jvm-js-clean:detekt")
    }

    @Test
    fun `shared baseline suppresses only existing findings on JVM and JS and tracks edits`() {
        fun writeViolations(name: String): File =
            projectDir.resolve("src/commonMain/kotlin/io/github/dzirbel/$name.kt").apply {
                writeText(
                    """
                    package io.github.dzirbel

                    fun ${name.replaceFirstChar(Char::lowercase)}() {
                        var x = mutableSetOf<String>()
                        println("Hello ${'$'}x")
                    }
                    """.trimIndent() + "\n",
                )
            }

        val existing = writeViolations("ExistingFindings")
        val taskPaths = arrayOf(":kmp-jvm-js-clean:detektMainJvm", ":kmp-jvm-js-clean:detektMainJs")
        val arguments = taskPaths + arrayOf("--configuration-cache", "--configuration-cache-problems=fail")

        fun assertFindings(vararg files: File) {
            val result = projectDir.gradle(*arguments).buildAndFail()
            assertFailedTasks(result, *taskPaths)
            taskPaths.forEach { path ->
                assertSameContents(expectedWarnings(*files), assertTaskFailed(result, path))
            }
        }

        // Prove both compilations report the syntax and type-dependent findings before suppressing them.
        assertFindings(existing)
        val baselineResult = projectDir.gradle("detektBaselineMainJvm").build()
        assertTaskPassed(baselineResult, ":kmp-jvm-js-clean:detektBaselineMainJvm")
        val baseline = projectDir.resolve("detekt-baseline-main.xml")
        assertTrue(baseline.isFile)
        val baselineContents = baseline.readText()
        val issuePattern = Regex("<ID>(.*?)</ID>")
        assertSameContents(
            listOf("VarCouldBeVal", "DoubleMutabilityForCollection", "ForbiddenMethodCall"),
            issuePattern.findAll(baselineContents).map { it.groupValues[1].substringBefore(':') }.toList(),
        )
        projectDir.resolve("build.gradle.kts").appendText(
            "\ndetekt { baseline.set(layout.projectDirectory.file(\"detekt-baseline-main.xml\")) }\n",
        )

        val suppressed = projectDir.gradle(*arguments).build()
        taskPaths.forEach { path ->
            assertDetektTaskPassed(suppressed, path)
            assertEquals(TaskOutcome.SUCCESS, suppressed.task(path)?.outcome)
        }

        val added = writeViolations("NewFindings")
        assertFindings(added)

        // Change only the baseline, leaving sources and build configuration untouched.
        baseline.writeText(baselineContents.replace(issuePattern, ""))
        assertFindings(existing, added)
    }
}
