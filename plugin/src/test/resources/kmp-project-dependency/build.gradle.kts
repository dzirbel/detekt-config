import io.github.dzirbel.DetektConfigExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("io.github.dzirbel.detekt-config")
}

repositories {
    mavenCentral()
}

kotlin {
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

    sourceSets.commonMain.dependencies {
        implementation(project(":kmp-project-dependency:producer"))
    }
}

detektConfig {
    forbiddenMethodCalls.add(
        DetektConfigExtension.ForbiddenMethodCall(
            value = "sample.platformName",
            reason = "Use the application-owned platform name instead.",
        ),
    )
}
