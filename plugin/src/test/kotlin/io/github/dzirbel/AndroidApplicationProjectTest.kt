package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertFalse

class AndroidApplicationProjectTest : SampleProjectTest(
    projectName = "android-application",
    requiresAndroidSdk = true,
) {

    private val mainFile = projectDir.resolve(
        "src/main/kotlin/io/github/dzirbel/application/SampleApplication.kt",
    )
    private val unitTestFile = projectDir.resolve(
        "src/test/kotlin/io/github/dzirbel/application/SampleUnitTest.kt",
    )
    private val instrumentedTestFile = projectDir.resolve(
        "src/androidTest/kotlin/io/github/dzirbel/application/SampleInstrumentedTest.kt",
    )

    @Test
    fun `check aggregates every Android variant and nested test`() {
        val result = projectDir.gradle(
            "check",
            "-x",
            "lint",
            "-x",
            "testDebugUnitTest",
        ).buildAndFail()

        val mainTasks = listOf(
            ":android-application:detektDebug",
            ":android-application:detektRelease",
        )
        val unitTestTasks = listOf(":android-application:detektDebugUnitTest")
        val instrumentedTestTask = ":android-application:detektDebugAndroidTest"

        assertFailedTasks(result, *(mainTasks + unitTestTasks + instrumentedTestTask).toTypedArray())
        mainTasks.forEach { task ->
            assertSameContents(
                listOf(expectedExternalDependencyWarning(mainFile)),
                assertTaskFailed(result, task),
            )
        }
        unitTestTasks.forEach { task ->
            assertSameContents(
                listOf(expectedExternalDependencyWarning(unitTestFile)),
                assertTaskFailed(result, task),
            )
        }
        assertSameContents(
            listOf(expectedExternalDependencyWarning(instrumentedTestFile)),
            assertTaskFailed(result, instrumentedTestTask),
        )
        assertTaskNotRun(result, ":android-application:detektMain")
        assertTaskNotRun(result, ":android-application:detektTest")
        assertTaskNotRun(result, ":android-application:detekt")
        assertTaskNotRun(result, ":android-application:check")
    }

    @Test
    fun `non Compose Android application does not load Compose rules`() {
        val result = projectDir.gradle("dependencies", "--configuration", "detektPlugins").build()

        assertFalse(result.output.contains("io.nlopez.compose.rules"))
    }
}
