package io.github.dzirbel

import org.gradle.testfixtures.ProjectBuilder
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ConfigFileTest {

    @Test
    fun `build detekt config replaces placeholders`() {
        val project = ProjectBuilder.builder().build()
        project.createDetektConfigExtension()

        val config = project.buildDetektConfig().get()

        assertFalse(config.contains("<TEST_PATHS>"))
        assertFalse(config.contains("<FORBIDDEN_METHOD_CALLS>"))
        assertContains(config, "excludes: ['**/test/**', '**/androidTest/**', '**/testFixtures/**', '**/*Test/**']")
        assertContains(config, "reason: 'println does not allow you to configure the output stream.")
        assertContains(config, "value: 'kotlin.io.println'")
    }

    @Test
    fun `build detekt config escapes custom extension values`() {
        val project = ProjectBuilder.builder().build()
        val extension = project.createDetektConfigExtension()

        extension.testPaths.set(listOf("**/team's-tests/**", "**/dollar${'$'}path/**"))
        extension.forbiddenMethodCalls.set(
            listOf(
                DetektConfigExtension.ForbiddenMethodCall(
                    value = "com.example.Logger.log'One",
                    reason = "don't use this ${'$'}api",
                ),
                DetektConfigExtension.ForbiddenMethodCall(
                    value = "com.example.${'$'}debug",
                ),
            ),
        )

        val config = project.buildDetektConfig().get()

        assertContains(config, "excludes: ['**/team''s-tests/**', '**/dollar${'$'}path/**']")
        assertContains(config, "reason: 'don''t use this ${'$'}api'")
        assertContains(config, "value: 'com.example.Logger.log''One'")
        assertContains(config, "value: 'com.example.${'$'}debug'")
    }

    @Test
    fun `build detekt config keeps forbidden methods as empty list`() {
        val project = ProjectBuilder.builder().build()
        val extension = project.createDetektConfigExtension()

        extension.forbiddenMethodCalls.set(emptyList())

        val config = project.buildDetektConfig().get()

        assertFalse(config.contains("\r"))
        assertContains(config, "methods:\n      []")
        assertEquals(emptyList<Any>(), config.parseConfig().rule("style", "ForbiddenMethodCall")["methods"])
    }

    @Test
    fun `extension strings survive YAML parsing unchanged`() {
        val project = ProjectBuilder.builder().build()
        val extension = project.createDetektConfigExtension()
        val paths = listOf("**/team's-tests/**", "**/line\nbreak/**", "**/      <FORBIDDEN_METHOD_CALLS>/**")
        val reasons = listOf(
            "first line\nsecond line",
            "CRLF\r\ncarriage return\rtab\tcontrol\u0001",
            "Unicode breaks\u0085\u2028\u2029, quotes '\" and backslash \\",
            "literal <TEST_PATHS> and <FORBIDDEN_METHOD_CALLS>",
        )
        extension.testPaths.set(paths)
        extension.forbiddenMethodCalls.set(
            reasons.mapIndexed { index, reason ->
                DetektConfigExtension.ForbiddenMethodCall("example.method$index", reason)
            } + DetektConfigExtension.ForbiddenMethodCall("example.noReason"),
        )

        val config = project.buildDetektConfig().get().parseConfig()

        assertEquals(paths, config.rule("style", "MagicNumber")["excludes"])
        assertEquals(
            reasons.mapIndexed { index, reason -> mapOf("value" to "example.method$index", "reason" to reason) } +
                mapOf("value" to "example.noReason"),
            config.rule("style", "ForbiddenMethodCall")["methods"],
        )
    }

    @Test
    fun `base and Compose config form one YAML document without duplicate keys`() {
        val project = ProjectBuilder.builder().build()
        project.createDetektConfigExtension()

        val config = (project.buildDetektConfig().get() + "\n" + readResource("compose.yml")).parseConfig()

        assertEquals(true, config.rule("dzirbel", "InjectConstructorParameterOrder")["active"])
        assertEquals(true, config.rule("Compose", "ModifierMissing")["active"])
    }

    private fun String.parseConfig(): Map<*, *> =
        Yaml(SafeConstructor(LoaderOptions().apply { isAllowDuplicateKeys = false })).load(this)

    private fun Map<*, *>.rule(ruleSet: String, rule: String): Map<*, *> =
        (get(ruleSet) as Map<*, *>)[rule] as Map<*, *>
}
