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
        test {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
