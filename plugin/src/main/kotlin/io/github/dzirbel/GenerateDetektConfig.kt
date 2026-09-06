package io.github.dzirbel

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Assembles plugin defaults and ordered project overrides into a tracked analysis input. */
@CacheableTask
abstract class GenerateDetektConfig : DefaultTask() {
    @get:Input
    abstract val testPaths: ListProperty<String>

    @get:Input
    abstract val forbiddenMethods: ListProperty<Map<String, String>>

    @get:Input
    abstract val compose: Property<Boolean>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val configFiles: ConfigurableFileCollection

    // File collection fingerprints ignore ordering; contents here also encode the merge order for the cache key.
    @get:Input
    val orderedConfigContents: List<String>
        get() = configFiles.map { it.readText() }

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val contents = buildDetektConfig(
            testPaths.get(),
            forbiddenMethods.get().map {
                DetektConfigExtension.ForbiddenMethodCall(it.getValue("value"), it["reason"])
            },
            compose.get(),
            configFiles,
        )
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(contents)
        }
    }
}
