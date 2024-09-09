package io.github.dzirbel

import java.util.Properties

private val classLoader = DetektConfigPlugin::class.java.classLoader

internal fun readResourceProperties(name: String): Properties {
    return classLoader.getResourceAsStream(name).use { stream ->
        Properties().apply { load(stream) }
    }
}

internal fun readResource(name: String): String? {
    val stream = checkNotNull(classLoader.getResourceAsStream(name)) { "resource $name could not be found" }
    return stream.use { it.bufferedReader().readLines().joinToString(separator = "\n") }
}
