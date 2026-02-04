plugins {
    kotlin("js") version libs.versions.kotlin
    id("io.github.dzirbel.detekt-config")
}

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        nodejs()
    }

    sourceSets {
        val test by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
