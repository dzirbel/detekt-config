package io.github.dzirbel

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask

internal fun BuildResult.findTaskOutput(task: BuildTask) = findTaskOutput(path = task.path)

internal fun BuildResult.findTaskOutput(path: String): String {
    val lines = output.lineSequence()
        .map { line -> line.stripAnsi().trimEnd('\r') }
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

private val ansiRegex = Regex("\\u001B\\[[;\\d]*m")

private fun String.stripAnsi(): String = replace(ansiRegex, "")
