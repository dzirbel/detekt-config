package io.github.dzirbel.rules

import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DummyRuleTest {
    private val subject = DummyRule()

    @Test
    fun `reports when file has no package`() {
        val findings = subject.lint(
            """
            class TestClass
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
        assertEquals("File has no package declaration.", findings.single().message)
    }

    @Test
    fun `does not report when file has package`() {
        val findings = subject.lint(
            """
            package foo.bar

            class TestClass
            """.trimIndent(),
        )

        assertTrue(findings.isEmpty())
    }
}
