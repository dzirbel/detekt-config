package io.github.dzirbel

data class SampleCommon(val name: String)

fun buildCommon(name: String): SampleCommon {
    val trimmed = name.trim()
    return SampleCommon(trimmed)
}
