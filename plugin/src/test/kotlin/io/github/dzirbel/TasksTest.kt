package io.github.dzirbel

import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import kotlin.test.Test
import kotlin.test.assertContains

/**
 * Verifies task dependencies on projects created via [ProjectBuilder] (not TestKit projects built by test resource
 * configuration files). This only verifies the task structure but has the advantage for KMP projects of not needing the
 * system to match the project configuration, e.g. it can verify configuration for iOS native targets on non-macOS
 * machines.
 */
class TasksTest {

    private fun project() = ProjectBuilder.builder().build()

    @Test
    fun `no other plugins`() {
        val project = project()
        project.apply(plugin = "io.github.dzirbel.detekt-config")

        assertSameContents(
            listOf(":detekt", ":detektBaseline", ":detektGenerateConfig"),
            project.tasks.map { it.path },
        )
    }

    @Test
    fun `after kotlin jvm plugin`() {
        val project = project()
        project.apply(plugin = "org.jetbrains.kotlin.jvm")
        assertSameContents(listOf(":test"), project.tasks.check.dependencyPaths())

        project.apply(plugin = "io.github.dzirbel.detekt-config")

        assertSameContents(listOf(":detekt", ":test"), project.tasks.check.dependencyPaths())
        assertSameContents(listOf(":detektMain", ":detektTest"), project.tasks.detekt.dependencyPaths())
    }

    @Test
    fun `before kotlin jvm plugin`() {
        val project = project()
        project.apply(plugin = "io.github.dzirbel.detekt-config")
        project.apply(plugin = "org.jetbrains.kotlin.jvm")

        assertSameContents(listOf(":detekt", ":test"), project.tasks.check.dependencyPaths())
        assertSameContents(listOf(":detektMain", ":detektTest"), project.tasks.detekt.dependencyPaths())
    }

    @Test
    fun `after kotlin multiplatform jvm plugin`() {
        val project = project()
        project.apply(plugin = "org.jetbrains.kotlin.multiplatform")
        project.extensions.configure<KotlinMultiplatformExtension> {
            jvm()
        }
        project.apply(plugin = "io.github.dzirbel.detekt-config")

        assertContains(project.tasks.check.dependencyPaths(), ":detekt")
        assertSameContents(listOf(":detektJvmMain", ":detektJvmTest"), project.tasks.detekt.dependencyPaths())
    }

    @Test
    fun `after kotlin multiplatform js plugin`() {
        val project = project()
        project.apply(plugin = "org.jetbrains.kotlin.multiplatform")
        project.extensions.configure<KotlinMultiplatformExtension> {
            js()
        }
        project.apply(plugin = "io.github.dzirbel.detekt-config")

        assertContains(project.tasks.check.dependencyPaths(), ":detekt")
        assertSameContents(listOf(":detektJsMain", ":detektJsTest"), project.tasks.detekt.dependencyPaths())
    }

    @Test
    fun `after kotlin multiplatform native plugin, windows`() {
        val project = project()
        project.apply(plugin = "org.jetbrains.kotlin.multiplatform")
        project.extensions.configure<KotlinMultiplatformExtension> {
            mingwX64()
        }
        project.apply(plugin = "io.github.dzirbel.detekt-config")

        assertContains(project.tasks.check.dependencyPaths(), ":detekt")
        assertSameContents(listOf(":detektMingwX64Main", ":detektMingwX64Test"), project.tasks.detekt.dependencyPaths())
    }

    @Test
    fun `after kotlin multiplatform native plugin, ios`() {
        val project = project()
        project.apply(plugin = "org.jetbrains.kotlin.multiplatform")
        project.extensions.configure<KotlinMultiplatformExtension> {
            iosX64()
            iosArm64()
            iosSimulatorArm64()
        }
        project.apply(plugin = "io.github.dzirbel.detekt-config")

        assertContains(project.tasks.check.dependencyPaths(), ":detekt")
        assertSameContents(
            listOf(
                ":detektIosArm64Main",
                ":detektIosArm64Test",
                ":detektIosSimulatorArm64Main",
                ":detektIosSimulatorArm64Test",
                ":detektIosX64Main",
                ":detektIosX64Test",
            ),
            project.tasks.detekt.dependencyPaths(),
        )
    }

    @Test
    fun `after kotlin multiplatform native plugin, linux arm64`() {
        val project = project()
        project.apply(plugin = "org.jetbrains.kotlin.multiplatform")
        project.extensions.configure<KotlinMultiplatformExtension> {
            linuxArm64()
        }
        project.apply(plugin = "io.github.dzirbel.detekt-config")

        assertContains(project.tasks.check.dependencyPaths(), ":detekt")
        assertSameContents(
            listOf(":detektLinuxArm64Main", ":detektLinuxArm64Test"),
            project.tasks.detekt.dependencyPaths(),
        )
    }

    @Test
    fun `after kotlin multiplatform native plugin, linux x64`() {
        val project = project()
        project.apply(plugin = "org.jetbrains.kotlin.multiplatform")
        project.extensions.configure<KotlinMultiplatformExtension> {
            linuxX64()
        }
        project.apply(plugin = "io.github.dzirbel.detekt-config")

        assertContains(project.tasks.check.dependencyPaths(), ":detekt")
        assertSameContents(
            listOf(":detektLinuxX64Main", ":detektLinuxX64Test"),
            project.tasks.detekt.dependencyPaths(),
        )
    }

    @Test
    fun `after kotlin multiplatform jvm js plugin`() {
        val project = project()
        project.apply(plugin = "org.jetbrains.kotlin.multiplatform")
        project.extensions.configure<KotlinMultiplatformExtension> {
            jvm()
            js()
        }
        project.apply(plugin = "io.github.dzirbel.detekt-config")

        assertContains(project.tasks.check.dependencyPaths(), ":detekt")
        assertSameContents(
            listOf(":detektJvmMain", ":detektJsMain", ":detektJvmTest", ":detektJsTest"),
            project.tasks.detekt.dependencyPaths(),
        )
    }

    private val TaskContainer.check: Task get() = getByPath(":check")
    private val TaskContainer.detekt: Task get() = getByPath(":detekt")

    private fun Task.dependencyPaths(): Iterable<String> = taskDependencies.getDependencies(this).map { it.path }
}
