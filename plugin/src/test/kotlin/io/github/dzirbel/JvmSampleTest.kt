package io.github.dzirbel

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JvmSampleTest {

    val projectDir = File("../samples")

    @Test
    fun `check fails`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(":jvm:check")
            .buildAndFail()

        val detektOutcome = checkNotNull(result.task(":jvm:detektMain")).outcome
        assertEquals(detektOutcome, TaskOutcome.FAILED)
        assertContains(
            result.output,
            "samples/jvm/src/main/kotlin/com/dzirbel/Main.kt:4:5: Variable 'x' could be val. [VarCouldBeVal]",
        )

        assertNull(result.task(":jvm:check")) // check doesn't run because a dependency failed
    }

    @Test
    fun `detekt passes`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(":jvm:detekt")
            .build()

        val detektOutcome = checkNotNull(result.task(":jvm:detekt")).outcome
        assertEquals(detektOutcome, TaskOutcome.SUCCESS)
    }
}
