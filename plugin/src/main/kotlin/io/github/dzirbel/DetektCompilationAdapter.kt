package io.github.dzirbel

import dev.detekt.gradle.Detekt
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** Adapts each Kotlin compilation to exactly one type-resolved detekt task. */
internal class DetektCompilationAdapter(private val project: Project) {
    private val detektRoot = project.tasks.named("detekt", Detekt::class.java)

    fun configure() {
        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            val kotlin = project.extensions.getByType<KotlinJvmProjectExtension>()
            kotlin.target.compilations.configureEach {
                configureCompilation(this, targetName = null, isMultiplatform = false)
            }
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kotlin = project.extensions.getByType<KotlinMultiplatformExtension>()
            kotlin.targets.configureEach {
                if (platformType == KotlinPlatformType.common) return@configureEach
                val targetName = name
                compilations.configureEach {
                    configureCompilation(this, targetName, isMultiplatform = true)
                }
            }
        }
    }

    private fun configureCompilation(
        compilation: KotlinCompilation<*>,
        targetName: String?,
        isMultiplatform: Boolean,
    ) {
        val taskName = detektTaskName(compilation.name, targetName)
        val reusesUpstreamTask = taskName in project.tasks.names
        val detektTask = project.detektTaskProvider(taskName)
        val compileTaskProvider = compilation.compileTaskProvider
        val sourceDirectories = project.providers.provider {
            compilation.allKotlinSourceSets.map { it.kotlin.sourceDirectories }
        }
        val associatedOutputs = project.providers.provider {
            compilation.transitiveAssociatedCompilations().map { it.output.allOutputs }
        }
        val compilerArguments = compileTaskProvider.flatMap { it.compilerOptions.freeCompilerArgs }

        detektRoot.configure {
            dependsOn(detektTask)
            setSource(project.files())
        }

        detektTask.configure {
            group = "verification"
            description = buildString {
                append("Run detekt analysis for compilation ${compilation.name}")
                if (isMultiplatform) append(" on target ${compilation.target.name}")
                append(" with type resolution")
            }
            dependsOn(compileTaskProvider)
            setSource(sourceDirectories)
            apiVersion.convention(
                compileTaskProvider.flatMap { it.compilerOptions.apiVersion.map { version -> version.version } },
            )
            languageVersion.convention(
                compileTaskProvider.flatMap { it.compilerOptions.languageVersion.map { version -> version.version } },
            )
            freeCompilerArgs.convention(
                if (reusesUpstreamTask) compilerArguments else compilerArguments.withExplicitApi(compilation),
            )
            optIn.convention(compileTaskProvider.flatMap { it.compilerOptions.optIn })
            friendPaths.setFrom(associatedOutputs)
            if (compilation.platformType == KotlinPlatformType.jvm) {
                val kotlinCompileTask = compileTaskProvider.map { it as KotlinCompile }
                classpath.setFrom(compilation.output.classesDirs, kotlinCompileTask.map { it.libraries })
                friendPaths.setFrom(kotlinCompileTask.map { it.friendPaths })
                jvmTarget.convention(
                    kotlinCompileTask.flatMap { it.compilerOptions.jvmTarget.map { target -> target.target } },
                )
                noJdk.convention(kotlinCompileTask.flatMap { it.compilerOptions.noJdk })
            } else {
                classpath.setFrom(
                    detektClasspath,
                    project.detektJvmProjectClasspath(compilation),
                    project.detektAnalysisClasspath(taskName, compilation),
                )
            }
            if (isMultiplatform) multiPlatformEnabled.set(true)
        }
    }

    private fun Provider<List<String>>.withExplicitApi(compilation: KotlinCompilation<*>): Provider<List<String>> {
        if (compilation.name != KotlinCompilation.MAIN_COMPILATION_NAME) return this
        val kotlin = project.extensions.getByType<KotlinBaseExtension>()
        val explicitApiArgument = project.providers.provider {
            when (kotlin.explicitApi) {
                ExplicitApiMode.Strict -> listOf("-Xexplicit-api=strict")
                ExplicitApiMode.Warning -> listOf("-Xexplicit-api=warning")
                else -> emptyList()
            }
        }
        return zip(explicitApiArgument) { arguments, explicitApi -> arguments + explicitApi }
    }
}

private fun Project.detektJvmProjectClasspath(compilation: KotlinCompilation<*>): FileCollection {
    val kotlin = extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return files()

    return objects.fileCollection().from(
        providers.provider {
            val associatedCompilationNames = compilation.transitiveAssociatedCompilations().map { it.name }.toSet()
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
    if (compilation.isTestCompilation()) {
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
        if (compilation.isTestCompilation()) {
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

private fun KotlinCompilation<*>.isTestCompilation(): Boolean = name.endsWith("test", ignoreCase = true)

private fun KotlinCompilation<*>.transitiveAssociatedCompilations(): Set<KotlinCompilation<*>> = buildSet {
    fun visit(compilation: KotlinCompilation<*>) {
        compilation.associatedCompilations.forEach { associatedCompilation ->
            if (add(associatedCompilation)) visit(associatedCompilation)
        }
    }
    visit(this@transitiveAssociatedCompilations)
}

private fun Project.detektTaskProvider(taskName: String): TaskProvider<Detekt> =
    if (taskName in tasks.names) tasks.named(taskName, Detekt::class.java) else tasks.register(taskName, Detekt::class.java)

internal fun detektTaskName(compilationName: String, targetName: String? = null): String {
    val compilationSuffix = compilationName.replaceFirstChar { it.uppercaseChar() }
    val targetSuffix = targetName?.takeIf { it.isNotBlank() }
        ?.replaceFirstChar { it.uppercaseChar() }
        ?: ""
    return "detekt$compilationSuffix$targetSuffix"
}
