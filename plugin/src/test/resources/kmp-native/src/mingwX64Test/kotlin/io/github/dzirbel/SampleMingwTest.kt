package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertEquals

class SampleMingwTest {
    @Test
    fun sampleMingwTest() {
        var x = mutableSetOf<String>()
        println("Hello $x")
        assertEquals(0, x.size)
    }
}
