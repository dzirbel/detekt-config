package io.github.dzirbel

import java.io.File
import kotlin.test.Test

class KmpJvmJsCleanProjectTest {

    private val projectDir = File("src/test/resources/kmp-jvm-js-clean")

    @Test
    fun `compilation succeeds`() {
        projectDir.gradle("assemble").build()
    }

    @Test
    fun `jvm tests succeed`() {
        projectDir.gradle("jvmTest").build()
    }

    @Test
    fun `check succeeds`() {
        val result = projectDir.gradle("check").build()

        assertTaskPassed(result, ":kmp-jvm-js-clean:compileKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileKotlinJs")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileTestKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileTestKotlinJs")
        assertTaskPassed(result, ":kmp-jvm-js-clean:detektJvmMain")
        assertTaskPassed(result, ":kmp-jvm-js-clean:detektJsMain")
        assertTaskPassed(result, ":kmp-jvm-js-clean:detektJvmTest")
        assertTaskPassed(result, ":kmp-jvm-js-clean:detektJsTest")
        assertTaskPassed(result, ":kmp-jvm-js-clean:check")
    }

    @Test
    fun `detekt succeeds`() {
        val result = projectDir.gradle("detekt").build()

        assertTaskPassed(result, ":kmp-jvm-js-clean:compileKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileKotlinJs")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileTestKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js-clean:compileTestKotlinJs")
        assertTaskPassed(result, ":kmp-jvm-js-clean:detektJvmMain")
        assertTaskPassed(result, ":kmp-jvm-js-clean:detektJsMain")
        assertTaskPassed(result, ":kmp-jvm-js-clean:detektJvmTest")
        assertTaskPassed(result, ":kmp-jvm-js-clean:detektJsTest")
    }
}
