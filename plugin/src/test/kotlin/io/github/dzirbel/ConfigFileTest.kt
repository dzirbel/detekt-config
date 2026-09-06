package io.github.dzirbel

import org.gradle.testfixtures.ProjectBuilder
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse

class ConfigFileTest {

    @Test
    fun `generic resource names cannot shadow bundled policy`() {
        val generic = javaClass.classLoader.getResource("base.yml")!!.readText()
        assertContains(generic, "unrelated: true")
        assertFalse(readResource("base.yml").contains("unrelated: true"))
        assertEquals(true, readResource("base.yml").parse().value("style", "MagicNumber", "active"))
    }

    @Test
    fun `later files replace lists and preserve literal placeholder text`() {
        val project = ProjectBuilder.builder().build()
        val extension = project.createDetektConfigExtension()
        val first = project.file("first.yml").apply {
            writeText("style:\n  MagicNumber:\n    excludes: ['<TEST_PATHS>']\n    active: false\n")
        }
        val second = project.file("second.yml").apply {
            writeText("style:\n  MagicNumber:\n    excludes: []\n")
        }
        extension.config.from(first, second)
        val config = project.buildDetektConfig().get().parse()
        assertEquals(emptyList<String>(), config.value("style", "MagicNumber", "excludes"))
        assertEquals(false, config.value("style", "MagicNumber", "active"))
        extension.config.setFrom(second, first)
        assertEquals(listOf("<TEST_PATHS>"), project.buildDetektConfig().get().parse()
            .value("style", "MagicNumber", "excludes"))
    }

    @Test
    fun `assembled config parses and recursively merges generated values with bundled defaults`() {
        val project = ProjectBuilder.builder().build()
        project.createDetektConfigExtension()

        val assembled = project.buildDetektConfig().get()
        val config = assembled.parse()

        assertFalse(placeholderRegex.containsMatchIn(assembled), "unresolved placeholder in:\n$assembled")
        assertEquals(
            DetektConfigExtension.DEFAULT_TEST_PATHS,
            config.value("complexity", "TooManyFunctions", "excludes"),
        )
        assertEquals(25, config.value("complexity", "TooManyFunctions", "allowedFunctionsPerFile"))
        assertEquals(
            DetektConfigExtension.DEFAULT_FORBIDDEN_METHOD_CALLS.map { it.value },
            config.value<List<Map<String, String>>>("style", "ForbiddenMethodCall", "methods").map { it["value"] },
        )
    }

    @Test
    fun `assembled config preserves quoted and escaped extension values`() {
        val project = ProjectBuilder.builder().build()
        val extension = project.createDetektConfigExtension()
        val testPaths = listOf(
            "**/team's-tests/**",
            "**/dollar${'$'}path/**",
            "**/line\nbreak/**",
            "**/tab\tpath/**",
        )
        val forbiddenMethodCalls = listOf(
            DetektConfigExtension.ForbiddenMethodCall(
                value = "com.example.Logger.log'One",
                reason = "don't use this ${'$'}api\nuse the logger instead",
            ),
            DetektConfigExtension.ForbiddenMethodCall(
                value = "com.example.${'$'}debug",
            ),
        )
        extension.testPaths.set(testPaths)
        extension.forbiddenMethodCalls.set(forbiddenMethodCalls)

        val config = project.buildDetektConfig().get().parse()

        assertEquals(testPaths, config.value("style", "MagicNumber", "excludes"))
        assertEquals(
            listOf(
                mapOf(
                    "value" to forbiddenMethodCalls[0].value,
                    "reason" to forbiddenMethodCalls[0].reason,
                ),
                mapOf("value" to forbiddenMethodCalls[1].value),
            ),
            config.value("style", "ForbiddenMethodCall", "methods"),
        )
    }

    @Test
    fun `assembled config keeps extension values as empty lists`() {
        val project = ProjectBuilder.builder().build()
        val extension = project.createDetektConfigExtension()
        extension.testPaths.set(emptyList())
        extension.forbiddenMethodCalls.set(emptyList())

        val config = project.buildDetektConfig().get().parse()

        assertEquals(emptyList<String>(), config.value("style", "MagicNumber", "excludes"))
        assertEquals(
            emptyList<Map<String, String>>(),
            config.value("style", "ForbiddenMethodCall", "methods"),
        )
    }

    @Test
    fun `appended project config overrides generated and bundled values without replacing siblings`() {
        val project = ProjectBuilder.builder().build()
        val extension = project.createDetektConfigExtension()
        val projectConfig = project.file("project-detekt.yml").apply {
            writeText(
                """
                complexity:
                  TooManyFunctions:
                    allowedFunctionsPerFile: 10
                style:
                  MagicNumber:
                    active: false
                """.trimIndent(),
            )
        }
        extension.config.from(projectConfig)

        val config = project.buildDetektConfig().get().parse()

        assertEquals(10, config.value("complexity", "TooManyFunctions", "allowedFunctionsPerFile"))
        assertEquals(false, config.value("style", "MagicNumber", "active"))
        assertEquals(
            DetektConfigExtension.DEFAULT_TEST_PATHS,
            config.value("style", "MagicNumber", "excludes"),
        )
        assertEquals(
            listOf("-1", "0", "1", "2", "100"),
            config.value("style", "MagicNumber", "ignoreNumbers"),
        )
    }

    @Test
    fun `project config rejects duplicate keys`() {
        val project = ProjectBuilder.builder().build()
        val extension = project.createDetektConfigExtension()
        val projectConfig = project.file("project-detekt.yml").apply {
            writeText(
                """
                style:
                  MagicNumber:
                    active: false
                    active: true
                """.trimIndent(),
            )
        }
        extension.config.from(projectConfig)

        assertFails {
            project.buildDetektConfig().get()
        }
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

    private fun String.parseConfig(): Map<String, Any> = parse()
    private fun Map<*, *>.rule(ruleSet: String, rule: String): Map<*, *> =
        (get(ruleSet) as Map<*, *>)[rule] as Map<*, *>

}

private val placeholderRegex = Regex("<[A-Z][A-Z_]*>")

private fun String.parse(): Map<String, Any> {
    val parsed = Yaml(SafeConstructor(LoaderOptions().apply { isAllowDuplicateKeys = false })).load<Any>(this)

    require(parsed is Map<*, *>)
    @Suppress("UNCHECKED_CAST")
    return parsed as Map<String, Any>
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> Map<String, Any>.value(ruleSet: String, rule: String, property: String): T {
    val ruleSetConfig = getValue(ruleSet) as Map<String, Any>
    val ruleConfig = ruleSetConfig.getValue(rule) as Map<String, Any>
    return ruleConfig.getValue(property) as T
}

private fun Project.buildDetektConfig() = providers.provider {
    val extension = extensions.getByType<DetektConfigExtension>()
    buildDetektConfig(extension.testPaths.get(), extension.forbiddenMethodCalls.get(), hasCompose, extension.config)
}
