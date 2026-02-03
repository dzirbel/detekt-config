package io.github.dzirbel

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import dev.detekt.gradle.extensions.FailOnSeverity
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

        target.pluginManager.apply("dev.detekt")

        target.configure<DetektExtension> {
            config.setFrom(target.buildDetektConfig().map { target.resources.text.fromString(it) })
            failOnSeverity.set(FailOnSeverity.Warning)
        }

        target.tasks.withType<Detekt>().configureEach {
            config.setFrom(target.buildDetektConfig().map { target.resources.text.fromString(it) })
            pluginClasspath.from(target.configurations.named("detektPlugins"))
            disableDefaultRuleSets.set(false)
            failOnSeverity.set(FailOnSeverity.Warning)
            ignoreFailures.set(false)
        }

        target.dependencies {
            val versions = readResourceProperties("versions.properties")

            add("detektPlugins", "io.github.dzirbel:rules:${versions["rules"]}")
            add("detektPlugins", "dev.detekt:detekt-rules-ktlint-wrapper:${versions["detekt"]}")

            target.withCompose {
                add("detektPlugins", "io.nlopez.compose.rules:detekt:${versions["detekt-compose"]}")
            }
        }

        target.configureDetektDefaultTask()
    }
}

private fun Project.configureDetektDefaultTask() {
    val detektAnalysisTasks = tasks.withType<Detekt>()
        .matching {
            it.name != "detekt" &&
                it.name.startsWith("detekt") &&
                !it.name.contains("Metadata") &&
                !it.name.contains("Baseline") &&
                (it.name.endsWith("Main") || it.name.endsWith("Test"))
        }

    val detektSourceSetTasks = tasks.withType<Detekt>()
        .matching { it.name.endsWith("SourceSet") }

    tasks.named("detekt").configure {
        dependsOn(detektAnalysisTasks)
        onlyIf { detektAnalysisTasks.isNotEmpty() }
    }

    detektSourceSetTasks.configureEach {
        onlyIf { !isRedundantSourceSetTask(name) }
        classpath.from(detektClasspath)
    }

    afterEvaluate {
        detektAnalysisTasks.configureEach {
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
        registerLegacyDetektTasks(kotlin)
        afterEvaluate {
            tasks.withType<Detekt>()
                .matching {
                    it.name != "detekt" &&
                        it.name.startsWith("detekt") &&
                        !it.name.contains("Metadata") &&
                        !it.name.contains("Baseline")
                }
                .configureEach {
                    if (name.endsWith("SourceSet") && isRedundantSourceSetTask(name)) return@configureEach
                val sourceSetName = detektTaskSourceSetName(name) ?: return@configureEach
                val sourceSet = kotlin.sourceSets.findByName(sourceSetName) ?: return@configureEach
                val sourceSets = if (name.endsWith("SourceSet")) {
                    setOf(sourceSet)
                } else {
                    sourceSet.dependsOnIncludingSelf()
                }
                val sourceDirs = sourceSets
                    .flatMap { it.kotlin.srcDirs }
                    .filter { it.exists() }
                    .toSet()
                if (sourceDirs.isNotEmpty()) {
                    source(sourceDirs)
                }
                }
        }

        tasks.withType<Detekt>().configureEach {
            multiPlatformEnabled.set(true)
        }
    }
}

private fun Project.registerLegacyDetektTasks(kotlin: KotlinMultiplatformExtension) {
    kotlin.targets.configureEach {
        val targetName = name
        compilations.configureEach {
            val compilationName = name
            if (compilationName != "main" && compilationName != "test") return@configureEach
            val taskName = "detekt" +
                targetName.replaceFirstChar { it.uppercase() } +
                compilationName.replaceFirstChar { it.uppercase() }
            if (tasks.findByName(taskName) != null) return@configureEach
            tasks.register(taskName, Detekt::class.java) {
                description = "Run detekt analysis for $compilationName on target $targetName"
                group = "verification"
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
        ?.removeSuffix("SourceSet")
        ?.let { suffixName ->
            val normalized = suffixName.replaceFirstChar { it.lowercase() }
            when {
                normalized.startsWith("main") && normalized.length > "main".length ->
                    (normalized.removePrefix("main") + "Main").replaceFirstChar { it.lowercase() }
                normalized.startsWith("test") && normalized.length > "test".length ->
                    (normalized.removePrefix("test") + "Test").replaceFirstChar { it.lowercase() }
                else -> normalized
            }
        }

private fun detektJavaCompileTaskName(taskName: String): String? {
    val sourceSetName = detektTaskSourceSetName(taskName) ?: return null
    return if (sourceSetName == "main") {
        "compileJava"
    } else {
        "compile${sourceSetName.replaceFirstChar { it.uppercase() }}Java"
    }
}

private fun Project.isRedundantSourceSetTask(taskName: String): Boolean {
    if (!taskName.endsWith("SourceSet")) return false
    val sourceSetName = detektTaskSourceSetName(taskName) ?: return false
    val capitalized = sourceSetName.replaceFirstChar { it.uppercase() }
    val candidateTasks = buildSet {
        add("detekt$capitalized")
        when {
            sourceSetName.endsWith("Main") -> {
                val prefix = sourceSetName.removeSuffix("Main")
                if (prefix.isNotBlank()) {
                    add("detekt${"Main"}${prefix.replaceFirstChar { it.uppercase() }}")
                }
            }
            sourceSetName.endsWith("Test") -> {
                val prefix = sourceSetName.removeSuffix("Test")
                if (prefix.isNotBlank()) {
                    add("detekt${"Test"}${prefix.replaceFirstChar { it.uppercase() }}")
                }
            }
        }
    }
    if (candidateTasks.any { tasks.findByName(it) != null }) return true

    val kotlin = extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return false
    val sourceSet = kotlin.sourceSets.findByName(sourceSetName) ?: return false
    return tasks.withType<Detekt>()
        .matching {
            it.name.startsWith("detekt") &&
                it.name != "detekt" &&
                !it.name.endsWith("SourceSet") &&
                !it.name.contains("Baseline") &&
                !it.name.contains("Metadata")
        }
        .any { task ->
            val taskSourceSetName = detektTaskSourceSetName(task.name) ?: return@any false
            val compilation = kotlin.targets
                .asSequence()
                .flatMap { it.compilations.asSequence() }
                .firstOrNull { it.defaultSourceSet.name == taskSourceSetName }
                ?: return@any false
            compilation.defaultSourceSet.dependsOnIncludingSelf().contains(sourceSet)
        }
}

private fun isKotlinStdlib2(file: java.io.File): Boolean =
    file.name.startsWith("kotlin-stdlib") && file.name.contains("-2.")

private fun isNotKotlinStdlib2(file: java.io.File): Boolean = !isKotlinStdlib2(file)

private fun KotlinSourceSet.dependsOnIncludingSelf(): Set<KotlinSourceSet> {
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
