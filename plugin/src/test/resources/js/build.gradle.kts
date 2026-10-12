import io.github.dzirbel.DetektConfigExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("io.github.dzirbel.detekt-config")
}

repositories {
    mavenCentral()
}

kotlin {
    js {
        nodejs()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.core)
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
