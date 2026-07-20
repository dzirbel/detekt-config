package io.github.dzirbel

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

class SampleCommonTest {
    @Test
    fun sampleCommonTest() {
        var x = mutableSetOf<String>()
        println("Hello $x")
        CoroutineScope(EmptyCoroutineContext)
        assertEquals(0, x.size)
    }
}
