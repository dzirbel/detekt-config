package io.github.dzirbel

import kotlin.test.Test
import kotlin.test.assertContains

class KmpNativeCustomProjectTest : SampleProjectTest("kmp-native-custom") {
    @Test
    fun `native custom tests compile and resolve dependencies while honoring filters and test exemptions`() {
        val arguments = arrayOf("detekt", "--configuration-cache", "--configuration-cache-problems=fail")
        val source = projectDir.resolve("src/nativeIntegrationTest/kotlin/sample/SampleIntegrationTest.kt")
        val expected = listOf(
            "${source.absolutePath}:11:9: The method `kotlin.io.println` has been forbidden: " +
                "println does not allow you to configure the output stream. Use a logger instead. [ForbiddenMethodCall]",
            expectedExternalDependencyWarning(source),
        )

        val first = projectDir.gradle(*arguments).buildAndFail()

        assertTaskPassed(first, ":kmp-native-custom:compileIntegrationTestKotlinNative")
        assertFailedTasks(first, ":kmp-native-custom:detektIntegrationTestNative")
        assertSameContents(expected, assertTaskFailed(first, ":kmp-native-custom:detektIntegrationTestNative"))
        assertTaskNoSource(first, ":kmp-native-custom:detektMainNative")
        assertTaskNoSource(first, ":kmp-native-custom:detektTestNative")
        assertContains(first.output, "Configuration cache entry stored.")

        val cached = projectDir.gradle(*arguments).buildAndFail()

        assertContains(cached.output, "Reusing configuration cache.")
        assertFailedTasks(cached, ":kmp-native-custom:detektIntegrationTestNative")
        assertSameContents(expected, assertTaskFailed(cached, ":kmp-native-custom:detektIntegrationTestNative"))
    }
}
