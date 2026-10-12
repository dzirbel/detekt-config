package io.github.dzirbel

import dev.detekt.gradle.Detekt
import org.gradle.api.Task
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DetektCompilationAdapterTest {
    @Test
    fun `targets and compilations created after plugin application join the lifecycle`() {
        val project = ProjectBuilder.builder().build()
        project.apply(plugin = "io.github.dzirbel.detekt-config")
        project.apply(plugin = "org.jetbrains.kotlin.multiplatform")
        val kotlin = project.extensions.getByType<KotlinMultiplatformExtension>()
        val jvm = kotlin.jvm("desktop")
        val js = kotlin.js("web")
        val native = kotlin.linuxX64("native")
        jvm.compilations.create("integrationTest")
        js.compilations.create("integrationTest")
        native.compilations.create("integrationTest")

        val root = project.tasks.getByName("detekt") as Detekt

        assertSameContents(
            listOf(
                "generateDetektConfig",
                "detektMainDesktop", "detektTestDesktop", "detektIntegrationTestDesktop",
                "detektMainWeb", "detektTestWeb", "detektIntegrationTestWeb",
                "detektMainNative", "detektTestNative", "detektIntegrationTestNative",
            ),
            root.dependencyNames(),
        )
        assertTrue(root.source.isEmpty)
        assertContains(project.tasks.getByName("check").dependencyNames(), "detekt")
        assertFalse("detektMainMetadata" in project.tasks.names)

        val customAnalysis = project.tasks.getByName("detektIntegrationTestNative") as Detekt
        assertContains(customAnalysis.dependencyNames(), "compileIntegrationTestKotlinNative")
        assertTrue(customAnalysis.multiPlatformEnabled.get())
    }

    @Test
    fun `realized JS tasks see later compiler settings and explicit API mode`() {
        val project = ProjectBuilder.builder().build()
        project.apply(plugin = "org.jetbrains.kotlin.multiplatform")
        val kotlin = project.extensions.getByType<KotlinMultiplatformExtension>()
        val js = kotlin.js()
        project.apply(plugin = "io.github.dzirbel.detekt-config")
        val main = project.tasks.getByName("detektMainJs") as Detekt
        val test = project.tasks.getByName("detektTestJs") as Detekt

        js.compilations.getByName("main").compileTaskProvider.get().compilerOptions.apply {
            languageVersion.set(KotlinVersion.KOTLIN_2_1)
            apiVersion.set(KotlinVersion.KOTLIN_2_0)
            optIn.set(listOf("sample.ExperimentalApi"))
            freeCompilerArgs.set(listOf("-Xcontext-receivers"))
        }
        kotlin.explicitApi = ExplicitApiMode.Warning

        assertEquals("2.1", main.languageVersion.get())
        assertEquals("2.0", main.apiVersion.get())
        assertEquals(listOf("sample.ExperimentalApi"), main.optIn.get())
        assertEquals(listOf("-Xcontext-receivers", "-Xexplicit-api=warning"), main.freeCompilerArgs.get())
        assertFalse(test.freeCompilerArgs.get().any { it.startsWith("-Xexplicit-api") })

        kotlin.explicitApi = ExplicitApiMode.Disabled
        assertEquals(listOf("-Xcontext-receivers"), main.freeCompilerArgs.get())
    }

    @Test
    fun `realized JVM tasks see later JVM target and no JDK settings`() {
        val project = ProjectBuilder.builder().build()
        project.apply(plugin = "org.jetbrains.kotlin.jvm")
        project.apply(plugin = "io.github.dzirbel.detekt-config")
        val main = project.tasks.getByName("detektMain") as Detekt
        val kotlin = project.extensions.getByType<KotlinJvmProjectExtension>()

        val compile = kotlin.target.compilations.getByName("main").compileTaskProvider.get() as KotlinCompile
        compile.compilerOptions.apply {
            jvmTarget.set(JvmTarget.JVM_17)
            noJdk.set(true)
        }

        assertEquals("17", main.jvmTarget.get())
        assertTrue(main.noJdk.get())
    }

    @Test
    fun `explicit detekt compiler settings override inherited conventions`() {
        val project = ProjectBuilder.builder().build()
        project.apply(plugin = "org.jetbrains.kotlin.multiplatform")
        val kotlin = project.extensions.getByType<KotlinMultiplatformExtension>()
        val js = kotlin.js()
        project.apply(plugin = "io.github.dzirbel.detekt-config")
        val main = project.tasks.getByName("detektMainJs") as Detekt
        main.languageVersion.set("2.0")
        main.apiVersion.set("2.0")
        main.freeCompilerArgs.set(listOf("-Xexplicit-api=warning"))
        main.optIn.set(listOf("sample.DetektOnlyApi"))

        js.compilations.getByName("main").compileTaskProvider.get().compilerOptions.apply {
            languageVersion.set(KotlinVersion.KOTLIN_2_1)
            apiVersion.set(KotlinVersion.KOTLIN_2_1)
            freeCompilerArgs.set(listOf("-Xcontext-receivers"))
            optIn.set(listOf("sample.CompilerOnlyApi"))
        }
        kotlin.explicitApi = ExplicitApiMode.Strict

        assertEquals("2.0", main.languageVersion.get())
        assertEquals("2.0", main.apiVersion.get())
        assertEquals(listOf("-Xexplicit-api=warning"), main.freeCompilerArgs.get())
        assertEquals(listOf("sample.DetektOnlyApi"), main.optIn.get())
    }

    @Test
    fun `custom compilation friend paths include transitive associations added after task realization`() {
        val project = ProjectBuilder.builder().build()
        project.apply(plugin = "org.jetbrains.kotlin.multiplatform")
        val kotlin = project.extensions.getByType<KotlinMultiplatformExtension>()
        val js = kotlin.js()
        val main = js.compilations.getByName("main")
        val test = js.compilations.getByName("test")
        val integrationTest = js.compilations.create("integrationTest")
        project.apply(plugin = "io.github.dzirbel.detekt-config")
        val analysis = project.tasks.getByName("detektIntegrationTestJs") as Detekt

        integrationTest.associateWith(test)

        val expected = main.output.allOutputs.files + test.output.allOutputs.files
        assertTrue(expected.isNotEmpty())
        assertEquals(expected, analysis.friendPaths.files)
    }

    private fun Task.dependencyNames(): List<String> = taskDependencies.getDependencies(this).map { it.name }
}
