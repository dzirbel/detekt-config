package io.github.dzirbel.rules

import dev.detekt.api.Config
import dev.detekt.test.lint
import dev.detekt.test.assertj.assertThat
import kotlin.test.Test

class InjectConstructorParameterOrderTest {
    private val subject = InjectConstructorParameterOrder(Config.empty)

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
            .hasStartSourceLocation(3, 14)
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
            .hasStartSourceLocation(3, 14)
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
            .hasStartSourceLocation(1, 14)
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
            .hasStartSourceLocation(6, 5)
            .hasMessage("Constructor parameters should be in alphabetical order: apple, zebra.")
    }

    @Test
    fun `orders parameter names rather than their types and reports the complete order`() {
        val findings = subject.lint(
            """
            class Sample @javax.inject.Inject constructor(
                zebra: Apple,
                apple: Zebra,
                middle: Middle,
            )
            """.trimIndent(),
        )

        assertThat(findings)
            .singleElement()
            .hasMessage("Constructor parameters should be in alphabetical order: apple, middle, zebra.")
    }

    @Test
    fun `class and parameter annotations do not mark a constructor as injected`() {
        val findings = subject.lint(
            """
            @Inject
            class Sample constructor(
                @Inject val zebra: Zebra,
                val apple: Apple,
            )
            """.trimIndent(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `secondary constructors without Inject are ignored`() {
        val findings = subject.lint(
            """
            class Sample {
                constructor(zebra: Zebra, apple: Apple)
            }
            """.trimIndent(),
        )

        assertThat(findings).isEmpty()
    }

    @Test
    fun `each unordered Inject constructor reports exactly once`() {
        val findings = subject.lint(
            """
            class Sample @javax.inject.Inject constructor(zebra: Zebra, apple: Apple) {
                @javax.inject.Inject constructor(zebra: Zebra, apple: Apple, middle: Middle) : this(zebra, apple)
            }
            """.trimIndent(),
        )

        assertThat(findings).hasSize(2)
        assertThat(findings[0])
            .hasStartSourceLocation(1, 14)
            .hasMessage("Constructor parameters should be in alphabetical order: apple, zebra.")
        assertThat(findings[1])
            .hasStartSourceLocation(2, 5)
            .hasMessage("Constructor parameters should be in alphabetical order: apple, middle, zebra.")
    }
}
