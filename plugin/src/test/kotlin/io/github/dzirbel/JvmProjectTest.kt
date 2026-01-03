package io.github.dzirbel

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JvmProjectTest {

    private val projectDir = File("src/test/resources/jvm")
    private val sampleFile = projectDir.resolve("src/main/kotlin/io/github/dzirbel/Sample.kt")

    @Test
    fun `check fails`() {
        val result = GradleRunner.create().withProjectDir(projectDir).withArguments("check").buildAndFail()

        assertNotNull(result.task(":compileKotlin"))
        assertNull(result.task(":check")) // check doesn't run because a dependency failed

        val detektMain = checkNotNull(result.task(":detektMain"))
        assertEquals(TaskOutcome.FAILED, detektMain.outcome)
        assertEquals(
            "${sampleFile.absolutePath}:4:5: Variable 'x' could be val. [VarCouldBeVal]",
            result.findTaskOutput(detektMain),
        )
    }

    @Test
    fun `detekt passes`() {
        GradleRunner.create().withProjectDir(projectDir).withArguments("detekt").build()
    }
}
