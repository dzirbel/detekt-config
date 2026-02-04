package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertEquals

class SampleTest {
    @Test
    fun sampleTest() {
        var x = mutableSetOf<String>()
        println("Hello $x")
        assertEquals(0, x.size)
    }
}
