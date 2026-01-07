package io.github.dzirbel.rules

import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test

class InjectConstructorParameterOrderRuleTest {
    private val subject = InjectConstructorParameterOrderRule()

    @Test
    fun `reports when @Inject constructor parameters are not alphabetical`() {
        val findings = subject.lint(
            """
            import javax.inject.Inject

            class Sample @Inject constructor(
                val zebra: Zebra,
                val apple: Apple,
            )
            """.trimIndent(),
        )

        assertThat(findings)
            .singleElement()
            .hasSourceLocation(3, 14)
            .hasMessage("Constructor parameters should be in alphabetical order: apple, zebra.")
    }

    @Test
    fun `does not report when @Inject constructor parameters are alphabetical`() {
        val findings = subject.lint(
            """
            import javax.inject.Inject

            class Sample @Inject constructor(
                val apple: Apple,
                val zebra: Zebra,
            )
            """.trimIndent(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report when @Inject constructor has no parameters`() {
        val findings = subject.lint(
            """
            import javax.inject.Inject

            class Sample @Inject constructor()
            """.trimIndent(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report when @Inject constructor has one parameter`() {
        val findings = subject.lint(
            """
            import javax.inject.Inject

            class Sample @Inject constructor(
                val apple: Apple,
            )
            """.trimIndent(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports when @Inject constructor parameters are not alphabetical with defaults`() {
        val findings = subject.lint(
            """
            import javax.inject.Inject

            class Sample @Inject constructor(
                val zebra: Zebra = Zebra(),
                val apple: Apple = Apple(),
            )
            """.trimIndent(),
        )

        assertThat(findings)
            .singleElement()
            .hasSourceLocation(3, 14)
            .hasMessage("Constructor parameters should be in alphabetical order: apple, zebra.")
    }

    @Test
    fun `reports when @Inject constructor uses fully qualified annotation`() {
        val findings = subject.lint(
            """
            class Sample @javax.inject.Inject constructor(
                val zebra: Zebra,
                val apple: Apple,
            )
            """.trimIndent(),
        )

        assertThat(findings)
            .singleElement()
            .hasSourceLocation(1, 14)
            .hasMessage("Constructor parameters should be in alphabetical order: apple, zebra.")
    }

    @Test
    fun `does not report when constructor is not @Inject`() {
        val findings = subject.lint(
            """
            class Sample constructor(
                val zebra: Zebra,
                val apple: Apple,
            )
            """.trimIndent(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `does not report when @Inject secondary constructor parameters are alphabetical`() {
        val findings = subject.lint(
            """
            import javax.inject.Inject

            class Sample {
                constructor()

                @Inject constructor(
                    apple: Apple,
                    zebra: Zebra,
                ) : this()
            }
            """.trimIndent(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `reports when @Inject secondary constructor parameters are not alphabetical`() {
        val findings = subject.lint(
            """
            import javax.inject.Inject

            class Sample {
                constructor()

                @Inject constructor(
                    zebra: Zebra,
                    apple: Apple,
                ) : this()
            }
            """.trimIndent(),
        )

        assertThat(findings)
            .singleElement()
            .hasSourceLocation(6, 5)
            .hasMessage("Constructor parameters should be in alphabetical order: apple, zebra.")
    }
}
