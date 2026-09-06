package io.github.dzirbel

import dev.detekt.gradle.DetektGenerateConfigTask
import dev.detekt.gradle.extensions.DetektExtension
import dev.detekt.gradle.extensions.FailOnSeverity
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class DetektConfigPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.createDetektConfigExtension()

        target.pluginManager.apply("dev.detekt")

        target.configure<DetektExtension> {
            config.setFrom(target.buildDetektConfig().map { target.resources.text.fromString(it) })
            failOnSeverity.set(FailOnSeverity.Warning)
        }

        target.tasks.named("detektGenerateConfig", DetektGenerateConfigTask::class.java).configure {
            // Upstream uses the last analysis config, which here is an already-existing temporary text resource.
            configFile.convention(target.rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
        }

        target.dependencies {
            val versions = readResourceProperties("versions.properties")

            add("detektPlugins", "io.github.dzirbel:rules:${versions["rules"]}")
            add("detektPlugins", "dev.detekt:detekt-rules-ktlint-wrapper:${versions["detekt"]}")

            target.withCompose {
                add("detektPlugins", "io.nlopez.compose.rules:detekt:${versions["detekt-compose"]}")
            }
        }

        DetektCompilationAdapter(target).configure()
    }
}
