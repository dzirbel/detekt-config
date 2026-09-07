package io.github.dzirbel

import org.gradle.api.GradleException
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

internal fun checkAnalysisResolution(taskName: String, failures: Collection<Throwable>) {
    failures.forEach { failure ->
        if (failure.isNonJvmDependency()) {
            Logging.getLogger("io.github.dzirbel.detekt-config").warn(
                "$taskName: skipping a dependency with no JVM variant; its types are unavailable to detekt. " +
                    failure.message,
            )
        } else {
            throw GradleException("Could not resolve JVM analysis classpath for $taskName", failure)
        }
    }
}

// Gradle's public ArtifactCollection exposes Throwables without a structured failure category. Inspect the
// internal variant failure reflectively: Gradle's API jar relocates its collection return types. Keep this
// compatibility boundary isolated and fail closed if the structure changes; never classify error-message text.
private fun Throwable.isNonJvmDependency(): Boolean {
    val selectionFailure = generateSequence(this) { it.cause }
        .mapNotNull { it.resolutionProperty("getFailure") }
        .firstOrNull {
            it.javaClass.name == "org.gradle.internal.component.resolution.failure.type.NoCompatibleVariantsFailure"
        } ?: return false
    val candidates = selectionFailure.resolutionProperty("getCandidates") as? List<*> ?: return false
    val attributes = candidates.map { candidate ->
        candidate?.resolutionProperty("getAllCandidateAttributes") as? AttributeContainer ?: return false
    }.filter { it.namedValue(Category.CATEGORY_ATTRIBUTE.name) != Category.DOCUMENTATION }

    return attributes.isNotEmpty() && attributes.all {
        it.namedValue(KotlinPlatformType.attribute.name) in setOf("js", "native", "common", "wasm")
    }
}

private fun Any.resolutionProperty(getter: String): Any? = try {
    javaClass.getMethod(getter).invoke(this)
} catch (_: ReflectiveOperationException) {
    null
}

private fun AttributeContainer.namedValue(name: String): String? =
    keySet().firstOrNull { it.name == name }?.let { getAttribute(it)?.toString() }
