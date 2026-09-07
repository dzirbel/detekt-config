pluginManagement {
    includeBuild("../../../..")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

includeBuild("../../../..") {
    dependencySubstitution {
        substitute(module("io.github.dzirbel:rules")).using(project(":rules"))
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../../../gradle/libs.versions.toml"))
        }
    }
}

include("analysis-classpath")
include("android-application")
include("android-compose-library")
include("config-cache")
include("js")
include("jvm")
include("jvm-clean")
include("jvm-custom")
include("jvm-dependencies")
include("kmp-baseline")
include("kmp-compose")
include("kmp-jvm-js")
include("kmp-jvm-js-clean")
include("kmp-native")
include("kmp-project-dependency")
include("kmp-project-dependency:producer")

// Each temporary fixture build has an independent cache, including across test-suite invocations.
buildCache {
    local { directory = file("build-cache") }
}
