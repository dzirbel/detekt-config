package io.github.dzirbel

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File

private val testPathRules = linkedMapOf(
    "complexity" to listOf("TooManyFunctions"),
    "exceptions" to listOf(
        "InstanceOfCheckForException",
        "ThrowingExceptionsWithoutMessageOrCause",
        "TooGenericExceptionCaught",
        "TooGenericExceptionThrown",
    ),
    "performance" to listOf("CouldBeSequence", "SpreadOperator"),
    "potential-bugs" to listOf("CastNullableToNonNullableType", "LateinitUsage"),
    "style" to listOf("MagicNumber"),
)

/**
 * Assembles bundled, Compose, generated, and project-owned layers into one YAML document so detekt validates
 * the effective configuration. Later layers take precedence while nested maps retain unspecified earlier values.
 */
internal fun buildDetektConfig(
    testPaths: List<String>,
    forbiddenMethodCalls: List<DetektConfigExtension.ForbiddenMethodCall>,
    compose: Boolean,
    files: Iterable<File>,
): String {
    val layers = buildList {
        add(loadYaml("base.yml", readResource("base.yml")))
        if (compose) add(loadYaml("compose.yml", readResource("compose.yml")))
        add(buildExtensionOverrides(testPaths, forbiddenMethodCalls))
        files.forEach { add(loadYaml(it)) }
    }
    val merged = layers.fold(linkedMapOf<String, Any>()) { config, layer ->
        config.apply { mergeFrom(layer) }
    }
    return Yaml(DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        defaultScalarStyle = DumperOptions.ScalarStyle.DOUBLE_QUOTED
        nonPrintableStyle = DumperOptions.NonPrintableStyle.ESCAPE
        splitLines = false
    }).dump(merged)
}

private fun buildExtensionOverrides(
    testPaths: List<String>,
    forbiddenMethodCalls: List<DetektConfigExtension.ForbiddenMethodCall>,
): Map<String, Any> = buildMap {
    testPathRules.forEach { (ruleSet, rules) ->
        put(ruleSet, buildMap {
            rules.forEach { rule -> put(rule, mapOf("excludes" to testPaths)) }
            if (ruleSet == "style") {
                put("ForbiddenMethodCall", mapOf("methods" to forbiddenMethodCalls.map { method ->
                    buildMap {
                        put("value", method.value)
                        method.reason?.let { put("reason", it) }
                    }
                }))
            }
        })
    }
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
