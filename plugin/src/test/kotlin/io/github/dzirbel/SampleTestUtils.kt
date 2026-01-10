package io.github.dzirbel

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val ansiRegex = Regex("\\u001B\\[[;\\d]*m")

internal fun assertTaskNotRun(result: BuildResult, path: String) {
    assertNull(result.findTaskOutcome(path))
    assertNull(result.task(path))
}

internal fun assertTaskFailed(result: BuildResult, path: String): List<String> {
    return assertTaskRun(result, path, setOf(TaskOutcome.FAILED))
}

internal fun assertTaskPassed(result: BuildResult, path: String): List<String> {
    return assertTaskRun(result, path, setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE))
}

internal fun <T : Comparable<T>> assertSameContents(expected: Iterable<T>, actual: Iterable<T>) {
    assertEquals(expected.sorted(), actual.sorted())
}

internal fun expectedWarnings(vararg files: File): Iterable<String> {
    return files.flatMap { file ->
        val path = file.absolutePath
        listOf(
            "$path:4:5: Variable x is declared as `var` with a mutable type kotlin.collections.MutableSet. " +
                "Consider using `val` or an immutable collection or value type [DoubleMutabilityForCollection]",
            "$path:5:5: The method `kotlin.io.println` has been forbidden: println does not allow you to configure " +
                "the output stream. Use a logger instead. [ForbiddenMethodCall]",
            "$path:4:5: Variable 'x' could be val. [VarCouldBeVal]",
        )
    }
}

private fun BuildResult.outputLines(): Sequence<String> =
    output.lineSequence().map { line -> line.replace(ansiRegex, "").trimEnd('\r') }

private fun BuildResult.findTaskOutput(path: String): List<String> {
    val lines = outputLines().toList()
    val matcher = taskLineRegex(path)
    val startIndex = lines.indexOfFirst { line -> matcher.containsMatchIn(line) }
    if (startIndex == -1) return emptyList()
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
    }
}

private fun BuildResult.findTaskLines(path: String): Sequence<String> =
    outputLines().filter { line -> taskLineRegex(path).containsMatchIn(line) }

private fun BuildResult.findTaskOutcome(path: String): TaskOutcome? {
    val suffixes = findTaskLines(path)
        .map { line -> line.substringAfter("> Task $path").trim() }
        .toList()

    if (suffixes.isEmpty()) return null
    if (suffixes.all { it.isEmpty() }) return TaskOutcome.SUCCESS

    val outcomes = suffixes.filter { it.isNotEmpty() }.distinct()
    assertEquals(1, outcomes.size, "multiple task outcomes for $path: $outcomes. Output:\n\n$output")

    return when (val outcome = outcomes.first()) {
        "SUCCESS" -> TaskOutcome.SUCCESS
        "FAILED" -> TaskOutcome.FAILED
        "UP-TO-DATE" -> TaskOutcome.UP_TO_DATE
        "FROM-CACHE" -> TaskOutcome.FROM_CACHE
        "NO-SOURCE" -> TaskOutcome.NO_SOURCE
        "SKIPPED" -> TaskOutcome.SKIPPED
        else -> error("unexpected task outcome $outcome. Output:\n\n$output")
    }
}

private fun taskLineRegex(path: String? = null): Regex {
    val suffix = if (path == null) "" else "\\s+${Regex.escape(path)}"
    return Regex("> Task$suffix(?:\\s|\$)")
}

private fun assertTaskRun(result: BuildResult, path: String, outcomes: Set<TaskOutcome>): List<String> {
    val taskOutcome = result.task(path)?.outcome
    val parsedOutcome = result.findTaskOutcome(path)

    assertEquals(
        taskOutcome,
        parsedOutcome,
        "$path outcomes from output ($parsedOutcome) and build ($taskOutcome) are mismatched." +
            "Output:\n\n${result.output}",
    )
    assertTrue(
        taskOutcome in outcomes,
        "expected $path outcome in $outcomes, but was $parsedOutcome. Output:\n\n${result.output}",
    )

    return result.findTaskOutput(path)
}
