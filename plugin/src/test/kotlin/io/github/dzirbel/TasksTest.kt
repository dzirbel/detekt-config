package io.github.dzirbel

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektGenerateConfigTask
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

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
        assertSameContents(listOf(":detektMainJvm", ":detektTestJvm"), project.tasks.detekt.dependencyPaths())
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
        assertSameContents(listOf(":detektMainJs", ":detektTestJs"), project.tasks.detekt.dependencyPaths())
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
        assertSameContents(listOf(":detektMainMingwX64", ":detektTestMingwX64"), project.tasks.detekt.dependencyPaths())
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
                ":detektMainIosArm64",
                ":detektTestIosArm64",
                ":detektMainIosSimulatorArm64",
                ":detektTestIosSimulatorArm64",
                ":detektMainIosX64",
                ":detektTestIosX64",
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
            listOf(":detektMainLinuxArm64", ":detektTestLinuxArm64"),
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
            listOf(":detektMainLinuxX64", ":detektTestLinuxX64"),
            project.tasks.detekt.dependencyPaths(),
        )
    }

    @Test
    fun `after kotlin multiplatform jvm js plugin`() {
        val project = project()
        project.apply(plugin = "org.jetbrains.kotlin.multiplatform")
        project.extensions.configure<KotlinMultiplatformExtension> {
            jvm().compilations.create("integrationTest")
            js().compilations.create("integrationTest")
        }
        project.apply(plugin = "io.github.dzirbel.detekt-config")

        assertContains(project.tasks.check.dependencyPaths(), ":detekt")
        assertSameContents(
            listOf(
                ":detektMainJvm",
                ":detektMainJs",
                ":detektTestJvm",
                ":detektTestJs",
                ":detektIntegrationTestJvm",
                ":detektIntegrationTestJs",
            ),
            project.tasks.detekt.dependencyPaths(),
        )
    }

    @Test
    fun `plugin-owned multiplatform tasks inherit explicit API mode`() {
        val project = project()
        project.apply(plugin = "org.jetbrains.kotlin.multiplatform")
        project.extensions.configure<KotlinMultiplatformExtension> {
            explicitApi = ExplicitApiMode.Strict
            js()
        }
        project.apply(plugin = "io.github.dzirbel.detekt-config")

        val detektMainJs = project.tasks.getByPath(":detektMainJs") as Detekt
        assertContains(detektMainJs.freeCompilerArgs.get(), "-Xexplicit-api=strict")
    }

    @Test
    fun `plugin-owned compilations inherit the configured baseline lazily`() {
        val project = project()
        project.apply(plugin = "org.jetbrains.kotlin.multiplatform")
        project.extensions.configure<KotlinMultiplatformExtension> {
            js()
            linuxX64()
        }
        project.apply(plugin = "io.github.dzirbel.detekt-config")
        val tasks = listOf("detektMainJs", "detektTestJs", "detektMainLinuxX64", "detektTestLinuxX64")
            .map { project.tasks.getByName(it) as Detekt }
        val baselineFile = project.layout.projectDirectory.file("config/custom-baseline.xml")

        project.extensions.configure<DetektExtension> { baseline.set(baselineFile) }

        tasks.forEach { assertEquals(baselineFile, it.baseline.orNull, it.name) }
    }

    @Test
    fun `generate config targets a project file instead of the temporary assembled config`() {
        val project = project()
        project.apply(plugin = "io.github.dzirbel.detekt-config")

        val task = project.tasks.getByName("detektGenerateConfig") as DetektGenerateConfigTask

        assertEquals(project.file("config/detekt/detekt.yml"), task.configFile.get().asFile)
    }

    private val TaskContainer.check: Task get() = getByPath(":check")
    private val TaskContainer.detekt: Task get() = getByPath(":detekt")

    private fun Task.dependencyPaths(): Iterable<String> = taskDependencies.getDependencies(this).map { it.path }
}
