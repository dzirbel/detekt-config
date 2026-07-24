import io.github.dzirbel.DetektConfigExtension

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.dzirbel.detekt-config")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
}

detektConfig {
    forbiddenMethodCalls.addAll(DetektConfigExtension.DEFAULT_FORBIDDEN_METHOD_CALLS)
    forbiddenMethodCalls.add(
        DetektConfigExtension.ForbiddenMethodCall(
            value = "kotlinx.coroutines.runBlocking",
            reason = "runBlocking blocks threads. Use a suspend function instead.",
        ),
    )
}
