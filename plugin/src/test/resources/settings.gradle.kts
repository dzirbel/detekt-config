includeBuild("../../../..") {
    dependencySubstitution {
        substitute(module("io.github.dzirbel:detekt-config-rules")).using(project(":rules"))
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
