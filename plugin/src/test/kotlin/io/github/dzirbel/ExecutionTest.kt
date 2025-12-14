package io.github.dzirbel

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ExecutionTest {

    val projectDir = File("src/test/resources/jvm-sample")

    @Test
    fun `check passes`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("check")
            .build()

        val checkOutcome = checkNotNull(result.task(":check")).outcome
        assertEquals(checkOutcome, TaskOutcome.SUCCESS)
    }

    @Test
    fun `detektMain fails`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("detektMain")
            .buildAndFail()

        println(result.output)

        val checkOutcome = checkNotNull(result.task(":detektMain")).outcome
        assertEquals(checkOutcome, TaskOutcome.FAILED)
        assertContains(
            result.output,
            "detekt-config/plugin/src/test/resources/jvm-sample/src/main/kotlin/com/dzirbel/Main.kt:4:5: " +
                "Variable 'x' could be val. [VarCouldBeVal]"
        )
    }

    @AfterTest
    fun cleanup() {
        projectDir.resolve("build").deleteRecursively()
    }
}
