package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertEquals

class SampleTest {
    @Test
    fun greetFormatsName() {
        val result = greet("Ada")
        assertEquals(Sample("Hello, Ada"), result)
    }
}
