package io.github.dzirbel

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.Test

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
            .withArguments("check", "--continue")
            .buildAndFail()

        assertTaskPassed(result, ":kmp-jvm-js:compileKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js:compileKotlinJs")
        assertTaskNotRun(result, ":kmp-jvm-js:check")

        val jvmOutput = assertTaskFailed(result, ":kmp-jvm-js:detektJvmMain")
        val jsOutput = assertTaskFailed(result, ":kmp-jvm-js:detektJsMain")
        assertSameContents(expectedWarnings(commonFile, sharedFile, jvmFile), jvmOutput)
        assertSameContents(expectedWarnings(commonFile, sharedFile, jsFile), jsOutput)
    }
}
