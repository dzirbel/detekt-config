package io.github.dzirbel

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmProjectTest {

    private val projectDir = File("src/test/resources/jvm")
    private val sampleFile = projectDir.resolve("src/main/kotlin/io/github/dzirbel/Sample.kt")

    private val results = """
        ${sampleFile.absolutePath}:4:5: Variable x is declared as `var` with a mutable type kotlin.collections.MutableSet. Consider using `val` or an immutable collection or value type [DoubleMutabilityForCollection]
        ${sampleFile.absolutePath}:5:5: The method `kotlin.io.println` has been forbidden: println does not allow you to configure the output stream. Use a logger instead. [ForbiddenMethodCall]
        ${sampleFile.absolutePath}:4:5: Variable 'x' could be val. [VarCouldBeVal]
    """.trimIndent()

    @Test
    fun `check fails`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("check")
            .buildAndFail()

        assertTaskPassed(result, ":jvm:compileKotlin")
        assertTaskNotRun(result, ":check")
        assertEquals(results, assertTaskFailed(result, ":jvm:detektMain"))
    }

    @Test
    fun `detekt fails`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("detekt")
            .buildAndFail()

        assertTaskPassed(result, ":jvm:compileKotlin")
        assertTaskNotRun(result, ":detekt")
        assertEquals(results, assertTaskFailed(result, ":jvm:detektMain"))
    }
}
