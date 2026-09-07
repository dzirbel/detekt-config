plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.dzirbel.detekt-config")
}

repositories {
    exclusiveContent {
        forRepository { maven { url = uri("build/repository") } }
        filter { includeGroup("io.github.dzirbel") }
    }
    mavenCentral()
}

dependencies {
    implementation("javax.inject:javax.inject:1")
}

// Analyze the corrected example without rewriting the fixture's sources.
if (providers.gradleProperty("corrected").isPresent) {
    kotlin.sourceSets.named("main") { kotlin.setSrcDirs(listOf("src/corrected/kotlin")) }
}
