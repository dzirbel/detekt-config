// This consumer resolves published artifacts, without the shared fixtures' composite build.
pluginManagement {
    val versions = java.util.Properties().apply {
        file("../../../../../plugin/src/main/resources/io/github/dzirbel/detekt-config/versions.properties")
            .inputStream().use { load(it) }
    }
    plugins {
        id("io.github.dzirbel.detekt-config") version versions.getProperty("rules")
    }
    repositories {
        exclusiveContent {
            forRepository { maven { url = uri("build/repository") } }
            filter {
                includeGroup("io.github.dzirbel")
                includeGroup("io.github.dzirbel.detekt-config")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "published-consumer"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../../../../gradle/libs.versions.toml"))
        }
    }
}
