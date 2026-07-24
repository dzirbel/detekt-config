package io.github.dzirbel.application

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext

data object ApplicationMarker

fun applicationScope(): CoroutineScope {
    ApplicationMarker.hashCode()
    return CoroutineScope(EmptyCoroutineContext)
}
