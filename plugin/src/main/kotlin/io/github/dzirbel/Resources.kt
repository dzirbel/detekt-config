package io.github.dzirbel

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Properties

private val classLoader = DetektConfigPlugin::class.java.classLoader

internal fun readResourceProperties(name: String): Properties {
    return requireResourceStream(name).use { stream ->
        Properties().apply { load(stream) }
    }
}

internal fun readResource(name: String): String {
    return requireResourceStream(name).use { stream ->
        stream.bufferedReader(UTF_8).readText().normalizeLineEndings()
    }
}

private fun requireResourceStream(name: String) = checkNotNull(classLoader.getResourceAsStream("io/github/dzirbel/detekt-config/$name")) {
    "resource $name could not be found"
}

private fun String.normalizeLineEndings(): String {
    return replace("\r\n", "\n").replace("\r", "\n")
}
