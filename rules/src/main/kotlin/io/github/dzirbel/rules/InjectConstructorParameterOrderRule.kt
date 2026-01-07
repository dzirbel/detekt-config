package io.github.dzirbel.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

class InjectConstructorParameterOrderRule(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        id = "InjectConstructorParameterOrder",
        severity = Severity.Style,
        description = "Reports @Inject constructors whose parameters are not in alphabetical order.",
        debt = Debt.FIVE_MINS,
    )

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
                CodeSmell(
                    issue = issue,
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
