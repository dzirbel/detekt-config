package io.github.dzirbel

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class ConfigFileTest {

    @Test
    fun `build detekt config replaces placeholders`() {
        val project = ProjectBuilder.builder().build()
        project.createDetektConfigExtension()

        val config = project.buildDetektConfig().get()

        assertFalse(config.contains("<TEST_PATHS>"))
        assertFalse(config.contains("<FORBIDDEN_METHOD_CALLS>"))
        assertContains(config, "excludes: ['**/test/**', '**/androidTest/**', '**/testFixtures/**']")
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
    }
}
