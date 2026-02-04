package io.github.dzirbel

fun buildJs(name: String): SampleCommon {
    val qualified = "js:$name"
    return buildShared(qualified)
}
