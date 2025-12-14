package io.github.dzirbel

import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class TasksTest {
    @Test
    fun test() {
        val project = ProjectBuilder.builder().build()
        project.apply(plugin = "io.github.dzirbel.detekt-config")

        assertEquals(listOf(":detekt", ":detektBaseline", ":detektGenerateConfig"), project.tasks.map { it.path })
    }
}
