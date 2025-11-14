pluginManagement {
    repositories {
        google() // Repositorio de Google
        mavenCentral() // Repositorio Maven Central
        gradlePluginPortal() // Repositorio del portal de Gradle
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}


rootProject.name = "FranJoFit"
include(":app")
