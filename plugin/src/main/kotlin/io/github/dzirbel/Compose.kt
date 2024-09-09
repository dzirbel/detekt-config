package io.github.dzirbel

import org.gradle.api.Project
import java.util.concurrent.atomic.AtomicBoolean

internal val Project.hasCompose: Boolean
    get() = COMPOSE_PLUGINS.any { pluginManager.hasPlugin(it) }

internal fun Project.withCompose(block: () -> Unit) {
    val run = AtomicBoolean(false)
    for (composePlugin in COMPOSE_PLUGINS) {
        pluginManager.withPlugin(composePlugin) {
            if (!run.getAndSet(true)) {
                block()
            }
        }
    }
}

private val COMPOSE_PLUGINS = listOf(
    "org.jetbrains.compose",
    "org.jetbrains.kotlin.plugin.compose",
    // assume Android applications want Compose configuration
    "com.android.application",
    "com.android.library",
)
