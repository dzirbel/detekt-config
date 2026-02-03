package io.github.dzirbel.rules

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class RuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("dzirbel")

    override fun instance() = RuleSet(
        id = ruleSetId,
        rules = listOf(
            ::InjectConstructorParameterOrder,
        ),
    )
}
