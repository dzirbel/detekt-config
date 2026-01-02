package io.github.dzirbel

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class DetektConfigPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.createDetektConfigExtension()

        target.pluginManager.apply("io.gitlab.arturbosch.detekt")

        target.configure<DetektExtension> {
            config.setFrom(target.buildDetektConfig().map { target.resources.text.fromString(it) })
        }

        target.dependencies {
            val versions = readResourceProperties("versions.properties")

            // TODO only works within this project, won't work externally
            val rulesProject = target.rootProject.findProject(":rules")
            if (rulesProject != null && rulesProject != target) {
                add("detektPlugins", rulesProject)
            }

            add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:${versions["detekt"]}")

            target.withCompose {
                add("detektPlugins", "io.nlopez.compose.rules:detekt:${versions["detekt-compose"]}")
            }
        }

        target.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            target.tasks.named { it == "check" }.configureEach {
                dependsOn("detektMain")
            }
        }
    }
}
