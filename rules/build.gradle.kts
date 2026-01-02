import java.util.Properties

plugins {
    kotlin("jvm") version "1.9.24"
    `java-library`
}

group = "io.github.dzirbel"
version = "1.0.0"

repositories {
    mavenCentral()
}

val versions = Properties().apply {
    val versionsFile = rootProject.file("plugin/src/main/resources/versions.properties")
    check(versionsFile.exists()) { "Missing versions.properties at ${versionsFile.path}" }
    versionsFile.inputStream().use { load(it) }
}
val detektVersion = versions["detekt"] as String

dependencies {
    compileOnly("io.gitlab.arturbosch.detekt:detekt-api:$detektVersion")
    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:$detektVersion")
    testImplementation("io.gitlab.arturbosch.detekt:detekt-api:$detektVersion")
    testImplementation(kotlin("test"))
}
