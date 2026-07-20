package io.github.dzirbel

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

class SampleJsTest {
    @Test
    fun sampleJsTest() {
        var x = mutableSetOf<String>()
        println("Hello $x")
        CoroutineScope(EmptyCoroutineContext)
        assertEquals(0, x.size)
    }
}
