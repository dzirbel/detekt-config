plugins {
    kotlin("multiplatform") version libs.versions.kotlin
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
        if (isMac) {
            val iosMain by getting
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
