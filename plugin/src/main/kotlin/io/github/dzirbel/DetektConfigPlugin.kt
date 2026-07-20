package io.github.dzirbel

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import dev.detekt.gradle.extensions.FailOnSeverity
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
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
        val sourceDirectories = providers.provider {
            compilation.allKotlinSourceSets.map { it.kotlin.sourceDirectories }
        }

        detektTask.configure {
            group = "verification"
            description = buildString {
                append("Run detekt analysis for ${compilation.name}")
                if (isMultiplatform) {
                    append(" on target ${compilation.target.name}")
                }
            }
            dependsOn(compileTaskProvider)
            setSource(sourceDirectories)
            apiVersion.convention(
                compileTaskProvider.flatMap { it.compilerOptions.apiVersion.map { version -> version.version } }
            )
            languageVersion.convention(
                compileTaskProvider.flatMap { it.compilerOptions.languageVersion.map { version -> version.version } }
            )
            freeCompilerArgs.convention(compileTaskProvider.flatMap { it.compilerOptions.freeCompilerArgs })
            optIn.convention(compileTaskProvider.flatMap { it.compilerOptions.optIn })
            friendPaths.setFrom(compilation.associatedCompilations.map { it.output.allOutputs })
            if (compilation.platformType == KotlinPlatformType.jvm) {
                val kotlinCompileTask = compileTaskProvider.map { it as KotlinCompile }
                classpath.setFrom(compilation.output.classesDirs, kotlinCompileTask.map { it.libraries })
                friendPaths.setFrom(kotlinCompileTask.map { it.friendPaths })
                jvmTarget.convention(
                    kotlinCompileTask.flatMap { it.compilerOptions.jvmTarget.map { target -> target.target } }
                )
                noJdk.convention(kotlinCompileTask.flatMap { it.compilerOptions.noJdk })
            } else {
                classpath.setFrom(
                    detektClasspath,
                    detektJvmProjectClasspath(compilation),
                    detektAnalysisClasspath(taskName, compilation),
                )
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

private fun Project.detektJvmProjectClasspath(compilation: KotlinCompilation<*>): FileCollection {
    val associatedCompilationNames = compilation.associatedCompilations.map { it.name }.toSet()
    if (associatedCompilationNames.isEmpty()) return files()
    val kotlin = extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return files()

    return objects.fileCollection().from(
        providers.provider {
            kotlin.targets
                .filter { target -> target.platformType == KotlinPlatformType.jvm }
                .flatMap { target ->
                    target.compilations
                        .filter { candidate -> candidate.name in associatedCompilationNames }
                        .map { candidate -> candidate.output.allOutputs }
                }
        },
    )
}

private fun Project.detektAnalysisClasspath(
    taskName: String,
    compilation: KotlinCompilation<*>,
): FileCollection {
    val analysisDependenciesName = "${taskName}AnalysisDependencies"
    if (compilation.name == KotlinCompilation.TEST_COMPILATION_NAME) {
        configurations.dependencyScope(analysisDependenciesName) {
            defaultDependencies {
                add(
                    this@detektAnalysisClasspath.dependencies.create(
                        "org.jetbrains.kotlin:kotlin-test-junit:" +
                            extensions.getByType<KotlinBaseExtension>().coreLibrariesVersion,
                    ),
                )
            }
        }
    }
    val configuration = configurations.resolvable("${taskName}AnalysisClasspath") {
        extendsFrom(configurations.getByName(compilation.compileDependencyConfigurationName))
        if (compilation.name == KotlinCompilation.TEST_COMPILATION_NAME) {
            extendsFrom(configurations.getByName(analysisDependenciesName))
        }
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
        }
    }
    return objects.fileCollection().from(
        configuration.map { analysisConfiguration ->
            analysisConfiguration.incoming.artifactView {
                isLenient = true
                componentFilter { identifier ->
                    identifier !is ModuleComponentIdentifier ||
                        identifier.group != "org.jetbrains.kotlin" ||
                        !identifier.module.startsWith("kotlin-stdlib")
                }
            }.files
        },
    )
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
