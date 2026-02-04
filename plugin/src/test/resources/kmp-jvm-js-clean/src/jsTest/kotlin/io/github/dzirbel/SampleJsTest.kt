package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertEquals

class SampleJsTest {
    @Test
    fun buildsJs() {
        val result = buildJs("Ada")
        assertEquals(SampleCommon("shared:js:Ada"), result)
    }
}
