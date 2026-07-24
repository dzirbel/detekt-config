import io.github.dzirbel.DetektConfigExtension

plugins {
    alias(libs.plugins.android.application)
    id("io.github.dzirbel.detekt-config")
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "io.github.dzirbel.application"
    compileSdk = 36
    enableKotlin = true

    defaultConfig {
        applicationId = "io.github.dzirbel.application"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
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
