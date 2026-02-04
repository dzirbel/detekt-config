package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertEquals

class SampleJvmTest {
    @Test
    fun buildsJvm() {
        val result = buildJvm("Ada")
        assertEquals(SampleCommon("shared:jvm:Ada"), result)
    }
}
