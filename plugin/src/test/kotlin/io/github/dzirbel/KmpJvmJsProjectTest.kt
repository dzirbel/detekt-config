package io.github.dzirbel

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class KmpJvmJsProjectTest {

    private val projectDir = File("src/test/resources/kmp-jvm-js")
    private val commonFile = projectDir.resolve("src/commonMain/kotlin/io/github/dzirbel/SampleCommon.kt")
    private val sharedFile = projectDir.resolve("src/sharedMain/kotlin/io/github/dzirbel/SampleShared.kt")
    private val jvmFile = projectDir.resolve("src/jvmMain/kotlin/io/github/dzirbel/SampleJvm.kt")
    private val jsFile = projectDir.resolve("src/jsMain/kotlin/io/github/dzirbel/SampleJs.kt")

    @Test
    fun `check runs detekt for kmp jvm and js`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPlainConsole(":kmp-jvm-js:check", "--continue")
            .buildAndFail()

        val jvmOutput = assertTaskFailed(result, ":kmp-jvm-js:detektJvmMain")
        assertVarCouldBeVal(jvmOutput, commonFile)
        assertVarCouldBeVal(jvmOutput, jvmFile)
        assertFalse(jvmOutput.contains(jsFile.absolutePath))

        val jsOutput = assertTaskFailed(result, ":kmp-jvm-js:detektJsMain")
        assertVarCouldBeVal(jsOutput, commonFile)
        assertVarCouldBeVal(jsOutput, jsFile)
        assertFalse(jsOutput.contains(jvmFile.absolutePath))
    }

    @Ignore("TODO: sharedMain sources are not yet included in detekt inputs")
    @Test
    fun `detekt includes shared source set`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPlainConsole(":kmp-jvm-js:check", "--continue")
            .buildAndFail()

        val jvmOutput = assertTaskFailed(result, ":kmp-jvm-js:detektJvmMain")
        assertVarCouldBeVal(jvmOutput, sharedFile)

        val jsOutput = assertTaskFailed(result, ":kmp-jvm-js:detektJsMain")
        assertVarCouldBeVal(jsOutput, sharedFile)
    }

    private fun assertVarCouldBeVal(output: String, file: File) {
        assertContains(output, "${file.absolutePath}:4:5: Variable 'x' could be val. [VarCouldBeVal]")
    }
}
