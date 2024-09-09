import java.util.Properties

plugins {
    `kotlin-dsl`
}

group = "io.github.dzirbel"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    val versions = file("src/main/resources/versions.properties").inputStream().use {
        Properties().apply { load(it) }
    }

    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${versions["detekt"]}")
}

kotlin {
    jvmToolchain(jdkVersion = 16)
}

gradlePlugin {
    vcsUrl = "https://github.com/dzirbel/detekt-config"

    plugins {
        register(name) {
            id = "$group.detekt-config"
            implementationClass = "$group.DetektConfigPlugin"
        }
    }
}
