package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertEquals

class SampleCommonTest {
    @Test
    fun buildsCommon() {
        val result = buildCommon(" Ada ")
        assertEquals(SampleCommon("Ada"), result)
    }
}
