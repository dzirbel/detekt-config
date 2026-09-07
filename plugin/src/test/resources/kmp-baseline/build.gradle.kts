import dev.detekt.gradle.DetektCreateBaselineTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("io.github.dzirbel.detekt-config")
}

repositories { mavenCentral() }

kotlin {
    jvm()
    js { nodejs() }
    if (providers.gradleProperty("includeNewFindings").isPresent) {
        sourceSets.commonMain { kotlin.srcDir("src/newFindings/kotlin") }
    }
}

detekt {
    baseline.set(layout.buildDirectory.file("baseline.xml"))
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    baseline.set(layout.buildDirectory.file("baseline.xml"))
}

// Replace only the generated baseline; analysis sources and configuration stay unchanged.
tasks.register<Copy>("clearBaseline") {
    from("empty-baseline.xml")
    into(layout.buildDirectory)
    rename { "baseline.xml" }
}
