package io.github.dzirbel

import org.gradle.api.Project

internal val Project.hasCompose: Boolean
    get() = COMPOSE_PLUGINS.any { pluginManager.hasPlugin(it) } || hasAndroidComposeFeature

internal fun Project.withCompose(block: () -> Unit) {
    var run = false
    fun runOnce() {
        if (!run) {
            run = true
            block()
        }
    }

    COMPOSE_PLUGINS.forEach { composePlugin ->
        pluginManager.withPlugin(composePlugin) {
            runOnce()
        }
    }
    ANDROID_PLUGINS.forEach { androidPlugin ->
        pluginManager.withPlugin(androidPlugin) {
            afterEvaluate {
                if (hasAndroidComposeFeature) {
                    runOnce()
                }
            }
        }
    }
}

private val Project.hasAndroidComposeFeature: Boolean
    get() {
        if (ANDROID_PLUGINS.none { pluginManager.hasPlugin(it) }) return false

        val android = extensions.findByName("android") ?: return false
        val buildFeatures = android.readProperty("buildFeatures") ?: return false
        return buildFeatures.readProperty("compose") == true
    }

private fun Any.readProperty(name: String): Any? {
    val getterName = "get${name.replaceFirstChar { it.uppercaseChar() }}"
    return javaClass.methods
        .firstOrNull { method -> method.name == getterName && method.parameterCount == 0 }
        ?.invoke(this)
}

private val COMPOSE_PLUGINS = listOf(
    "org.jetbrains.compose",
    "org.jetbrains.kotlin.plugin.compose",
)

private val ANDROID_PLUGINS = listOf(
    "com.android.application",
    "com.android.library",
)
