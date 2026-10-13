package io.github.dzirbel

import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test

class JvmBaselineProjectTest : SampleProjectTest("jvm-baseline") {
    @Test
    fun `baseline ignores the same excluded file as analysis`() {
        val included = projectDir.resolve("src/main/kotlin/sample/Included.kt")
        val analysis = projectDir.gradle("detektMain").buildAndFail()
        assertSameContents(
            listOf(
                "${included.absolutePath}:4:5: The method `kotlin.io.println` has been forbidden: " +
                    "println does not allow you to configure the output stream. Use a logger instead. [ForbiddenMethodCall]",
            ),
            assertTaskFailed(analysis, ":jvm-baseline:detektMain"),
        )

        val generated = projectDir.gradle("detektBaselineMain").build()
        assertTaskPassed(generated, ":jvm-baseline:detektBaselineMain")
        assertSameContents(
            listOf("ForbiddenMethodCall:Included.kt:println(\"Included\")"),
            baselineEntries(),
        )

        // The saved warning is ignored on the next analysis run.
        val suppressed = projectDir.gradle("detektMain").build()
        assertDetektTaskPassed(suppressed, ":jvm-baseline:detektMain")
    }

    @Test
    fun `baseline includes both files when neither is excluded`() {
        val generated = projectDir.gradle("detektBaselineMain", "-PincludeExcludedFile").build()
        assertTaskPassed(generated, ":jvm-baseline:detektBaselineMain")
        assertSameContents(
            listOf(
                "ForbiddenMethodCall:Included.kt:println(\"Included\")",
                "ForbiddenMethodCall:Excluded.kt:println(\"Excluded\")",
            ),
            baselineEntries(),
        )
    }

    private fun baselineEntries(): List<String> {
        val file = projectDir.resolve("build/baseline-main.xml")
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val entries = document.getElementsByTagName("ID")
        return (0 until entries.length).map { entries.item(it).textContent }
    }
}
