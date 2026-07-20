package io.github.dzirbel

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.ExternalResource
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val ansiRegex = Regex("\\u001B\\[[;\\d]*m")
private val detektDiagnosticRegex = Regex("^[ewi]: (.+:\\d+:\\d+) (.+)$")
private val normalizedDiagnosticRegex = Regex("^.+:\\d+:\\d+: .+$")
private val compilerErrorCountRegex = Regex(
    "^There were \\d+ compiler errors found during analysis\\. This affects accuracy of reporting\\.$",
)

abstract class SampleProjectTest(private val projectName: String) {
    private val fixtureRoot = Files.createTempDirectory("detekt-config-test-").toFile()

    @get:Rule
    val fixtureCleanup = object : ExternalResource() {
        override fun after() {
            fixtureRoot.deleteRecursively()
        }
    }

    protected val projectDir: File = run {
        val sourceRoot = File("src/test/resources")
        copyFixtureResources(sourceRoot, fixtureRoot)

        val repositoryRoot = File("..").canonicalFile.invariantSeparatorsPath
        fixtureRoot.resolve("settings.gradle.kts").let { settingsFile ->
            settingsFile.writeText(settingsFile.readText().replace("../../../..", repositoryRoot))
        }
        fixtureRoot.resolve(projectName)
    }
}

private fun copyFixtureResources(sourceRoot: File, destinationRoot: File) {
    val excludedDirectories = setOf(".gradle", ".kotlin", "build", "kotlin-js-store")
    sourceRoot.walkTopDown()
        .onEnter { directory -> directory == sourceRoot || directory.name !in excludedDirectories }
        .forEach { source ->
            val destination = destinationRoot.resolve(source.relativeTo(sourceRoot).path)
            if (source.isDirectory) {
                destination.mkdirs()
            } else {
                source.copyTo(destination)
            }
        }
}

internal fun File.gradle(task: String): GradleRunner {
    return GradleRunner.create()
        .withProjectDir(this)
        .withArguments(task, "--continue")
}

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

internal fun assertDetektTaskPassed(result: BuildResult, path: String) {
    assertSameContents(emptyList(), assertTaskPassed(result, path))
}

internal fun assertTaskNoSource(result: BuildResult, path: String): List<String> {
    return assertTaskRun(result, path, setOf(TaskOutcome.NO_SOURCE))
}

internal fun assertTestsExecuted(projectDir: File, taskName: String) {
    val resultFiles = projectDir.resolve("build/test-results/$taskName")
        .walkTopDown()
        .filter { file -> file.isFile && file.extension == "xml" }
        .toList()
    assertTrue(resultFiles.isNotEmpty(), "no XML test results found for $taskName")

    val testCount = resultFiles.sumOf { file ->
        Regex("""\btests="(\d+)"""").find(file.readText())?.groupValues?.get(1)?.toInt() ?: 0
    }
    assertTrue(testCount > 0, "no tests were executed by $taskName")
}

internal fun assertFailedTasks(result: BuildResult, vararg tasks: String) {
    assertSameContents(tasks.toList(), result.tasks.filter { it.outcome == TaskOutcome.FAILED }.map { it.path })
}

internal fun <T : Comparable<T>> assertSameContents(expected: Iterable<T>, actual: Iterable<T>) {
    assertEquals(expected.sorted(), actual.sorted())
}

internal fun expectedWarnings(vararg files: File): Iterable<String> {
    return files.flatMap { file ->
        val path = file.absolutePath
        val variable = file.locationOf("var x")
        val print = file.locationOf("println")
        listOf(
            "$path:${variable.line}:${variable.column}: Variable 'x' could be val. [VarCouldBeVal]",
            "$path:${variable.line}:${variable.column}: Variable x is declared as `var` with a mutable type " +
                "kotlin.collections.MutableSet. " +
                "Consider using `val` or an immutable collection or value type [DoubleMutabilityForCollection]",
            "$path:${print.line}:${print.column}: The method `kotlin.io.println` has been forbidden: " +
                "println does not allow you to configure " +
                "the output stream. Use a logger instead. [ForbiddenMethodCall]",
        )
    }
}

internal fun expectedTestWarnings(vararg files: File): Iterable<String> = expectedWarnings(*files)

internal fun expectedExternalDependencyWarning(file: File): String {
    val call = file.locationOf("CoroutineScope(EmptyCoroutineContext)")
    return "${file.absolutePath}:${call.line}:${call.column}: " +
        "The method `kotlinx.coroutines.CoroutineScope` has been forbidden: " +
        "Use an application-owned coroutine scope instead. [ForbiddenMethodCall]"
}

private fun File.locationOf(text: String): SourceLocation = useLines { lines ->
    lines.withIndex()
        .firstNotNullOfOrNull { (index, line) ->
            line.indexOf(text).takeIf { it >= 0 }?.let { column -> SourceLocation(index + 1, column + 1) }
        }
        ?: error("$text not found in $this")
}

private data class SourceLocation(val line: Int, val column: Int)

private fun BuildResult.outputLines(): Sequence<String> =
    output.lineSequence().map { line -> line.replace(ansiRegex, "").trimEnd('\r') }

private fun BuildResult.findTaskOutput(path: String): List<String> {
    val lines = outputLines().toList()
    val matcher = taskLineRegex(path)
    val startIndex = lines.indexOfFirst { line -> matcher.containsMatchIn(line) }
    if (startIndex == -1) return emptyList()
    return buildList {
        var i = startIndex + 1
        while (i < lines.size) {
            val line = lines[i]
            if (taskLineRegex().containsMatchIn(line)) break
            val diagnostic = detektDiagnosticRegex.matchEntire(line)
            val normalizedLine = diagnostic?.let { "${it.groupValues[1]}: ${it.groupValues[2]}" } ?: line
            if (normalizedLine.isDetektOutput()) {
                add(normalizedLine)
            }
            i++
        }
    }
}

// TODO find a more elegant solution
private fun String.isDetektOutput(): Boolean =
    normalizedDiagnosticRegex.matches(this) ||
        compilerErrorCountRegex.matches(this) ||
        startsWith("Run detekt CLI with ") ||
        startsWith("See https://mrmans0n.github.io/compose-rules/")

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
