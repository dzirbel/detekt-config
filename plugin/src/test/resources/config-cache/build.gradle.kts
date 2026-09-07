plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.dzirbel.detekt-config")
}

repositories { mavenCentral() }

detektConfig {
    if (providers.gradleProperty("allowPrint").isPresent) forbiddenMethodCalls.set(emptyList())
    val overrideOrder = providers.gradleProperty("overrideOrder").orNull
    if (overrideOrder == null) {
        config.from(layout.buildDirectory.file("project-config/project-detekt.yml"))
    } else {
        config.from(overrideOrder.split(",").map { "$it.yml" })
    }
}

// Stage alternate contents at the same path to exercise file-input invalidation without editing sources.
tasks.register<Copy>("stageRelaxedConfig") {
    from("relaxed.yml")
    into(layout.buildDirectory.dir("project-config"))
    rename { "project-detekt.yml" }
}
tasks.register<Copy>("stageStrictConfig") {
    from("strict-magic-number.yml")
    into(layout.buildDirectory.dir("project-config"))
    rename { "project-detekt.yml" }
}
tasks.register<Delete>("removeGeneratedConfig") {
    delete(layout.buildDirectory.file("detekt/config.yml"))
}
