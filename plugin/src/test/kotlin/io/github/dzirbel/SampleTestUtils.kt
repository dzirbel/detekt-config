package io.github.dzirbel

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask

internal fun BuildResult.findTaskOutput(task: BuildTask) = findTaskOutput(path = task.path)

internal fun BuildResult.findTaskOutput(path: String): String {
    val lines = output.split('\n')
    return buildList {
        var i = lines.indexOfFirst { line -> line.startsWith("> Task $path ") } + 1
        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank() || line.startsWith("> Task ")) break
            add(line)
            i++
        }
    }.joinToString(separator = "\n")
}
