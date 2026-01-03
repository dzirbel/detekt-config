include("plugin")
include("rules")

// required for TestKit included builds to resolve; not entirely clear why
project(":rules").name = "detekt-config-rules"

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}
