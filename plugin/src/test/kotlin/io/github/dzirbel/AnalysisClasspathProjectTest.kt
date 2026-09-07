package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertContains

class AnalysisClasspathProjectTest : SampleProjectTest("analysis-classpath") {
    private val analysisTasks = arrayOf("detektMainJs", "detektMainNative")

    @Test
    fun `target-only dependencies are skipped with a diagnostic and configuration cache reuse`() {
        val arguments = analysisTasks + arrayOf("--configuration-cache", "--configuration-cache-problems=fail")

        val first = projectDir.gradle(*arguments).build()
        analysisTasks.forEach { task ->
            assertDetektTaskPassed(first, ":analysis-classpath:$task")
            assertContains(first.output, "$task: skipping a dependency with no JVM variant")
        }
        assertContains(first.output, "test.analysis:target-only:1.0")
        assertContains(first.output, "Configuration cache entry stored.")

        val cached = projectDir.gradle(*arguments).build()
        analysisTasks.forEach { assertDetektTaskPassed(cached, ":analysis-classpath:$it") }
        assertContains(cached.output, "Reusing configuration cache.")
    }

    @Test
    fun `missing JVM artifact fails until its publication is repaired`() {
        val arguments = analysisTasks + "-PanalysisDependency=missing-artifact"
        val compilation = projectDir.gradle("compileKotlinJs", "compileKotlinNative").build()
        assertTaskPassed(compilation, ":analysis-classpath:compileKotlinJs")
        assertTaskPassed(compilation, ":analysis-classpath:compileKotlinNative")

        val missingArtifact = projectDir.gradle(*arguments).buildAndFail()
        assertContains(missingArtifact.output, "missing-artifact-1.0.jar")

        projectDir.gradle("restoreArtifact").build()
        val repaired = projectDir.gradle(*arguments).build()
        analysisTasks.forEach { assertDetektTaskPassed(repaired, ":analysis-classpath:$it") }
    }

    @Test
    fun `missing JVM module still fails alongside target-only dependencies`() {
        val result = projectDir.gradle(*analysisTasks, "-PanalysisDependency=missing-module").buildAndFail()

        assertContains(result.output, "Could not find test.analysis:missing-module:1.0")
    }

    @Test
    fun `missing transitive JVM dependency still fails`() {
        val result = projectDir.gradle(*analysisTasks, "-PanalysisDependency=missing-transitive").buildAndFail()

        assertContains(result.output, "Could not find test.analysis:missing-module:1.0")
        assertContains(result.output, "test.analysis:missing-transitive:1.0")
    }

    @Test
    fun `incompatible JVM variants are not mistaken for target-only dependencies`() {
        val result = projectDir.gradle(*analysisTasks, "-PanalysisDependency=incompatible-jvm").buildAndFail()

        assertContains(result.output, "No matching variant of test.analysis:incompatible-jvm:1.0 was found")
        assertContains(result.output, "unsupported-usage")
    }
}
