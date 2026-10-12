package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertEquals

class SampleTestUtilsTest {
    @Test
    fun `findings belong to the exact task path`() {
        val output = """
            > Task :app:detektMain
            w: /src/Main.kt:3:5 main warning [Rule]
            > Task :app:detektMainJs FAILED
            w: /src/Js.kt:4:6 JS warning [Rule]
            > Task :other:detektMain FAILED
            w: /src/Other.kt:5:7 other warning [Rule]
        """.trimIndent()

        assertEquals(listOf("/src/Main.kt:3:5: main warning [Rule]"), output.findTaskOutput(":app:detektMain"))
    }

    @Test
    fun `repeated task headers resume collection without duplicating findings`() {
        val output = """
            > Task :app:detektMain
            w: /src/Main.kt:3:5 first warning [Rule]
            > Task :app:compileTestKotlin
            w: /src/Test.kt:4:6 compiler warning
            > Task :app:detektMain FAILED
            w: /src/Main.kt:5:7 second warning [Rule]
        """.trimIndent()

        assertEquals(
            listOf("/src/Main.kt:3:5: first warning [Rule]", "/src/Main.kt:5:7: second warning [Rule]"),
            output.findTaskOutput(":app:detektMain"),
        )
    }

    @Test
    fun `ANSI formatting and CRLF do not hide diagnostics`() {
        val output = "\u001B[1m> Task :app:detektMain\u001B[0m\r\n" +
            "\u001B[33mw: /src/Main.kt:3:5 warning [Rule]\u001B[0m\r\n"

        assertEquals(listOf("/src/Main.kt:3:5: warning [Rule]"), output.findTaskOutput(":app:detektMain"))
    }

    @Test
    fun `compiler error summaries must not be discarded as Gradle noise`() {
        val output = """
            > Task :app:detektMain FAILED
            e: /src/Main.kt:3:5 unresolved reference
            There were 1 compiler errors found during analysis. This affects accuracy of reporting.
            Run detekt CLI with --debug to see compiler diagnostics.

            BUILD FAILED in 1s
            2 actionable tasks: 2 executed
        """.trimIndent()

        assertEquals(
            listOf(
                "/src/Main.kt:3:5: unresolved reference",
                "There were 1 compiler errors found during analysis. This affects accuracy of reporting.",
                "Run detekt CLI with --debug to see compiler diagnostics.",
            ),
            output.findTaskOutput(":app:detektMain"),
        )
    }

    @Test
    fun `missing task has no output`() {
        assertEquals(emptyList(), "> Task :app:compileKotlin\nw: /src/Main.kt:3:5 warning".findTaskOutput(":app:detektMain"))
    }
}
