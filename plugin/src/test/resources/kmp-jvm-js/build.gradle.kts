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
        nodejs()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val sharedMain by creating {
            dependsOn(commonMain)
        }
        val sharedTest by creating {
            dependsOn(commonTest)
        }
        val jvmMain by getting {
            dependsOn(sharedMain)
        }
        val jvmTest by getting {
            dependsOn(sharedTest)
        }
        val jsMain by getting {
            dependsOn(sharedMain)
        }
        val jsTest by getting {
            dependsOn(sharedTest)
        }
    }
}
