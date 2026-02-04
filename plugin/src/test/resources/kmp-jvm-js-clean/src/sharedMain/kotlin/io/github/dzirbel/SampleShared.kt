package io.github.dzirbel

fun buildShared(name: String): SampleCommon {
    val decorated = "shared:$name"
    return buildCommon(decorated)
}
