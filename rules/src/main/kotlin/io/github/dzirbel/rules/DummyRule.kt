package io.github.dzirbel.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtFile

class DummyRule(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        id = "DummyRule",
        severity = Severity.Style,
        description = "Reports Kotlin files without an explicit package declaration.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        if (file.packageFqName.isRoot) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(file),
                    message = "File has no package declaration.",
                ),
            )
        }
    }
}
