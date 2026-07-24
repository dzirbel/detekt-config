package io.github.dzirbel.application

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext

fun unitTestScope(): CoroutineScope {
    ApplicationMarker.hashCode()
    return CoroutineScope(EmptyCoroutineContext)
}
