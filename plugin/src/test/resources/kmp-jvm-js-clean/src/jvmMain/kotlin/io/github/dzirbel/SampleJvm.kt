package io.github.dzirbel

fun buildJvm(name: String): SampleCommon {
    val qualified = "jvm:$name"
    return buildShared(qualified)
}
