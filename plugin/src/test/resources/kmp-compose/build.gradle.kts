plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
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
