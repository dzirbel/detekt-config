package io.github.dzirbel

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains

class KmpNativeProjectTest {

    private val projectDir = File("src/test/resources/kmp-native")
    private val commonFile = projectDir.resolve("src/commonMain/kotlin/io/github/dzirbel/SampleCommon.kt")
    private val appleFile = projectDir.resolve("src/iosMain/kotlin/io/github/dzirbel/SampleNative.kt")
    private val linuxArm64File = projectDir.resolve("src/linuxArm64Main/kotlin/io/github/dzirbel/SampleLinuxArm64.kt")
    private val linuxFile = projectDir.resolve("src/linuxX64Main/kotlin/io/github/dzirbel/SampleLinux.kt")
    private val mingwFile = projectDir.resolve("src/mingwX64Main/kotlin/io/github/dzirbel/SampleMingw.kt")

    @Test
    fun `detekt runs for native target`() {
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")
        val isMac = osName.contains("Mac", ignoreCase = true)
        val isWindows = osName.contains("Windows", ignoreCase = true)
        val isArm64 = osArch.equals("aarch64", ignoreCase = true) || osArch.equals("arm64", ignoreCase = true)
        val detektTaskName = when {
            isMac && isArm64 -> ":kmp-native:detektIosSimulatorArm64Main"
            isMac -> ":kmp-native:detektIosX64Main"
            isWindows -> ":kmp-native:detektMingwX64Main"
            isArm64 -> ":kmp-native:detektLinuxArm64Main"
            else -> ":kmp-native:detektLinuxX64Main"
        }
        val nativeFile = when {
            isMac -> appleFile
            isWindows -> mingwFile
            isArm64 -> linuxArm64File
            else -> linuxFile
        }

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPlainConsole(":kmp-native:detekt", "--continue")
            .buildAndFail()

        val output = assertTaskFailed(result, detektTaskName)
        assertVarCouldBeVal(output, commonFile)
        assertVarCouldBeVal(output, nativeFile)
    }

    private fun assertVarCouldBeVal(output: String, file: File) {
        val expected = "${file.absolutePath}:4:5: Variable 'x' could be val. [VarCouldBeVal]"
        assertContains(output, expected)
    }
}
