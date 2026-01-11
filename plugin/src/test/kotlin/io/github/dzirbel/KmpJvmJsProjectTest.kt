package io.github.dzirbel

import java.io.File
import kotlin.test.Test

class KmpJvmJsProjectTest {

    private val projectDir = File("src/test/resources/kmp-jvm-js")
    private val commonFile = projectDir.resolve("src/commonMain/kotlin/io/github/dzirbel/SampleCommon.kt")
    private val sharedFile = projectDir.resolve("src/sharedMain/kotlin/io/github/dzirbel/SampleShared.kt")
    private val jvmFile = projectDir.resolve("src/jvmMain/kotlin/io/github/dzirbel/SampleJvm.kt")
    private val jsFile = projectDir.resolve("src/jsMain/kotlin/io/github/dzirbel/SampleJs.kt")
    private val commonTestFile = projectDir.resolve("src/commonTest/kotlin/io/github/dzirbel/SampleCommonTest.kt")
    private val sharedTestFile = projectDir.resolve("src/sharedTest/kotlin/io/github/dzirbel/SampleSharedTest.kt")
    private val jvmTestFile = projectDir.resolve("src/jvmTest/kotlin/io/github/dzirbel/SampleJvmTest.kt")
    private val jsTestFile = projectDir.resolve("src/jsTest/kotlin/io/github/dzirbel/SampleJsTest.kt")

    @Test
    fun `compilation succeeds`() {
        projectDir.gradle("assemble").build()
    }

    @Test
    fun `jvm tests succeed`() {
        projectDir.gradle("jvmTest").build()
    }

    @Test
    fun `check fails`() {
        val result = projectDir.gradle("check").buildAndFail()

        assertTaskPassed(result, ":kmp-jvm-js:compileKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js:compileKotlinJs")
        assertTaskPassed(result, ":kmp-jvm-js:compileTestKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js:compileTestKotlinJs")
        assertTaskNotRun(result, ":kmp-jvm-js:check")
        assertFailedTasks(
            result,
            ":kmp-jvm-js:detektJvmMain",
            ":kmp-jvm-js:detektJsMain",
            ":kmp-jvm-js:detektJvmTest",
            ":kmp-jvm-js:detektJsTest",
            ":kmp-jvm-js:jsBrowserTest",
        )

        val jvmOutput = assertTaskFailed(result, ":kmp-jvm-js:detektJvmMain")
        val jsOutput = assertTaskFailed(result, ":kmp-jvm-js:detektJsMain")
        val jvmTestOutput = assertTaskFailed(result, ":kmp-jvm-js:detektJvmTest")
        val jsTestOutput = assertTaskFailed(result, ":kmp-jvm-js:detektJsTest")
        assertSameContents(expectedWarnings(commonFile, sharedFile, jvmFile), jvmOutput)
        assertSameContents(expectedWarnings(commonFile, sharedFile, jsFile), jsOutput)
        assertSameContents(expectedTestWarnings(commonTestFile, sharedTestFile, jvmTestFile), jvmTestOutput)
        assertSameContents(expectedTestWarnings(commonTestFile, sharedTestFile, jsTestFile), jsTestOutput)
    }

    @Test
    fun `detekt fails`() {
        val result = projectDir.gradle("detekt").buildAndFail()

        assertTaskPassed(result, ":kmp-jvm-js:compileKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js:compileKotlinJs")
        assertTaskPassed(result, ":kmp-jvm-js:compileTestKotlinJvm")
        assertTaskPassed(result, ":kmp-jvm-js:compileTestKotlinJs")
        assertTaskNotRun(result, ":kmp-jvm-js:detekt")
        assertFailedTasks(
            result,
            ":kmp-jvm-js:detektJvmMain",
            ":kmp-jvm-js:detektJsMain",
            ":kmp-jvm-js:detektJvmTest",
            ":kmp-jvm-js:detektJsTest",
        )

        val jvmOutput = assertTaskFailed(result, ":kmp-jvm-js:detektJvmMain")
        val jsOutput = assertTaskFailed(result, ":kmp-jvm-js:detektJsMain")
        val jvmTestOutput = assertTaskFailed(result, ":kmp-jvm-js:detektJvmTest")
        val jsTestOutput = assertTaskFailed(result, ":kmp-jvm-js:detektJsTest")
        assertSameContents(expectedWarnings(commonFile, sharedFile, jvmFile), jvmOutput)
        assertSameContents(expectedWarnings(commonFile, sharedFile, jsFile), jsOutput)
        assertSameContents(expectedTestWarnings(commonTestFile, sharedTestFile, jvmTestFile), jvmTestOutput)
        assertSameContents(expectedTestWarnings(commonTestFile, sharedTestFile, jsTestFile), jsTestOutput)
    }
}
