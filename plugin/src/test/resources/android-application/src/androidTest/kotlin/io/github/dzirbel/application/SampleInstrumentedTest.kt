package io.github.dzirbel.application

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext

fun instrumentedTestScope(): CoroutineScope {
    ApplicationMarker.hashCode()
    return CoroutineScope(EmptyCoroutineContext)
}
