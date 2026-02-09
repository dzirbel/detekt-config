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
include("js-standalone")
include("jvm")
include("jvm-clean")
include("jvm-custom")
include("jvm-deps")
include("kmp-native")
include("kmp-compose")
include("kmp-jvm-js")
include("kmp-jvm-js-clean")
