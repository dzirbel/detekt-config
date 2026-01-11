package io.github.dzirbel

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.Test

class KmpNativeProjectTest {

    private val projectDir = File("src/test/resources/kmp-native")

    @Test
    fun `compilation succeeds`() {
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("assemble")
            .build()
    }

    // TODO run tests

    @Test
    fun `check fails`() {
        val expectation = nativeExpectation()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("check", "--continue")
            .buildAndFail()

        assertTaskPassed(result, expectation.compileTaskName)
        assertTaskPassed(result, expectation.compileTestTaskName)
        assertTaskNotRun(result, ":kmp-native:check")
        assertFailedTasks(result, expectation.detektTaskName, expectation.detektTestTaskName)

        val mainOutput = assertTaskFailed(result, expectation.detektTaskName)
        val testOutput = assertTaskFailed(result, expectation.detektTestTaskName)
        assertSameContents(expectedWarnings(*expectation.mainFiles.toTypedArray()), mainOutput)
        assertSameContents(expectedTestWarnings(*expectation.testFiles.toTypedArray()), testOutput)
    }

    @Test
    fun `detekt fails`() {
        val expectation = nativeExpectation()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("detekt", "--continue")
            .buildAndFail()

        assertTaskPassed(result, expectation.compileTaskName)
        assertTaskPassed(result, expectation.compileTestTaskName)
        assertTaskNotRun(result, ":kmp-native:detekt")
        assertFailedTasks(result, expectation.detektTaskName, expectation.detektTestTaskName)

        val mainOutput = assertTaskFailed(result, expectation.detektTaskName)
        val testOutput = assertTaskFailed(result, expectation.detektTestTaskName)
        assertSameContents(expectedWarnings(*expectation.mainFiles.toTypedArray()), mainOutput)
        assertSameContents(expectedTestWarnings(*expectation.testFiles.toTypedArray()), testOutput)
    }

    private fun nativeExpectation(): NativeExpectation {
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")
        val isMac = osName.contains("Mac", ignoreCase = true)
        val isWindows = osName.contains("Windows", ignoreCase = true)
        val isArm64 = osArch.equals("aarch64", ignoreCase = true) || osArch.equals("arm64", ignoreCase = true)

        val srcDir = projectDir.resolve("src")
        val commonFile = srcDir.resolve("commonMain/kotlin/io/github/dzirbel/SampleCommon.kt")
        val commonTestFile = srcDir.resolve("commonTest/kotlin/io/github/dzirbel/SampleCommonTest.kt")

        return when {
            isMac && isArm64 -> NativeExpectation(
                mainFiles = listOf(
                    commonFile,
                    srcDir.resolve("iosMain/kotlin/io/github/dzirbel/SampleNative.kt"),
                    srcDir.resolve("iosSimulatorArm64Main/kotlin/io/github/dzirbel/SampleIosSimulatorArm64.kt"),
                ),
                testFiles = listOf(
                    commonTestFile,
                    srcDir.resolve("iosTest/kotlin/io/github/dzirbel/SampleIosTest.kt"),
                    srcDir.resolve("iosSimulatorArm64Test/kotlin/io/github/dzirbel/SampleIosSimulatorArm64Test.kt"),
                ),
                detektTaskName = ":kmp-native:detektIosSimulatorArm64Main",
                detektTestTaskName = ":kmp-native:detektIosSimulatorArm64Test",
                compileTaskName = ":kmp-native:compileKotlinIosSimulatorArm64",
                compileTestTaskName = ":kmp-native:compileTestKotlinIosSimulatorArm64",
            )

            isMac -> NativeExpectation(
                mainFiles = listOf(
                    commonFile,
                    srcDir.resolve("iosMain/kotlin/io/github/dzirbel/SampleNative.kt"),
                    srcDir.resolve("iosX64Main/kotlin/io/github/dzirbel/SampleIosX64.kt"),
                ),
                testFiles = listOf(
                    commonTestFile,
                    srcDir.resolve("iosTest/kotlin/io/github/dzirbel/SampleIosTest.kt"),
                    srcDir.resolve("iosX64Test/kotlin/io/github/dzirbel/SampleIosX64Test.kt"),
                ),
                detektTaskName = ":kmp-native:detektIosX64Main",
                detektTestTaskName = ":kmp-native:detektIosX64Test",
                compileTaskName = ":kmp-native:compileKotlinIosX64",
                compileTestTaskName = ":kmp-native:compileTestKotlinIosX64",
            )

            isWindows -> NativeExpectation(
                mainFiles = listOf(
                    commonFile,
                    srcDir.resolve("mingwX64Main/kotlin/io/github/dzirbel/SampleMingw.kt"),
                ),
                testFiles = listOf(
                    commonTestFile,
                    srcDir.resolve("mingwX64Test/kotlin/io/github/dzirbel/SampleMingwTest.kt"),
                ),
                detektTaskName = ":kmp-native:detektMingwX64Main",
                detektTestTaskName = ":kmp-native:detektMingwX64Test",
                compileTaskName = ":kmp-native:compileKotlinMingwX64",
                compileTestTaskName = ":kmp-native:compileTestKotlinMingwX64",
            )

            isArm64 -> NativeExpectation(
                mainFiles = listOf(
                    commonFile,
                    srcDir.resolve("linuxArm64Main/kotlin/io/github/dzirbel/SampleLinuxArm64.kt"),
                ),
                testFiles = listOf(
                    commonTestFile,
                    srcDir.resolve("linuxArm64Test/kotlin/io/github/dzirbel/SampleLinuxArm64Test.kt"),
                ),
                detektTaskName = ":kmp-native:detektLinuxArm64Main",
                detektTestTaskName = ":kmp-native:detektLinuxArm64Test",
                compileTaskName = ":kmp-native:compileKotlinLinuxArm64",
                compileTestTaskName = ":kmp-native:compileTestKotlinLinuxArm64",
            )

            else -> NativeExpectation(
                mainFiles = listOf(
                    commonFile,
                    srcDir.resolve("linuxX64Main/kotlin/io/github/dzirbel/SampleLinux.kt"),
                ),
                testFiles = listOf(
                    commonTestFile,
                    srcDir.resolve("linuxX64Test/kotlin/io/github/dzirbel/SampleLinuxTest.kt"),
                ),
                detektTaskName = ":kmp-native:detektLinuxX64Main",
                detektTestTaskName = ":kmp-native:detektLinuxX64Test",
                compileTaskName = ":kmp-native:compileKotlinLinuxX64",
                compileTestTaskName = ":kmp-native:compileTestKotlinLinuxX64",
            )
        }
    }

    private data class NativeExpectation(
        val mainFiles: List<File>,
        val testFiles: List<File>,
        val detektTaskName: String,
        val detektTestTaskName: String,
        val compileTaskName: String,
        val compileTestTaskName: String,
    )
}
