package io.github.dzirbel

import kotlin.test.Test

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
        assertDetektTaskPassed(result, ":jvm-clean:detekt")
    }
}
