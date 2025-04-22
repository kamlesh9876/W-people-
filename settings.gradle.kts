pluginManagement {
    repositories {
        google {
            content {
                // These filters are fine as long as they match the required dependencies
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()  // Always include Maven Central
        gradlePluginPortal()  // For Gradle plugins
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)  // Ensures no duplicate repositories in project-level build files
    repositories {
        google()  // Google repository for Firebase and Android dependencies
        mavenCentral()  // Maven Central for other dependencies
    }
}

rootProject.name = "W-people"
include(":app")  // Make sure the app module is included
