package io.github.dzirbel

import kotlin.test.Test

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
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektJvmMain")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektJsMain")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektJvmTest")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektJsTest")
        assertTaskPassed(result, ":kmp-jvm-js-clean:check")
    }

    @Test
    fun `detekt succeeds`() {
        val result = projectDir.gradle("detekt").build()

        assertTaskPassed(result, ":kmp-jvm-js-clean:compileKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileKotlinJs")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileTestKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileTestKotlinJs")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektJvmMain")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektJsMain")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektJvmTest")
        assertDetektTaskPassed(result, ":kmp-jvm-js-clean:detektJsTest")
    }
}
