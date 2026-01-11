package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertEquals

class SampleIosTest {
    @Test
    fun sampleIosTest() {
        var x = mutableSetOf<String>()
        println("Hello $x")
        assertEquals(0, x.size)
    }
}
