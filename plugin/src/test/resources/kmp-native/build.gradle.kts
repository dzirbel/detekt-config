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
            linuxArm64()
        } else {
            linuxX64()
        }
    }

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        if (isMac) {
            if (findByName("iosMain") == null) {
                // Ensure iOS source sets exist even if the default hierarchy template is disabled.
                val commonMain by getting
                val iosMain by creating {
                    dependsOn(commonMain)
                }
                val iosX64Main by getting {
                    dependsOn(iosMain)
                }
                val iosArm64Main by getting {
                    dependsOn(iosMain)
                }
                val iosSimulatorArm64Main by getting {
                    dependsOn(iosMain)
                }
            }
            if (findByName("iosTest") == null) {
                val iosTest by creating {
                    dependsOn(commonTest)
                }
                val iosX64Test by getting {
                    dependsOn(iosTest)
                }
                val iosArm64Test by getting {
                    dependsOn(iosTest)
                }
                val iosSimulatorArm64Test by getting {
                    dependsOn(iosTest)
                }
            }
        } else if (isWindows) {
            val mingwX64Main by getting
        } else {
            if (isArm64) {
                val linuxArm64Main by getting
            } else {
                val linuxX64Main by getting
            }
        }
    }
}
