package io.github.dzirbel

import java.util.Properties

private val classLoader = DetektConfigPlugin::class.java.classLoader

internal fun readResourceProperties(name: String): Properties {
    return requireResourceStream(name).use { stream ->
        Properties().apply { load(stream) }
    }
}

internal fun readResource(name: String): String {
    return requireResourceStream(name).use { it.reader().readText() }
}

private fun requireResourceStream(name: String) = checkNotNull(classLoader.getResourceAsStream(name)) {
    "resource $name could not be found"
}
