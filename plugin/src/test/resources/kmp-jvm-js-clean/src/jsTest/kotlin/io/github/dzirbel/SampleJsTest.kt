package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertEquals

class SampleJsTest {
    @Test
    fun buildsSharedCodeForJs() {
        val result = buildShared("Ada")
        assertEquals(SampleCommon("shared:Ada"), result)
    }
}
