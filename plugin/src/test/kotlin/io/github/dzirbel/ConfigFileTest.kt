package io.github.dzirbel

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ConfigFileTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `generic resource names cannot shadow bundled policy`() {
        val generic = javaClass.classLoader.getResource("base.yml")!!.readText()
        assertContains(generic, "unrelated: true")
        assertFalse(readResource("base.yml").contains("unrelated: true"))
        assertEquals(true, readResource("base.yml").parse().value("style", "MagicNumber", "active"))
    }

    @Test
    fun `later files replace lists and preserve literal placeholder text`() {
        val first = temporaryFolder.newFile("first.yml").apply {
            writeText(
                """
                style:
                  MagicNumber:
                    excludes: ['<TEST_PATHS>']
                    active: false
                """.trimIndent(),
            )
        }
        val second = temporaryFolder.newFile("second.yml").apply {
            writeText(
                """
                style:
                  MagicNumber:
                    excludes: []
                """.trimIndent(),
            )
        }

        val config = assemble(files = listOf(first, second)).parse()

        assertEquals(emptyList<String>(), config.value("style", "MagicNumber", "excludes"))
        assertEquals(false, config.value("style", "MagicNumber", "active"))

        val reversed = assemble(files = listOf(second, first)).parse()

        assertEquals(listOf("<TEST_PATHS>"), reversed.value("style", "MagicNumber", "excludes"))
    }

    @Test
    fun `assembled config resolves extension defaults`() {
        val assembled = assemble()
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
    fun `bundled config changes only at explicit extension placeholders`() {
        val expected = readResource("base.yml")
            .replace("'<TEST_PATHS>'", "['**/customTests/**']")
            .replace("'<FORBIDDEN_METHOD_CALLS>'", "[{value: 'example.forbidden'}]")
            .parse()

        val actual = assemble(
            testPaths = listOf("**/customTests/**"),
            forbiddenMethodCalls = listOf(DetektConfigExtension.ForbiddenMethodCall("example.forbidden")),
        ).parse()

        assertEquals(expected, actual)
    }

    @Test
    fun `assembled config preserves quoted and escaped extension values`() {
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

        val config = assemble(testPaths = testPaths, forbiddenMethodCalls = forbiddenMethodCalls).parse()

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
        val config = assemble(testPaths = emptyList(), forbiddenMethodCalls = emptyList()).parse()

        assertEquals(emptyList<String>(), config.value("style", "MagicNumber", "excludes"))
        assertEquals(
            emptyList<Map<String, String>>(),
            config.value("style", "ForbiddenMethodCall", "methods"),
        )
    }

    @Test
    fun `appended project config overrides generated and bundled values without replacing siblings`() {
        val projectConfig = temporaryFolder.newFile("project-detekt.yml").apply {
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

        val config = assemble(files = listOf(projectConfig)).parse()

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
        val projectConfig = temporaryFolder.newFile("project-detekt.yml").apply {
            writeText(
                """
                style:
                  MagicNumber:
                    active: false
                    active: true
                """.trimIndent(),
            )
        }

        val failure = assertFailsWith<IllegalArgumentException> {
            assemble(files = listOf(projectConfig))
        }

        assertContains(failure.message.orEmpty(), projectConfig.path)
        assertContains(assertNotNull(failure.cause).message.orEmpty(), "duplicate key active")
    }

    @Test
    fun `extension strings survive YAML parsing unchanged`() {
        val paths = listOf("**/team's-tests/**", "**/line\nbreak/**", "**/      <FORBIDDEN_METHOD_CALLS>/**")
        val reasons = listOf(
            "first line\nsecond line",
            "CRLF\r\ncarriage return\rtab\tcontrol\u0001",
            "Unicode breaks\u0085\u2028\u2029, quotes '\" and backslash \\",
            "literal '<TEST_PATHS>' and '<FORBIDDEN_METHOD_CALLS>'",
        )
        val methods = reasons.mapIndexed { index, reason ->
            DetektConfigExtension.ForbiddenMethodCall("example.method$index", reason)
        } + DetektConfigExtension.ForbiddenMethodCall("example.noReason")

        val config = assemble(testPaths = paths, forbiddenMethodCalls = methods).parse()

        assertEquals(paths, config.value("style", "MagicNumber", "excludes"))
        assertEquals(
            reasons.mapIndexed { index, reason -> mapOf("value" to "example.method$index", "reason" to reason) } +
                mapOf("value" to "example.noReason"),
            config.value("style", "ForbiddenMethodCall", "methods"),
        )
    }

    @Test
    fun `base and Compose config form one YAML document without duplicate keys`() {
        val base = assemble().parse()
        val config = assemble(compose = true).parse()

        assertFalse("Compose" in base)
        assertEquals(base + readResource("compose.yml").parse(), config)
        assertEquals(true, config.value("dzirbel", "InjectConstructorParameterOrder", "active"))
        assertEquals(true, config.value("Compose", "ModifierMissing", "active"))
    }

    @Test
    fun `project overrides win over Compose while retaining other Compose rules`() {
        val override = temporaryFolder.newFile("compose-override.yml").apply {
            writeText(
                """
                Compose:
                  ModifierMissing:
                    active: false
                """.trimIndent(),
            )
        }

        val config = assemble(compose = true, files = listOf(override)).parse()

        assertEquals(false, config.value("Compose", "ModifierMissing", "active"))
        assertEquals(true, config.value("Compose", "ModifierNaming", "active"))
        assertEquals(true, config.value("style", "MagicNumber", "active"))
    }

    @Test
    fun `empty project files leave the bundled policy intact`() {
        val empty = temporaryFolder.newFile("empty.yml")
        val comments = temporaryFolder.newFile("comments.yml").apply { writeText("# no overrides\n") }
        val emptyMap = temporaryFolder.newFile("empty-map.yml").apply { writeText("{}") }

        assertEquals(assemble().parse(), assemble(files = listOf(empty, comments, emptyMap)).parse())
    }

    @Test
    fun `maps merge recursively but scalars and lists replace earlier values`() {
        val first = temporaryFolder.newFile("first.yml").apply {
            writeText(
                """
                custom:
                  retained: true
                  mapToScalar:
                    obsolete: true
                  scalarToMap: false
                  listToMap: [obsolete]
                  mapToList:
                    obsolete: true
                  nested:
                    retained: 1
                    replaced: 2
                """.trimIndent(),
            )
        }
        val second = temporaryFolder.newFile("second.yml").apply {
            writeText(
                """
                custom:
                  mapToScalar: false
                  scalarToMap:
                    added: true
                  listToMap:
                    added: true
                  mapToList: [replacement]
                  nested:
                    replaced: 3
                    added: 4
                """.trimIndent(),
            )
        }

        val config = assemble(files = listOf(first, second)).parse()

        assertEquals(
            mapOf(
                "retained" to true,
                "mapToScalar" to false,
                "scalarToMap" to mapOf("added" to true),
                "listToMap" to mapOf("added" to true),
                "mapToList" to listOf("replacement"),
                "nested" to mapOf("retained" to 1, "replaced" to 3, "added" to 4),
            ),
            config["custom"],
        )
    }

    @Test
    fun `project config rejects a sequence instead of a map`() {
        assertInvalidConfig("- style\n- complexity", "must be a YAML map")
    }

    @Test
    fun `project config rejects a scalar instead of a map`() {
        assertInvalidConfig("false", "must be a YAML map")
    }

    @Test
    fun `project config rejects non string root keys`() {
        assertInvalidConfig("123: false", "keys must be strings")
    }

    @Test
    fun `project config rejects null root values`() {
        assertInvalidConfig("style: null", "values must not be null")
    }

    @Test
    fun `project config rejects malformed YAML`() {
        assertInvalidConfig("style: [", "while parsing a flow node")
    }

    @Test
    fun `missing project config identifies the file that could not be read`() {
        val missing = File(temporaryFolder.root, "missing.yml")

        val failure = assertFailsWith<IllegalArgumentException> { assemble(files = listOf(missing)) }

        assertContains(failure.message.orEmpty(), missing.path)
        assertNotNull(failure.cause)
    }

    private fun assertInvalidConfig(contents: String, reason: String) {
        val file = temporaryFolder.newFile("invalid.yml").apply { writeText(contents) }

        val failure = assertFailsWith<IllegalArgumentException> { assemble(files = listOf(file)) }

        assertContains(failure.message.orEmpty(), file.path)
        assertContains(assertNotNull(failure.cause).message.orEmpty(), reason)
    }

    private fun assemble(
        testPaths: List<String> = DetektConfigExtension.DEFAULT_TEST_PATHS,
        forbiddenMethodCalls: List<DetektConfigExtension.ForbiddenMethodCall> =
            DetektConfigExtension.DEFAULT_FORBIDDEN_METHOD_CALLS,
        compose: Boolean = false,
        files: List<File> = emptyList(),
    ): String = buildDetektConfig(testPaths, forbiddenMethodCalls, compose, files)
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
