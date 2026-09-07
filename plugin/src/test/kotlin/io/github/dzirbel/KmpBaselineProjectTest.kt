package io.github.dzirbel

import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KmpBaselineProjectTest : SampleProjectTest("kmp-baseline") {

    @Test
    fun `shared baseline suppresses only existing findings on JVM and JS and tracks edits`() {
        val existing = projectDir.resolve("src/commonMain/kotlin/io/github/dzirbel/ExistingFindings.kt")
        val taskPaths = arrayOf(":kmp-baseline:detektMainJvm", ":kmp-baseline:detektMainJs")
        val arguments = taskPaths + arrayOf("--configuration-cache", "--configuration-cache-problems=fail")

        fun assertFindings(arguments: Array<String>, vararg files: File) {
            val result = projectDir.gradle(*arguments).buildAndFail()
            assertFailedTasks(result, *taskPaths)
            taskPaths.forEach { path ->
                assertSameContents(expectedWarnings(*files), assertTaskFailed(result, path))
            }
        }

        // Prove both compilations report the syntax and type-dependent findings before suppressing them.
        assertFindings(arguments, existing)
        val baselineResult = projectDir.gradle("detektBaselineMainJvm").build()
        assertTaskPassed(baselineResult, ":kmp-baseline:detektBaselineMainJvm")
        val baseline = projectDir.resolve("build/baseline.xml")
        assertTrue(baseline.isFile)
        val baselineContents = baseline.readText()
        val issuePattern = Regex("<ID>(.*?)</ID>")
        assertSameContents(
            listOf("VarCouldBeVal", "DoubleMutabilityForCollection", "ForbiddenMethodCall"),
            issuePattern.findAll(baselineContents).map { it.groupValues[1].substringBefore(':') }.toList(),
        )
        val suppressed = projectDir.gradle(*arguments).build()
        taskPaths.forEach { path ->
            assertDetektTaskPassed(suppressed, path)
            assertEquals(TaskOutcome.SUCCESS, suppressed.task(path)?.outcome)
        }

        val added = projectDir.resolve("src/newFindings/kotlin/io/github/dzirbel/NewFindings.kt")
        val withNewFindings = arguments + "-PincludeNewFindings"
        assertFindings(withNewFindings, added)

        // Change only the baseline, leaving sources and build configuration untouched.
        projectDir.gradle("clearBaseline").build()
        assertFindings(withNewFindings, existing, added)
    }
}
