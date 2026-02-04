includeBuild("../../../..") {
    dependencySubstitution {
        substitute(module("io.github.dzirbel:rules")).using(project(":rules"))
    }
}

pluginManagement {
    includeBuild("../../../..")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../../../gradle/libs.versions.toml"))
        }
    }
}

include("js")
include("jvm")
include("jvm-clean")
include("kmp-native")
include("kmp-jvm-js")
include("kmp-jvm-js-clean")
