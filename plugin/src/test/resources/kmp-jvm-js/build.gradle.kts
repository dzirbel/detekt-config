plugins {
    kotlin("multiplatform") version libs.versions.kotlin
    id("io.github.dzirbel.detekt-config")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting
        val sharedMain by creating {
            dependsOn(commonMain)
        }
        val jvmMain by getting {
            dependsOn(sharedMain)
        }
        val jsMain by getting {
            dependsOn(sharedMain)
        }
    }
}
