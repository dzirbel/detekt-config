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
        jvmMain {
            dependsOn(sharedMain)
        }
        jvmTest {
            dependsOn(sharedTest)
        }
        jsMain {
            dependsOn(sharedMain)
        }
        jsTest {
            dependsOn(sharedTest)
        }
    }
}
