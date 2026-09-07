package io.github.dzirbel

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.nodes.Tag
import java.io.File

/**
 * Resolves explicit extension placeholders in bundled YAML, then merges optional Compose and project-owned layers.
 * Later layers take precedence while nested maps retain unspecified earlier values.
 */
internal fun buildDetektConfig(
    testPaths: List<String>,
    forbiddenMethodCalls: List<DetektConfigExtension.ForbiddenMethodCall>,
    compose: Boolean,
    files: Iterable<File>,
): String {
    val yaml = Yaml(DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        defaultScalarStyle = DumperOptions.ScalarStyle.DOUBLE_QUOTED
        nonPrintableStyle = DumperOptions.NonPrintableStyle.ESCAPE
        splitLines = false
    })
    val placeholders = mapOf(
        "TEST_PATHS" to testPaths,
        "FORBIDDEN_METHOD_CALLS" to forbiddenMethodCalls.map { method ->
            buildMap<String, String> {
                put("value", method.value)
                method.reason?.let { put("reason", it) }
            }
        },
    ).mapValues { (_, value) -> yaml.dumpAs(value, Tag.SEQ, DumperOptions.FlowStyle.FLOW).trimEnd() }

    fun loadBundledConfig(name: String): Map<String, Any> {
        val contents = Regex("'<(TEST_PATHS|FORBIDDEN_METHOD_CALLS)>'").replace(readResource(name)) { match ->
            placeholders.getValue(match.groupValues[1])
        }
        return loadYaml(name, contents)
    }

    val layers = buildList {
        add(loadBundledConfig("base.yml"))
        if (compose) add(loadBundledConfig("compose.yml"))
        files.forEach { add(loadYaml(it)) }
    }
    val merged = layers.fold(linkedMapOf<String, Any>()) { config, layer ->
        config.apply { mergeFrom(layer) }
    }
    return yaml.dump(merged)
}

private fun loadYaml(file: File): Map<String, Any> = try {
    loadYaml(file.path, file.readText())
} catch (exception: Exception) {
    throw IllegalArgumentException("Cannot load detekt configuration: $file", exception)
}

private fun loadYaml(name: String, contents: String): Map<String, Any> {
    val loaded = Yaml(SafeConstructor(LoaderOptions().apply {
        isAllowDuplicateKeys = false
        allowRecursiveKeys = false
    })).load<Any>(contents) ?: return emptyMap()
    require(loaded is Map<*, *>) { "Detekt configuration must be a YAML map: $name" }

    return loaded.entries.associateTo(linkedMapOf()) { (key, value) ->
        require(key is String) { "Detekt configuration keys must be strings: $name" }
        key to requireNotNull(value) { "Detekt configuration values must not be null: $name > $key" }
    }
}

private fun MutableMap<String, Any>.mergeFrom(layer: Map<String, Any>) {
    layer.forEach { (key, value) ->
        val existing = this[key]
        this[key] = if (existing is Map<*, *> && value is Map<*, *>) {
            existing.toStringKeyedMutableMap().apply {
                mergeFrom(value.toStringKeyedMap())
            }
        } else {
            value
        }
    }
}

private fun Map<*, *>.toStringKeyedMutableMap(): MutableMap<String, Any> =
    toStringKeyedMap().toMap(linkedMapOf())

private fun Map<*, *>.toStringKeyedMap(): Map<String, Any> =
    entries.associateTo(linkedMapOf()) { (key, value) ->
        require(key is String) { "Detekt configuration keys must be strings" }
        key to requireNotNull(value) { "Detekt configuration values must not be null: $key" }
    }
