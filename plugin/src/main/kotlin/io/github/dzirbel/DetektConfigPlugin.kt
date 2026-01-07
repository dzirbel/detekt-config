package io.github.dzirbel

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

class DetektConfigPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.createDetektConfigExtension()

        target.pluginManager.apply("io.gitlab.arturbosch.detekt")

        target.configure<DetektExtension> {
            config.setFrom(target.buildDetektConfig().map { target.resources.text.fromString(it) })
        }

        target.dependencies {
            val versions = readResourceProperties("versions.properties")

            add("detektPlugins", "io.github.dzirbel:rules:${versions["rules"]}")
            add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:${versions["detekt"]}")

            target.withCompose {
                add("detektPlugins", "io.nlopez.compose.rules:detekt:${versions["detekt-compose"]}")
            }
        }

        target.configureDetektDefaultTask()
    }
}

private fun Project.configureDetektDefaultTask() {
    val detektMainTasks = tasks.withType<Detekt>()
        .matching { it.name != "detekt" && it.name.startsWith("detekt") && it.name.endsWith("Main") }
    val detektTypeResolutionTasks = detektMainTasks
        .matching { !it.name.contains("Metadata") }

    tasks.named("detekt").configure {
        dependsOn(detektTypeResolutionTasks)
        onlyIf { detektTypeResolutionTasks.isNotEmpty() }
    }

    afterEvaluate {
        detektTypeResolutionTasks.configureEach {
            val compileClasspaths = detektCompileClasspaths(name)
            if (compileClasspaths.isNotEmpty()) {
                compileClasspaths.forEach { classpath.from(it) }
            }
            if ((compileClasspaths.isEmpty() || compileClasspaths.all { it.files.isEmpty() }) && classpath.isEmpty) {
                classpath.from(detektClasspath)
            }
            val kotlinStdlibFiles = classpath.files.filter { it.name.startsWith("kotlin-stdlib") }
            if (kotlinStdlibFiles.any { it.name.contains("-2.") }) {
                val filteredClasspath = classpath.filter { !it.name.startsWith("kotlin-stdlib") }
                classpath.setFrom(filteredClasspath)
                classpath.from(detektClasspath)
            }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
        afterEvaluate {
            detektTypeResolutionTasks.configureEach {
                val sourceSetName = detektTaskSourceSetName(name) ?: return@configureEach
                val sourceSet = kotlin.sourceSets.findByName(sourceSetName) ?: return@configureEach
                val sourceDirs = sourceSet.allDependsOnSourceSets()
                    .flatMap { it.kotlin.srcDirs }
                    .filter { it.exists() }
                    .toSet()
                if (sourceDirs.isNotEmpty()) {
                    source(sourceDirs)
                }
            }
        }
    }
}

private fun Project.detektCompileClasspaths(taskName: String): List<org.gradle.api.artifacts.Configuration> = taskName
    .let(::detektTaskSourceSetName)
    ?.let { sourceSetName ->
        if (sourceSetName == "main") {
            listOfNotNull(
                configurations.findByName("mainCompileClasspath"),
                configurations.findByName("compileClasspath"),
            )
        } else {
            listOfNotNull(configurations.findByName("${sourceSetName}CompileClasspath"))
        }
    }
    ?: emptyList()

private fun detektTaskSourceSetName(taskName: String): String? =
    taskName
        .removePrefix("detekt")
        .takeIf { it.isNotBlank() }
        ?.replaceFirstChar { it.lowercase() }

private fun KotlinSourceSet.allDependsOnSourceSets(): Set<KotlinSourceSet> {
    val visited = LinkedHashSet<KotlinSourceSet>()
    val queue = ArrayDeque<KotlinSourceSet>()
    queue.add(this)
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (!visited.add(current)) continue
        queue.addAll(current.dependsOn)
    }
    return visited
}
