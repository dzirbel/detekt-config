import io.github.dzirbel.DetektConfigExtension

plugins {
    embeddedKotlin("jvm")
    id("io.github.dzirbel.detekt-config")
}

detektConfig {
    // no-op assignments for demonstration
    testPaths = DetektConfigExtension.DEFAULT_TEST_PATHS
    forbiddenMethodCalls = DetektConfigExtension.DEFAULT_FORBIDDEN_METHOD_CALLS
}
