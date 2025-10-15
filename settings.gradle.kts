pluginManagement {
    repositories {
        google() // Repositorio de Google
        mavenCentral() // Repositorio Maven Central
        gradlePluginPortal() // Repositorio del portal de Gradle
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FranJoFit"
include(":app")
