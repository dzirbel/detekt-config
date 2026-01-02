package io.github.dzirbel

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JvmSampleTest {

    val projectDir = File("src/test/resources/jvm")

    @Test
    fun `check fails`() {
        val expectedTaskOutput = listOf(
            "> Task :detektMain FAILED",
            "${projectDir.absolutePath}/src/main/kotlin/io/github/dzirbel/Main.kt:4:5: " +
                "Variable 'x' could be val. [VarCouldBeVal]",
            "",
            "",
        ).joinToString(separator = "\n")

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("check")
            .buildAndFail()

        assertNotNull(result.task(":compileKotlin"))
        assertNull(result.task(":check")) // check doesn't run because a dependency failed

        assertEquals(TaskOutcome.FAILED, checkNotNull(result.task(":detektMain")).outcome)
        assertContains(result.output, expectedTaskOutput)
        assertTrue(result.output.contains(expectedTaskOutput))
    }

    @Test
    fun `detekt passes`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("detekt")
            .build()

        // remove tasks run in the included rules build
        val tasks = result.tasks.map { it.path }.filter { !it.startsWith(":detekt-config:rules") }
        assertEquals(listOf(":detekt"), tasks)

        assertEquals(TaskOutcome.SUCCESS, checkNotNull(result.task(":detekt")).outcome)
    }
}
