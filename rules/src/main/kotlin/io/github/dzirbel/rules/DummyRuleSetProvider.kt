package io.github.dzirbel.rules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetId
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class DummyRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = "dzirbel"

    override fun instance(config: Config): RuleSet = RuleSet(ruleSetId, listOf(DummyRule(config)))
}
