package io.github.dzirbel

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext

fun sampleJs() {
    var x = mutableSetOf<String>()
    println("Hello $x")
    CoroutineScope(EmptyCoroutineContext)
}
