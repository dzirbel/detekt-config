includeBuild("../../../../..") {
    dependencySubstitution {
        substitute(module("io.github.dzirbel:detekt-config-rules")).using(project(":rules"))
    }
}
