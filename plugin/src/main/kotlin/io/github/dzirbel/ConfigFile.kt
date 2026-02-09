package io.github.dzirbel

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

private const val testPathsPlaceholder = "<TEST_PATHS>"
private const val forbiddenMethodCallsPlaceholder = "<FORBIDDEN_METHOD_CALLS>"
private const val indentedForbiddenMethodCallsPlaceholder = "      $forbiddenMethodCallsPlaceholder"

internal fun Project.buildDetektConfig(): Provider<String> {
    return providers.provider {
        val extension = extensions.getByType<DetektConfigExtension>()

        buildString {
            appendLine(readResource("base.yml"))

            if (hasCompose) {
                appendLine(readResource("compose.yml"))
            }
        }
            .run {
                val testPaths = extension.testPaths.get()
                    .joinToString(separator = ", ", prefix = "[", postfix = "]") { it.toYamlSingleQuotedString() }
                replace(testPathsPlaceholder, testPaths)
            }
            .run {
                val forbiddenMethodCalls = extension.forbiddenMethodCalls.get()
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = "\n") { forbiddenMethodCall ->
                        val reasonLine = forbiddenMethodCall.reason?.let {
                            "      - reason: ${it.toYamlSingleQuotedString()}\n"
                        } ?: "      -\n"
                        buildString {
                            append(reasonLine)
                            append("        value: ${forbiddenMethodCall.value.toYamlSingleQuotedString()}")
                        }
                    }
                    ?: "      []"
                // Keep indentation in the template and only replace the explicit placeholder token.
                replace(indentedForbiddenMethodCallsPlaceholder, forbiddenMethodCalls)
            }
    }
}

private fun String.toYamlSingleQuotedString(): String {
    return "'${replace("'", "''")}'"
}
