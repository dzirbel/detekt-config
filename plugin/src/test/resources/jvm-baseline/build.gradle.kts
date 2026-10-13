plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.dzirbel.detekt-config")
}

repositories { mavenCentral() }

// Both files contain a warning, but Excluded.kt is normally excluded from the compilation.
if (!providers.gradleProperty("includeExcludedFile").isPresent) {
    kotlin.sourceSets.named("main") { kotlin.exclude("**/Excluded.kt") }
}

detekt { baseline.set(layout.buildDirectory.file("baseline.xml")) }
