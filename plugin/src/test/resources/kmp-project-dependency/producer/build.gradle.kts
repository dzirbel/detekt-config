plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js { nodejs() }
    val isArm64 = System.getProperty("os.arch") in listOf("aarch64", "arm64")
    when {
        System.getProperty("os.name").startsWith("Windows") -> mingwX64("native")
        System.getProperty("os.name") == "Mac OS X" -> {
            if (isArm64) macosArm64("native") else macosX64("native")
        }
        else -> {
            if (isArm64) linuxArm64("native") else linuxX64("native")
        }
    }
}
