plugins {
    kotlin("multiplatform") version libs.versions.kotlin
    kotlin("plugin.compose") version libs.versions.kotlin
    id("io.github.dzirbel.detekt-config")
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
            }
        }
    }
}
