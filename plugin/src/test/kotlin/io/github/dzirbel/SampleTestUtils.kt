package io.github.dzirbel

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.GradleRunner

internal fun BuildResult.findTaskOutput(task: BuildTask) = findTaskOutput(path = task.path)

internal fun BuildResult.findTaskOutput(path: String): String {
    val lines = output.lineSequence()
        .map { line -> line.trimEnd('\r') }
        .toList()
    val startIndex = lines.indexOfFirst { line -> line.startsWith("> Task $path") }
    if (startIndex == -1) return ""
    return buildList {
        var i = startIndex + 1
        var hasOutput = false
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("> Task ")) break
            if (line.isBlank()) {
                if (hasOutput) break
                i++
                continue
            }
            add(line)
            hasOutput = true
            i++
        }
    }.joinToString(separator = "\n")
}

internal fun GradleRunner.withPlainConsole(vararg arguments: String): GradleRunner =
    withArguments(*arguments, "--console=plain")
