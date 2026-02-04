package io.github.dzirbel

import java.io.File
import kotlin.test.Test

class JvmCleanProjectTest {

    private val projectDir = File("src/test/resources/jvm-clean")

    @Test
    fun `compilation succeeds`() {
        projectDir.gradle("assemble").build()
    }

    @Test
    fun `tests succeed`() {
        projectDir.gradle("test").build()
    }

    @Test
    fun `check succeeds`() {
        val result = projectDir.gradle("check").build()

        assertTaskPassed(result, ":jvm-clean:compileKotlin")
        assertTaskPassed(result, ":jvm-clean:compileTestKotlin")
        assertTaskPassed(result, ":jvm-clean:detektMain")
        assertTaskPassed(result, ":jvm-clean:detektTest")
        assertTaskPassed(result, ":jvm-clean:check")
    }

    @Test
    fun `detekt succeeds`() {
        val result = projectDir.gradle("detekt").build()

        assertTaskPassed(result, ":jvm-clean:compileKotlin")
        assertTaskPassed(result, ":jvm-clean:compileTestKotlin")
        assertTaskPassed(result, ":jvm-clean:detektMain")
        assertTaskPassed(result, ":jvm-clean:detektTest")
        assertTaskPassed(result, ":jvm-clean:detekt")
    }
}
