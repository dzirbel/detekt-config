package io.github.dzirbel

import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class TasksTest {

    private val project = ProjectBuilder.builder().build()

    @Test
    fun `no other plugins`() {
        project.apply(plugin = "io.github.dzirbel.detekt-config")
        assertEquals(
            listOf(":detekt", ":detektBaseline", ":detektGenerateConfig"),
            project.tasks.map { it.path },
        )
    }

    @Test
    fun `after kotlin jvm plugin`() {
        project.apply(plugin = "org.jetbrains.kotlin.jvm")
        assertEquals(setOf(":test"), project.tasks.check.dependencyPaths())

        project.apply(plugin = "io.github.dzirbel.detekt-config")
        assertEquals(setOf(":detekt", ":test"), project.tasks.check.dependencyPaths())
        assertEquals(setOf(":detektMain", ":detektTest"), project.tasks.detekt.dependencyPaths())
    }

    @Test
    fun `before kotlin jvm plugin`() {
        project.apply(plugin = "io.github.dzirbel.detekt-config")
        project.apply(plugin = "org.jetbrains.kotlin.jvm")
        assertEquals(setOf(":detekt", ":test"), project.tasks.check.dependencyPaths())
        assertEquals(setOf(":detektMain", ":detektTest"), project.tasks.detekt.dependencyPaths())
    }

    // TODO KMP/JS tests

    private val TaskContainer.check: Task get() = getByPath(":check")
    private val TaskContainer.detekt: Task get() = getByPath(":detekt")

    private fun Task.dependencyPaths(): Set<String> =
        taskDependencies.getDependencies(this).mapTo(mutableSetOf()) { it.path }
}
