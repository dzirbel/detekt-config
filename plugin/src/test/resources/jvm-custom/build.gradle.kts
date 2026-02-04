plugins {
    kotlin("jvm") version libs.versions.kotlin
    id("io.github.dzirbel.detekt-config")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    target.compilations.create("integrationTest") {
        associateWith(target.compilations.getByName("test"))
    }
}
