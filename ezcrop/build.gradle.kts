plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.maven.publish)
}

mavenPublishing {
    publishToMavenCentral()
    // Maven Central requires signatures; publishToMavenLocal should work without a key.
    val hasSigningKey = providers.gradleProperty("signing.keyId").isPresent ||
        providers.gradleProperty("signingInMemoryKey").isPresent
    if (hasSigningKey) {
        signAllPublications()
    }

    coordinates("io.github.bugburrito", "ezcrop", libs.versions.ezcrop.get())

    pom {
        name.set("EZCrop")
        description.set("A lightweight, easy-to-use image cropping component for Jetpack Compose.")
        inceptionYear.set("2026")
        url.set("https://github.com/bugburrito/EZCrop")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("bugburrito")
                name.set("bugburrito")
                url.set("https://github.com/bugburrito")
            }
        }
        scm {
            url.set("https://github.com/bugburrito/EZCrop")
            connection.set("scm:git:git://github.com/bugburrito/EZCrop.git")
            developerConnection.set("scm:git:ssh://git@github.com/bugburrito/EZCrop.git")
        }
    }
}

android {
    namespace = "io.github.bugburrito.ezcrop"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.kotlinx.coroutines.core)
}
