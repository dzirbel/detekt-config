package sample

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

class SampleIntegrationTest {
    @Test
    fun sample() {
        println(42)
        CoroutineScope(EmptyCoroutineContext)
        assertEquals(42, 42)
    }
}
