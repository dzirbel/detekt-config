import dev.detekt.gradle.Detekt

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.dzirbel.detekt-config")
}

repositories { mavenCentral() }

if (providers.gradleProperty("disableMainAutoCorrect").isPresent) {
    tasks.named<Detekt>("detektMain") { autoCorrect.set(false) }
}
