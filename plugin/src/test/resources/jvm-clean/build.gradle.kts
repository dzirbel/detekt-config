plugins {
    kotlin("jvm")
    id("io.github.dzirbel.detekt-config")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}
