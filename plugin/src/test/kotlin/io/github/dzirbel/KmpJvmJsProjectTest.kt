package io.github.dzirbel

import kotlin.test.Test

class KmpJvmJsProjectTest : SampleProjectTest("kmp-jvm-js") {

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
        val result = projectDir.gradle("jvmTest").build()

        assertTaskPassed(result, ":kmp-jvm-js:jvmTest")
        assertTestsExecuted(projectDir, "jvmTest")
    }

    @Test
    fun `js tests succeed`() {
        val result = projectDir.gradle("jsNodeTest").build()

        assertTaskPassed(result, ":kmp-jvm-js:jsNodeTest")
        assertTestsExecuted(projectDir, "jsNodeTest")
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
            ":kmp-jvm-js:detektMainJvm",
            ":kmp-jvm-js:detektMainJs",
            ":kmp-jvm-js:detektTestJvm",
            ":kmp-jvm-js:detektTestJs",
        )

        val jvmOutput = assertTaskFailed(result, ":kmp-jvm-js:detektMainJvm")
        val jsOutput = assertTaskFailed(result, ":kmp-jvm-js:detektMainJs")
        val jvmTestOutput = assertTaskFailed(result, ":kmp-jvm-js:detektTestJvm")
        val jsTestOutput = assertTaskFailed(result, ":kmp-jvm-js:detektTestJs")
        assertSameContents(expectedWarnings(commonFile, sharedFile, jvmFile), jvmOutput)
        assertSameContents(
            expectedWarnings(commonFile, sharedFile, jsFile) + expectedExternalDependencyWarning(jsFile),
            jsOutput,
        )
        assertSameContents(expectedTestWarnings(commonTestFile, sharedTestFile, jvmTestFile), jvmTestOutput)
        assertSameContents(
            expectedTestWarnings(commonTestFile, sharedTestFile, jsTestFile) +
                expectedExternalDependencyWarning(jsTestFile),
            jsTestOutput,
        )
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
            ":kmp-jvm-js:detektMainJvm",
            ":kmp-jvm-js:detektMainJs",
            ":kmp-jvm-js:detektTestJvm",
            ":kmp-jvm-js:detektTestJs",
        )

        val jvmOutput = assertTaskFailed(result, ":kmp-jvm-js:detektMainJvm")
        val jsOutput = assertTaskFailed(result, ":kmp-jvm-js:detektMainJs")
        val jvmTestOutput = assertTaskFailed(result, ":kmp-jvm-js:detektTestJvm")
        val jsTestOutput = assertTaskFailed(result, ":kmp-jvm-js:detektTestJs")
        assertSameContents(expectedWarnings(commonFile, sharedFile, jvmFile), jvmOutput)
        assertSameContents(
            expectedWarnings(commonFile, sharedFile, jsFile) + expectedExternalDependencyWarning(jsFile),
            jsOutput,
        )
        assertSameContents(expectedTestWarnings(commonTestFile, sharedTestFile, jvmTestFile), jvmTestOutput)
        assertSameContents(
            expectedTestWarnings(commonTestFile, sharedTestFile, jsTestFile) +
                expectedExternalDependencyWarning(jsTestFile),
            jsTestOutput,
        )
    }
}
