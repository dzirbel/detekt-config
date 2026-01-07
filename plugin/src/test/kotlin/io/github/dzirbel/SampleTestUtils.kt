package io.github.dzirbel

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val ansiRegex = Regex("\\u001B\\[[;\\d]*m")

internal fun assertTaskNotRun(result: BuildResult, path: String) {
    assertNull(result.findTaskLine(path))
    assertNull(result.task(path))
}

internal fun assertTaskFailed(result: BuildResult, path: String): String {
    return assertTaskRun(result, path, setOf(TaskOutcome.FAILED))
}

internal fun assertTaskPassed(result: BuildResult, path: String): String {
    return assertTaskRun(result, path, setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE))
}

private fun BuildResult.outputLines(): Sequence<String> =
    output.lineSequence().map { line -> line.replace(ansiRegex, "").trimEnd('\r') }

private fun BuildResult.findTaskOutput(path: String): String {
    val lines = outputLines().toList()
    val matcher = taskLineRegex(path)
    val startIndex = lines.indexOfFirst { line -> matcher.containsMatchIn(line) }
    if (startIndex == -1) return ""
    return buildList {
        var i = startIndex + 1
        var hasOutput = false
        while (i < lines.size) {
            val line = lines[i]
            if (taskLineRegex().containsMatchIn(line)) break
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

private fun BuildResult.findTaskLine(path: String): String? =
    outputLines().firstOrNull { line -> taskLineRegex(path).containsMatchIn(line) }

private fun BuildResult.findTaskOutcome(path: String): TaskOutcome? {
    val line = findTaskLine(path) ?: return null
    return when (val suffix = line.substringAfter("> Task $path").trim()) {
        "", "SUCCESS" -> TaskOutcome.SUCCESS
        "FAILED" -> TaskOutcome.FAILED
        "UP-TO-DATE" -> TaskOutcome.UP_TO_DATE
        "FROM-CACHE" -> TaskOutcome.FROM_CACHE
        "NO-SOURCE" -> TaskOutcome.NO_SOURCE
        "SKIPPED" -> TaskOutcome.SKIPPED
        else -> error("unexpected task suffix $suffix")
    }
}

private fun taskLineRegex(path: String? = null): Regex {
    val suffix = if (path == null) "" else "\\s+${Regex.escape(path)}"
    return Regex("> Task$suffix(?:\\s|\$)")
}

private fun assertTaskRun(result: BuildResult, path: String, outcomes: Set<TaskOutcome>): String {
    val outcome = result.findTaskOutcome(path)

    assertEquals(
        result.task(path)?.outcome,
        outcome,
        "outcomes from output and build are mismatched. Output:\n\n${result.output}",
    )
    assertTrue(
        outcome in outcomes,
        "expected $path outcome in $outcomes, but was $outcome. Output:\n\n${result.output}",
    )

    return result.findTaskOutput(path)
}
