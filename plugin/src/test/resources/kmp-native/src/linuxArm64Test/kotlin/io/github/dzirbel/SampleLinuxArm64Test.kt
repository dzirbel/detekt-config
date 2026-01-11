package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertEquals

class SampleLinuxArm64Test {
    @Test
    fun sampleLinuxArm64Test() {
        var x = mutableSetOf<String>()
        println("Hello $x")
        assertEquals(0, x.size)
    }
}
