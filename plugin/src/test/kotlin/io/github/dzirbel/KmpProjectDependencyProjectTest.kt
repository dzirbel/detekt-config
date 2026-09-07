package io.github.dzirbel

import kotlin.test.Test

class KmpProjectDependencyProjectTest : SampleProjectTest("kmp-project-dependency") {

    @Test
    fun `JS and native analysis resolve expect actual APIs from a KMP project dependency`() {
        val analysisTasks = arrayOf(
            ":kmp-project-dependency:detektMainJs",
            ":kmp-project-dependency:detektMainNative",
        )

        val result = projectDir.gradle(*analysisTasks).buildAndFail()

        assertTaskPassed(result, ":kmp-project-dependency:producer:compileKotlinJs")
        assertTaskPassed(result, ":kmp-project-dependency:producer:compileKotlinNative")
        assertTaskPassed(result, ":kmp-project-dependency:producer:compileKotlinJvm")
        assertTaskPassed(result, ":kmp-project-dependency:producer:jvmJar")
        assertTaskPassed(result, ":kmp-project-dependency:compileKotlinJs")
        assertTaskPassed(result, ":kmp-project-dependency:compileKotlinNative")
        assertFailedTasks(result, *analysisTasks)

        val consumer = projectDir.resolve("src/commonMain/kotlin/sample/Consumer.kt")
        val expectedFinding = "${consumer.absolutePath}:3:31: The method `sample.platformName` has been forbidden: " +
            "Use the application-owned platform name instead. [ForbiddenMethodCall]"
        analysisTasks.forEach { task ->
            assertSameContents(listOf(expectedFinding), assertTaskFailed(result, task))
        }
    }
}
