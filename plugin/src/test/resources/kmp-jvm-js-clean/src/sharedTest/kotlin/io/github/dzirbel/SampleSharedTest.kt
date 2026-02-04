package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertEquals

class SampleSharedTest {
    @Test
    fun buildsShared() {
        val result = buildShared("Ada")
        assertEquals(SampleCommon("shared:Ada"), result)
    }
}
