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

    // TODO this should also include the other results from the JS test
    private val results = "${sampleFile.absolutePath}:4:5: Variable 'x' could be val. [VarCouldBeVal]"

    @Test
    fun `check fails`() {
        val result = GradleRunner.create().withProjectDir(projectDir).withArguments("check").buildAndFail()

        assertNotNull(result.task(":jvm:compileKotlin"))
        assertNull(result.task(":check"))

        val detektMain = checkNotNull(result.task(":jvm:detektMain"))
        assertEquals(TaskOutcome.FAILED, detektMain.outcome)
        assertEquals(results, result.findTaskOutput(detektMain))
    }

    @Test
    fun `detekt fails`() {
        val result = GradleRunner.create().withProjectDir(projectDir).withArguments("detekt").buildAndFail()

        assertNotNull(result.task(":jvm:compileKotlin"))
        assertNull(result.task(":detekt"))

        val detektMain = checkNotNull(result.task(":jvm:detektMain"))
        assertEquals(TaskOutcome.FAILED, detektMain.outcome)
        assertEquals(results, result.findTaskOutput(detektMain))
    }
}
