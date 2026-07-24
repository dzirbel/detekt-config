plugins {
    id("io.github.dzirbel.detekt-config")
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "io.github.dzirbel.composelibrary"
    compileSdk = 36
    enableKotlin = true

    defaultConfig {
        minSdk = 23
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
}
