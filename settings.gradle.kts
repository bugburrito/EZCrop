pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Lets the demo pick up a locally published ezcrop (./gradlew :ezcrop:publishToMavenLocal).
        mavenLocal {
            content {
                includeGroup("io.github.bugburrito")
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "EZCrop"
include(":app")
include(":ezcrop")
 