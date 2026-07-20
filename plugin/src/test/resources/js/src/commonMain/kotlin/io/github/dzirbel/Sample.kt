package io.github.dzirbel

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext

fun sample() {
    var x = mutableSetOf<String>()
    println("Hello $x")
    CoroutineScope(EmptyCoroutineContext)
}
