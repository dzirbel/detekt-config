package io.github.dzirbel

data class Sample(val name: String)

fun greet(name: String): Sample {
    val trimmed = name.trim()
    return Sample("Hello, $trimmed")
}
