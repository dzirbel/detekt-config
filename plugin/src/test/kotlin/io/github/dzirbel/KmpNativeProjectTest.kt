package io.github.dzirbel

import java.io.File
import kotlin.test.Test

class KmpNativeProjectTest : SampleProjectTest("kmp-native") {

    @Test
    fun `compilation succeeds`() {
        projectDir.gradle("assemble").build()
    }

    @Test
    fun `tests succeed`() {
        val targets = nativeTargets()
        for (target in targets) {
            val result = projectDir.gradle(target.testTaskName).build()

            assertTaskPassed(result, target.testTaskName)
            if (!target.testTaskName.endsWith("TestBinaries")) {
                assertTestsExecuted(projectDir, target.testTaskName.substringAfterLast(':'))
            }
        }
    }

    @Test
    fun `check fails`() {
        val targets = nativeTargets()
        val result = projectDir.gradle("check").buildAndFail()

        assertTaskNotRun(result, ":kmp-native:check")
        assertFailedTasks(result, *targets.flatMap { it.failedTasks }.toTypedArray())

        for (target in targets) {
            assertTaskPassed(result, target.compileTaskName)
            assertTaskPassed(result, target.compileTestTaskName)

            val mainOutput = assertTaskFailed(result, target.detektTaskName)
            val testOutput = assertTaskFailed(result, target.detektTestTaskName)
            assertSameContents(
                expectedWarnings(*target.mainFiles.toTypedArray()) +
                    expectedExternalDependencyWarning(target.mainFiles.first()),
                mainOutput,
            )
            assertSameContents(
                expectedTestWarnings(*target.testFiles.toTypedArray()) +
                    expectedExternalDependencyWarning(target.testFiles.first()),
                testOutput,
            )
        }
    }

    @Test
    fun `detekt fails`() {
        val targets = nativeTargets()
        val result = projectDir.gradle("detekt").buildAndFail()

        assertTaskNotRun(result, ":kmp-native:detekt")
        assertFailedTasks(result, *targets.flatMap { it.failedTasks }.toTypedArray())

        for (target in targets) {
            assertTaskPassed(result, target.compileTaskName)
            assertTaskPassed(result, target.compileTestTaskName)

            val mainOutput = assertTaskFailed(result, target.detektTaskName)
            val testOutput = assertTaskFailed(result, target.detektTestTaskName)
            assertSameContents(
                expectedWarnings(*target.mainFiles.toTypedArray()) +
                    expectedExternalDependencyWarning(target.mainFiles.first()),
                mainOutput,
            )
            assertSameContents(
                expectedTestWarnings(*target.testFiles.toTypedArray()) +
                    expectedExternalDependencyWarning(target.testFiles.first()),
                testOutput,
            )
        }
    }

    private fun nativeTargets(): List<NativeTarget> {
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")
        val isMac = osName.contains("Mac", ignoreCase = true)
        val isWindows = osName.contains("Windows", ignoreCase = true)
        val isArm64 = osArch.equals("aarch64", ignoreCase = true) || osArch.equals("arm64", ignoreCase = true)

        val srcDir = projectDir.resolve("src")
        val commonFile = srcDir.resolve("commonMain/kotlin/io/github/dzirbel/SampleCommon.kt")
        val commonTestFile = srcDir.resolve("commonTest/kotlin/io/github/dzirbel/SampleCommonTest.kt")
        val iosMainFile = srcDir.resolve("iosMain/kotlin/io/github/dzirbel/SampleNative.kt")
        val iosTestFile = srcDir.resolve("iosTest/kotlin/io/github/dzirbel/SampleIosTest.kt")
        val iosSimulatorArm64MainFile =
            srcDir.resolve("iosSimulatorArm64Main/kotlin/io/github/dzirbel/SampleIosSimulatorArm64.kt")
        val iosSimulatorArm64TestFile =
            srcDir.resolve("iosSimulatorArm64Test/kotlin/io/github/dzirbel/SampleIosSimulatorArm64Test.kt")
        val iosX64MainFile = srcDir.resolve("iosX64Main/kotlin/io/github/dzirbel/SampleIosX64.kt")
        val iosX64TestFile = srcDir.resolve("iosX64Test/kotlin/io/github/dzirbel/SampleIosX64Test.kt")

        return when {
            isMac -> listOf(
                NativeTarget(
                    mainFiles = listOf(commonFile, iosMainFile),
                    testFiles = listOf(commonTestFile, iosTestFile),
                    detektTaskName = ":kmp-native:detektMainIosArm64",
                    detektTestTaskName = ":kmp-native:detektTestIosArm64",
                    compileTaskName = ":kmp-native:compileKotlinIosArm64",
                    compileTestTaskName = ":kmp-native:compileTestKotlinIosArm64",
                    testTaskName = ":kmp-native:iosArm64TestBinaries",
                ),
                if (isArm64) {
                    NativeTarget(
                        mainFiles = listOf(commonFile, iosMainFile, iosSimulatorArm64MainFile),
                        testFiles = listOf(commonTestFile, iosTestFile, iosSimulatorArm64TestFile),
                        detektTaskName = ":kmp-native:detektMainIosSimulatorArm64",
                        detektTestTaskName = ":kmp-native:detektTestIosSimulatorArm64",
                        compileTaskName = ":kmp-native:compileKotlinIosSimulatorArm64",
                        compileTestTaskName = ":kmp-native:compileTestKotlinIosSimulatorArm64",
                        testTaskName = ":kmp-native:iosSimulatorArm64Test",
                    )
                } else {
                    NativeTarget(
                        mainFiles = listOf(commonFile, iosMainFile, iosX64MainFile),
                        testFiles = listOf(commonTestFile, iosTestFile, iosX64TestFile),
                        detektTaskName = ":kmp-native:detektMainIosX64",
                        detektTestTaskName = ":kmp-native:detektTestIosX64",
                        compileTaskName = ":kmp-native:compileKotlinIosX64",
                        compileTestTaskName = ":kmp-native:compileTestKotlinIosX64",
                        testTaskName = ":kmp-native:iosX64Test",
                    )
                },
            )

            isWindows -> listOf(
                NativeTarget(
                    mainFiles = listOf(
                        commonFile,
                        srcDir.resolve("mingwX64Main/kotlin/io/github/dzirbel/SampleMingw.kt"),
                    ),
                    testFiles = listOf(
                        commonTestFile,
                        srcDir.resolve("mingwX64Test/kotlin/io/github/dzirbel/SampleMingwTest.kt"),
                    ),
                    detektTaskName = ":kmp-native:detektMainMingwX64",
                    detektTestTaskName = ":kmp-native:detektTestMingwX64",
                    compileTaskName = ":kmp-native:compileKotlinMingwX64",
                    compileTestTaskName = ":kmp-native:compileTestKotlinMingwX64",
                    testTaskName = ":kmp-native:mingwX64Test",
                ),
            )

            isArm64 -> listOf(
                NativeTarget(
                    mainFiles = listOf(
                        commonFile,
                        srcDir.resolve("linuxArm64Main/kotlin/io/github/dzirbel/SampleLinuxArm64.kt"),
                    ),
                    testFiles = listOf(
                        commonTestFile,
                        srcDir.resolve("linuxArm64Test/kotlin/io/github/dzirbel/SampleLinuxArm64Test.kt"),
                    ),
                    detektTaskName = ":kmp-native:detektMainLinuxArm64",
                    detektTestTaskName = ":kmp-native:detektTestLinuxArm64",
                    compileTaskName = ":kmp-native:compileKotlinLinuxArm64",
                    compileTestTaskName = ":kmp-native:compileTestKotlinLinuxArm64",
                    testTaskName = ":kmp-native:linuxArm64Test",
                ),
            )

            else -> listOf(
                NativeTarget(
                    mainFiles = listOf(
                        commonFile,
                        srcDir.resolve("linuxX64Main/kotlin/io/github/dzirbel/SampleLinux.kt"),
                    ),
                    testFiles = listOf(
                        commonTestFile,
                        srcDir.resolve("linuxX64Test/kotlin/io/github/dzirbel/SampleLinuxTest.kt"),
                    ),
                    detektTaskName = ":kmp-native:detektMainLinuxX64",
                    detektTestTaskName = ":kmp-native:detektTestLinuxX64",
                    compileTaskName = ":kmp-native:compileKotlinLinuxX64",
                    compileTestTaskName = ":kmp-native:compileTestKotlinLinuxX64",
                    testTaskName = ":kmp-native:linuxX64Test",
                ),
            )
        }
    }

    private data class NativeTarget(
        val mainFiles: List<File>,
        val testFiles: List<File>,
        val detektTaskName: String,
        val detektTestTaskName: String,
        val compileTaskName: String,
        val compileTestTaskName: String,
        val testTaskName: String,
    ) {
        val failedTasks = listOf(detektTaskName, detektTestTaskName)
    }
}
