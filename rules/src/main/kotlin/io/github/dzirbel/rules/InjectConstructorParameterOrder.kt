package io.github.dzirbel.rules

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

class InjectConstructorParameterOrder(config: Config) : Rule(
    config = config,
    description = "Reports @Inject constructors whose parameters are not in alphabetical order.",
) {

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
        super.visitPrimaryConstructor(constructor)
        checkParameterOrder(constructor)
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
        super.visitSecondaryConstructor(constructor)
        checkParameterOrder(constructor)
    }

    private fun checkParameterOrder(constructor: KtConstructor<*>) {
        if (!constructor.hasInjectAnnotation()) return

        val names = constructor.valueParameters.map { it.name }
        if (names.any { it == null }) return

        val parameterNames = names.filterNotNull()
        if (parameterNames.size < 2) return

        val sortedNames = parameterNames.sorted()
        if (parameterNames != sortedNames) {
            report(
                Finding(
                    entity = Entity.from(constructor),
                    message = "Constructor parameters should be in alphabetical order: " +
                        "${sortedNames.joinToString()}.",
                ),
            )
        }
    }

    private fun KtConstructor<*>.hasInjectAnnotation(): Boolean =
        annotationEntries.any { it.shortName?.asString() == "Inject" }
}
