package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class AutoCorrectProjectTest : SampleProjectTest("auto-correct") {
    private val main = projectDir.resolve("src/main/kotlin/sample/Sample.kt")
    private val test = projectDir.resolve("src/test/kotlin/sample/SampleTest.kt")

    @Test
    fun `without auto correct detekt reports the semicolons and leaves both files unchanged`() {
        val originalMain = main.readText()
        val originalTest = test.readText()

        val result = projectDir.gradle("detekt").buildAndFail()

        assertFailedTasks(result, ":auto-correct:detektMain", ":auto-correct:detektTest")
        assertContains(assertTaskFailed(result, ":auto-correct:detektMain").single(), "[NoSemicolons]")
        assertContains(assertTaskFailed(result, ":auto-correct:detektTest").single(), "[NoSemicolons]")
        assertEquals(originalMain, main.readText())
        assertEquals(originalTest, test.readText())
    }

    @Test
    fun `root auto correct removes the semicolons from both main and test files`() {
        val correctedMain = main.readText().replace("1;", "1")
        val correctedTest = test.readText().replace("1;", "1")
        val arguments = arrayOf(
            "detekt", "--auto-correct",
            "--configuration-cache", "--configuration-cache-problems=fail",
        )

        val result = projectDir.gradle(*arguments).build()

        assertDetektTaskPassed(result, ":auto-correct:detektMain")
        assertDetektTaskPassed(result, ":auto-correct:detektTest")
        assertTaskNoSource(result, ":auto-correct:detekt")
        assertEquals(correctedMain, main.readText())
        assertEquals(correctedTest, test.readText())

        val repeated = projectDir.gradle(*arguments).build()
        assertContains(repeated.output, "Reusing configuration cache.")
        assertDetektTaskPassed(repeated, ":auto-correct:detektMain")
        assertDetektTaskPassed(repeated, ":auto-correct:detektTest")
        assertEquals(correctedMain, main.readText())
        assertEquals(correctedTest, test.readText())
    }

    @Test
    fun `main task opt out leaves its semicolon while root auto correct still fixes the test file`() {
        val originalMain = main.readText()
        val correctedTest = test.readText().replace("1;", "1")

        val result = projectDir.gradle("detekt", "--auto-correct", "-PdisableMainAutoCorrect").buildAndFail()

        assertFailedTasks(result, ":auto-correct:detektMain")
        assertContains(assertTaskFailed(result, ":auto-correct:detektMain").single(), "[NoSemicolons]")
        assertDetektTaskPassed(result, ":auto-correct:detektTest")
        assertEquals(originalMain, main.readText())
        assertEquals(correctedTest, test.readText())
    }
}
