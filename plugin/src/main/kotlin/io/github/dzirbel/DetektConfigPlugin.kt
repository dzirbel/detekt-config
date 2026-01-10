package io.github.dzirbel

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
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
        .matching {
            it.name != "detekt" &&
                it.name.startsWith("detekt") &&
                it.name.endsWith("Main") &&
                !it.name.contains("Metadata")
        }

    tasks.named("detekt").configure {
        dependsOn(detektMainTasks)
        onlyIf { detektMainTasks.isNotEmpty() }
    }

    afterEvaluate {
        detektMainTasks.configureEach {
            val compilation = detektTaskCompilation(name)
            if (compilation != null) {
                dependsOn(compilation.compileKotlinTaskName)
                detektJavaCompileTaskName(name)?.let { javaTaskName ->
                    tasks.findByName(javaTaskName)?.let { dependsOn(it) }
                }
                classpath.setFrom(
                    providers.provider {
                        val rawFiles = buildList {
                            addAll(compilation.output.classesDirs.files)
                            if (compilation.platformType != KotlinPlatformType.native) {
                                addAll(compilation.compileDependencyFiles.files)
                            }
                        }
                        val filteredFiles = rawFiles.filter(::isNotKotlinStdlib2)
                        val needsDetektClasspath = compilation.platformType == KotlinPlatformType.native ||
                            filteredFiles.isEmpty() ||
                            rawFiles.any(::isKotlinStdlib2)
                        if (needsDetektClasspath) {
                            filteredFiles + detektClasspath.files
                        } else {
                            filteredFiles
                        }
                    }
                )
            } else {
                val taskClasspaths = detektCompileClasspaths(name)
                if (taskClasspaths.isNotEmpty()) {
                    classpath.setFrom(taskClasspaths.map { it.filter(::isNotKotlinStdlib2) })
                    classpath.from(
                        providers.provider {
                            val hasKotlinStdlib2 = taskClasspaths.any { collection ->
                                collection.files.any(::isKotlinStdlib2)
                            }
                            if (hasKotlinStdlib2) detektClasspath.files else emptyList()
                        }
                    )
                } else if (classpath.isEmpty) {
                    classpath.from(detektClasspath)
                }
            }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
        afterEvaluate {
            detektMainTasks.configureEach {
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

private fun Project.detektTaskCompilation(taskName: String): KotlinCompilation<*>? {
    val sourceSetName = detektTaskSourceSetName(taskName) ?: return null
    extensions.findByType(KotlinMultiplatformExtension::class.java)?.let { kotlin ->
        return kotlin.targets
            .asSequence()
            .flatMap { it.compilations.asSequence() }
            .firstOrNull { it.defaultSourceSet.name == sourceSetName }
    }
    extensions.findByType(KotlinJvmProjectExtension::class.java)?.let { kotlin ->
        return kotlin.target.compilations.firstOrNull { it.defaultSourceSet.name == sourceSetName }
    }
    return null
}

private fun Project.detektCompileClasspaths(taskName: String): List<FileCollection> = taskName
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

private fun detektJavaCompileTaskName(taskName: String): String? {
    val sourceSetName = detektTaskSourceSetName(taskName) ?: return null
    return if (sourceSetName == "main") {
        "compileJava"
    } else {
        "compile${sourceSetName.replaceFirstChar { it.uppercase() }}Java"
    }
}

private fun isKotlinStdlib2(file: java.io.File): Boolean =
    file.name.startsWith("kotlin-stdlib") && file.name.contains("-2.")

private fun isNotKotlinStdlib2(file: java.io.File): Boolean = !isKotlinStdlib2(file)

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
