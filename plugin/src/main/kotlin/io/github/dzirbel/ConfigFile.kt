package io.github.dzirbel

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

private const val testPathsPlaceholder = "<TEST_PATHS>"
private const val forbiddenMethodCallsPlaceholder = "<FORBIDDEN_METHOD_CALLS>"
private const val indentedForbiddenMethodCallsPlaceholder = "      $forbiddenMethodCallsPlaceholder"
private val configPlaceholderRegex = Regex("$testPathsPlaceholder|$indentedForbiddenMethodCallsPlaceholder")

internal fun Project.buildDetektConfig(): Provider<String> {
    return providers.provider {
        val extension = extensions.getByType<DetektConfigExtension>()
        val testPaths = extension.testPaths.get()
            .joinToString(separator = ", ", prefix = "[", postfix = "]") { it.toYamlQuotedString() }
        val forbiddenMethodCalls = extension.forbiddenMethodCalls.get()
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n") { forbiddenMethodCall ->
                buildString {
                    val reason = forbiddenMethodCall.reason
                    appendLine(if (reason == null) "      -" else "      - reason: ${reason.toYamlQuotedString()}")
                    append("        value: ${forbiddenMethodCall.value.toYamlQuotedString()}")
                }
            }
            ?: "      []"

        buildString {
            appendLine(readResource("base.yml"))

            if (hasCompose) {
                appendLine(readResource("compose.yml"))
            }
        }
            .replace(configPlaceholderRegex) { match ->
                // Replace only template tokens, never placeholder-like text inside extension values.
                when (match.value) {
                    testPathsPlaceholder -> testPaths
                    else -> forbiddenMethodCalls
                }
            }
    }
}

private fun String.toYamlQuotedString(): String {
    if (none { it.requiresYamlEscape() }) return "'${replace("'", "''")}'"

    // Single-quoted YAML folds line breaks and cannot represent control characters. Use escaped double quotes instead.
    return buildString {
        append('"')
        for (character in this@toYamlQuotedString) {
            when {
                character == '"' || character == '\\' -> append('\\').append(character)
                character.requiresYamlEscape() -> append("\\u").append(character.code.toString(16).padStart(4, '0'))
                else -> append(character)
            }
        }
        append('"')
    }
}

private fun Char.requiresYamlEscape(): Boolean =
    code < 0x20 || code in 0x7f..0x9f || this == '\u2028' || this == '\u2029'
