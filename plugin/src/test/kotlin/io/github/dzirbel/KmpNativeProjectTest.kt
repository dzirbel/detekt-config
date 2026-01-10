package io.github.dzirbel

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.Test

class KmpNativeProjectTest {

    private val projectDir = File("src/test/resources/kmp-native")
    private val commonFile = projectDir.resolve("src/commonMain/kotlin/io/github/dzirbel/SampleCommon.kt")

    @Test
    fun `check runs for native target`() {
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")
        val isMac = osName.contains("Mac", ignoreCase = true)
        val isWindows = osName.contains("Windows", ignoreCase = true)
        val isArm64 = osArch.equals("aarch64", ignoreCase = true) || osArch.equals("arm64", ignoreCase = true)
        val iosMainFile = projectDir.resolve("src/iosMain/kotlin/io/github/dzirbel/SampleNative.kt")
        val expectedFiles = mutableListOf(commonFile)
        val detektTaskName = when {
            isMac && isArm64 -> {
                expectedFiles += iosMainFile
                expectedFiles += projectDir.resolve(
                    "src/iosSimulatorArm64Main/kotlin/io/github/dzirbel/SampleIosSimulatorArm64.kt"
                )
                ":kmp-native:detektIosSimulatorArm64Main"
            }
            isMac -> {
                expectedFiles += iosMainFile
                expectedFiles += projectDir.resolve("src/iosX64Main/kotlin/io/github/dzirbel/SampleIosX64.kt")
                ":kmp-native:detektIosX64Main"
            }
            isWindows -> {
                expectedFiles += projectDir.resolve("src/mingwX64Main/kotlin/io/github/dzirbel/SampleMingw.kt")
                ":kmp-native:detektMingwX64Main"
            }
            isArm64 -> {
                expectedFiles += projectDir.resolve("src/linuxArm64Main/kotlin/io/github/dzirbel/SampleLinuxArm64.kt")
                ":kmp-native:detektLinuxArm64Main"
            }
            else -> {
                expectedFiles += projectDir.resolve("src/linuxX64Main/kotlin/io/github/dzirbel/SampleLinux.kt")
                ":kmp-native:detektLinuxX64Main"
            }
        }

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("check", "--continue")
            .buildAndFail()

        // TODO check compile task(s) were run
        assertTaskNotRun(result, ":kmp-native:check")

        val output = assertTaskFailed(result, detektTaskName)
        assertSameContents(expectedWarnings(*expectedFiles.toTypedArray()), output)
    }
}
