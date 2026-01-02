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
        assertEquals(listOf(":test"), project.tasks.check.dependencies().map { it.path })

        project.apply(plugin = "io.github.dzirbel.detekt-config")
        assertEquals(
            setOf(":detekt", ":test", ":detektMain"),
            project.tasks.check.dependencies().mapTo(mutableSetOf()) { it.path },
        )
    }

    @Test
    fun `before kotlin jvm plugin`() {
        project.apply(plugin = "io.github.dzirbel.detekt-config")
        project.apply(plugin = "org.jetbrains.kotlin.jvm")
        assertEquals(
            setOf(":detekt", ":test", ":detektMain"),
            project.tasks.check.dependencies().mapTo(mutableSetOf()) { it.path },
        )
    }

    private val TaskContainer.check: Task get() = getByPath(":check")

    private fun Task.dependencies(): Set<Task> = taskDependencies.getDependencies(this)
}
