import io.github.dzirbel.DetektConfigExtension

plugins {
    kotlin("jvm") version "2.0.20"
    id("io.github.dzirbel.detekt-config")
}

repositories {
    mavenCentral()
}

detektConfig {
    // no-op assignments for demonstration
    testPaths = DetektConfigExtension.DEFAULT_TEST_PATHS
    forbiddenMethodCalls = DetektConfigExtension.DEFAULT_FORBIDDEN_METHOD_CALLS
}
