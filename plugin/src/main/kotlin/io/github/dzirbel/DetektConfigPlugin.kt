package io.github.dzirbel

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import dev.detekt.gradle.extensions.FailOnSeverity
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class DetektConfigPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.createDetektConfigExtension()

        target.pluginManager.apply("dev.detekt")

        target.configure<DetektExtension> {
            config.setFrom(target.buildDetektConfig().map { target.resources.text.fromString(it) })
            failOnSeverity.set(FailOnSeverity.Warning)
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
    val detektTasks = mutableListOf<TaskProvider<Detekt>>()
    val detektRoot = tasks.named("detekt")

    // TODO add dedicated handling and TestKit coverage for `org.jetbrains.kotlin.android` projects.
    // Today this plugin focuses on JVM/JS/KMP compilations and may miss Android-only variants.
    fun registerDetektTask(taskName: String, compilation: KotlinCompilation<*>, isMultiplatform: Boolean) {
        val detektTask = detektTaskProvider(taskName)
        detektTasks += detektTask
        detektRoot.configure { dependsOn(detektTask) }

        val compileTaskProvider = compilation.compileTaskProvider

        detektTask.configure {
            group = "verification"
            description = buildString {
                append("Run detekt analysis for ${compilation.name}")
                if (isMultiplatform) {
                    append(" on target ${compilation.target.name}")
                }
            }
            dependsOn(compileTaskProvider)
            // TODO avoid realizing compile tasks at configuration time once classpath/source wiring can be fully lazy.
            val compileTask = compileTaskProvider.get()
            val kotlinCompileTask = compileTask as? AbstractKotlinCompileTool<*>
            if (kotlinCompileTask != null) {
                setSource(kotlinCompileTask.sources)
            }
            if (compileTask is KotlinCompile) {
                classpath.setFrom(compilation.output.classesDirs, compileTask.libraries)
            } else {
                classpath.setFrom(detektClasspath)
            }
            if (isMultiplatform) {
                multiPlatformEnabled.set(true)
            }
        }
    }

    detektRoot.configure {
        onlyIf { detektTasks.isNotEmpty() }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        val kotlin = extensions.getByType<KotlinJvmProjectExtension>()
        kotlin.target.compilations.configureEach {
            registerDetektTask(detektTaskName(name), this, isMultiplatform = false)
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.js") {
        val kotlin = extensions.getByType<KotlinJsProjectExtension>()
        kotlin.registerTargetObserver { target ->
            target?.compilations?.configureEach {
                registerDetektTask(detektTaskName(name), this, isMultiplatform = false)
            }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
        kotlin.targets.configureEach {
            if (platformType == KotlinPlatformType.common) return@configureEach
            val targetName = name
            compilations.configureEach {
                registerDetektTask(detektTaskName(name, targetName), this, isMultiplatform = true)
            }
        }
    }
}

private fun Project.detektTaskProvider(taskName: String): TaskProvider<Detekt> {
    return if (taskName in tasks.names) {
        tasks.named(taskName, Detekt::class.java)
    } else {
        tasks.register(taskName, Detekt::class.java)
    }
}

private fun detektTaskName(compilationName: String, targetName: String? = null): String {
    val targetPrefix = targetName?.takeIf { it.isNotBlank() }
        ?.replaceFirstChar { it.uppercaseChar() }
        ?: ""
    val compilationSuffix = compilationName.replaceFirstChar { it.uppercaseChar() }
    return "detekt$targetPrefix$compilationSuffix"
}
