package io.github.dzirbel

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.kotlin.dsl.create

interface DetektConfigExtension {
    data class ForbiddenMethodCall(val value: String, val reason: String? = null)

    /**
     * List of regex paths (e.g. `** /test/ **`, without the spaces) which should be excluded for rules that are allowed
     * in tests (such as magic numbers).
     */
    val testPaths: ListProperty<String>

    /**
     * List of fully-qualified method calls which are forbidden; these can be very project-specific so it may be
     * configured per-project.
     */
    val forbiddenMethodCalls: ListProperty<ForbiddenMethodCall>

    companion object {
        val DEFAULT_TEST_PATHS = listOf("**/test/**", "**/androidTest/**", "**/testFixtures/**")

        val DEFAULT_FORBIDDEN_METHOD_CALLS = listOf(
            ForbiddenMethodCall(
                value = "kotlin.io.print",
                reason = "print does not allow you to configure the output stream. Use a logger instead.",
            ),
            ForbiddenMethodCall(
                value = "kotlin.io.println",
                reason = "println does not allow you to configure the output stream. Use a logger instead.",
            ),
        )
    }
}

internal fun Project.createDetektConfigExtension(): DetektConfigExtension {
    return extensions.create<DetektConfigExtension>("detektConfig").apply {
        testPaths.convention(DetektConfigExtension.DEFAULT_TEST_PATHS)
        forbiddenMethodCalls.convention(DetektConfigExtension.DEFAULT_FORBIDDEN_METHOD_CALLS)
    }
}
