import io.github.dzirbel.DetektConfigExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("io.github.dzirbel.detekt-config")
}

repositories { mavenCentral() }

kotlin {
    val osName = System.getProperty("os.name")
    val osArch = System.getProperty("os.arch")
    val isArm64 = osArch.equals("aarch64", ignoreCase = true) || osArch.equals("arm64", ignoreCase = true)
    val native = when {
        osName.contains("Mac", ignoreCase = true) && isArm64 -> macosArm64("native")
        osName.contains("Mac", ignoreCase = true) -> macosX64("native")
        osName.contains("Windows", ignoreCase = true) -> mingwX64("native")
        isArm64 -> linuxArm64("native")
        else -> linuxX64("native")
    }
    native.compilations.create("integrationTest") {
        associateWith(native.compilations.getByName("test"))
        defaultSourceSet {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.core)
            }
            kotlin.exclude("**/Excluded.kt")
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
