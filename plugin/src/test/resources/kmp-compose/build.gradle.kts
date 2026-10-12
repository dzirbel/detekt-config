plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("io.github.dzirbel.detekt-config")
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvm()
    js { browser() }
    wasmJs { browser() }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }

    if (providers.gradleProperty("corrected").isPresent) {
        sourceSets.commonMain { kotlin.setSrcDirs(listOf("src/correctedMain/kotlin")) }
        sourceSets.commonTest { kotlin.setSrcDirs(listOf("src/correctedTest/kotlin")) }
    }
}
