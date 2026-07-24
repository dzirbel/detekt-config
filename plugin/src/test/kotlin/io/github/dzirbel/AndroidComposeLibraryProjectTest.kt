package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class AndroidComposeLibraryProjectTest : SampleProjectTest(
    projectName = "android-compose-library",
    requiresAndroidSdk = true,
) {

    private val composeFile = projectDir.resolve(
        "src/main/kotlin/io/github/dzirbel/composelibrary/SampleCompose.kt",
    )

    @Test
    fun `detekt aggregates Android variants and applies Compose rules`() {
        val result = projectDir.gradle("detekt").buildAndFail()

        val mainTasks = listOf(
            ":android-compose-library:detektDebug",
            ":android-compose-library:detektRelease",
        )
        assertFailedTasks(result, *mainTasks.toTypedArray())
        mainTasks.forEach { task ->
            assertComposeOutput(assertTaskFailed(result, task))
        }
        assertTaskNoSource(result, ":android-compose-library:detektDebugAndroidTest")
        assertTaskNoSource(result, ":android-compose-library:detektDebugUnitTest")
        assertTaskNotRun(result, ":android-compose-library:detektMain")
        assertTaskPassed(result, ":android-compose-library:detektTest")
        assertTaskNotRun(result, ":android-compose-library:detekt")
    }

    @Test
    fun `Compose Android library loads Compose rules`() {
        val result = projectDir.gradle("dependencies", "--configuration", "detektPlugins").build()

        assertContains(result.output, "io.nlopez.compose.rules:detekt")
    }

    private fun assertComposeOutput(output: List<String>) {
        assertEquals(
            listOf(
                "${composeFile.absolutePath}:7:14: This @Composable function has a modifier parameter but it doesn't have a default value.",
                "See https://mrmans0n.github.io/compose-rules/rules/#modifiers-should-have-default-parameters for more information. [ModifierWithoutDefault]",
                "${composeFile.absolutePath}:12:1: This @Composable declaration does not use composition and should not be marked @Composable.",
                "See https://mrmans0n.github.io/compose-rules/rules/#do-not-mark-functions-as-composable-when-they-dont-need-it for more information. [UnnecessaryComposable]",
            ),
            output,
        )
    }
}
