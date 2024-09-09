package io.github.dzirbel

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

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
                    .joinToString(separator = ", ", prefix = "[", postfix = "]") { "'$it'" }
                replace("<TEST_PATHS>".toRegex(), testPaths)
            }
            .run {
                val forbiddenMethodCalls = extension.forbiddenMethodCalls.get()
                    .map { forbiddenMethodCall ->
                        """
                            - reason: '${forbiddenMethodCall.reason}'
                              value: '${forbiddenMethodCall.value}'
                        """.replaceIndent(newIndent = " ".repeat(6))
                    }
                    .joinToString(separator = "\n")
                replace(" *<FORBIDDEN_METHOD_CALLS>".toRegex(), forbiddenMethodCalls)
            }
    }
}
