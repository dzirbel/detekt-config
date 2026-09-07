import org.gradle.api.tasks.bundling.Jar

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("io.github.dzirbel.detekt-config")
}

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven {
                url = uri("repository")
                metadataSources {
                    gradleMetadata()
                    mavenPom()
                }
            }
        }
        filter { includeGroup("test.analysis") }
    }
}

kotlin {
    js { nodejs() }
    when {
        System.getProperty("os.name").startsWith("Windows") -> mingwX64("native")
        System.getProperty("os.name") == "Mac OS X" -> {
            if (System.getProperty("os.arch") == "aarch64") macosArm64("native") else macosX64("native")
        }
        else -> linuxX64("native")
    }
}

// These dependencies exercise only detekt's JVM projection, leaving platform compilation healthy.
val analysisDependencies = configurations.dependencyScope("analysisDependencies")
dependencies {
    add(analysisDependencies.name, "test.analysis:target-only:1.0")
    providers.gradleProperty("analysisDependency").orNull?.let {
        add(analysisDependencies.name, "test.analysis:$it:1.0")
    }
}
configurations.matching { it.name.endsWith("AnalysisClasspath") }.configureEach {
    extendsFrom(analysisDependencies.get())
}

// Repair the deliberately incomplete Maven publication for the artifact-recovery test.
tasks.register<Jar>("restoreArtifact") {
    archiveFileName.set("missing-artifact-1.0.jar")
    destinationDirectory.set(layout.projectDirectory.dir("repository/test/analysis/missing-artifact/1.0"))
}
