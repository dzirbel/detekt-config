package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertContains

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
}
