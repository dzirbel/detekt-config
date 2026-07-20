plugins {
    kotlin("multiplatform")
    id("io.github.dzirbel.detekt-config")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js {
        nodejs()
    }

    sourceSets {
        val commonMain = getByName("commonMain")
        val commonTest = getByName("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val sharedMain = create("sharedMain") {
            dependsOn(commonMain)
        }
        val sharedTest = create("sharedTest") {
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
