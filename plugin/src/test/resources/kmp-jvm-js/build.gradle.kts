import io.github.dzirbel.DetektConfigExtension

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
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            }
        }
        val commonTest = getByName("commonTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
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

detektConfig {
    forbiddenMethodCalls.addAll(DetektConfigExtension.DEFAULT_FORBIDDEN_METHOD_CALLS)
    forbiddenMethodCalls.add(
        DetektConfigExtension.ForbiddenMethodCall(
            value = "kotlinx.coroutines.CoroutineScope",
            reason = "Use an application-owned coroutine scope instead.",
        ),
    )
}
