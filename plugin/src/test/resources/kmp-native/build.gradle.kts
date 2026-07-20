import io.github.dzirbel.DetektConfigExtension

plugins {
    kotlin("multiplatform")
    id("io.github.dzirbel.detekt-config")
}

repositories {
    mavenCentral()
}

kotlin {
    val osName = System.getProperty("os.name")
    val osArch = System.getProperty("os.arch")
    val isMac = osName.contains("Mac", ignoreCase = true)
    val isWindows = osName.contains("Windows", ignoreCase = true)
    val isArm64 = osArch.equals("aarch64", ignoreCase = true) || osArch.equals("arm64", ignoreCase = true)
    if (isMac) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    } else if (isWindows) {
        mingwX64()
    } else {
        if (isArm64) {
            linuxArm64 {
                binaries.configureEach {
                    linkerOpts("-Wl,--as-needed")
                }
            }
        } else {
            linuxX64 {
                binaries.configureEach {
                    linkerOpts("-Wl,--as-needed")
                }
            }
        }
    }

    sourceSets {
        commonMain {
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
        if (isMac) {
            if (findByName("iosMain") == null) {
                // Ensure iOS source sets exist even if the default hierarchy template is disabled.
                val commonMain = getByName("commonMain")
                val iosMain = create("iosMain") {
                    dependsOn(commonMain)
                }
                getByName("iosX64Main") {
                    dependsOn(iosMain)
                }
                getByName("iosArm64Main") {
                    dependsOn(iosMain)
                }
                getByName("iosSimulatorArm64Main") {
                    dependsOn(iosMain)
                }
            }
            if (findByName("iosTest") == null) {
                val iosTest = create("iosTest") {
                    dependsOn(commonTest)
                }
                getByName("iosX64Test") {
                    dependsOn(iosTest)
                }
                getByName("iosArm64Test") {
                    dependsOn(iosTest)
                }
                getByName("iosSimulatorArm64Test") {
                    dependsOn(iosTest)
                }
            }
        } else if (isWindows) {
            getByName("mingwX64Main")
        } else {
            if (isArm64) {
                getByName("linuxArm64Main")
            } else {
                getByName("linuxX64Main")
            }
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
