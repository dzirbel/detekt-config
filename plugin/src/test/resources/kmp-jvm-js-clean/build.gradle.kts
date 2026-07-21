plugins {
    kotlin("multiplatform")
    id("io.github.dzirbel.detekt-config")
}

repositories {
    mavenCentral()
}

kotlin {
    val jvmTarget = jvm()
    val jsTarget = js {
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

    val sharedTestSourceSet = sourceSets.getByName("sharedTest")
    jvmTarget.compilations.create("integrationTest") {
        associateWith(jvmTarget.compilations.getByName("test"))
        defaultSourceSet.dependsOn(sharedTestSourceSet)
    }
    jsTarget.compilations.create("integrationTest") {
        associateWith(jsTarget.compilations.getByName("test"))
        defaultSourceSet.dependsOn(sharedTestSourceSet)
    }
}
