pluginManagement {
    repositories {
        google() //Repositorio de Google
        mavenCentral() //Repositorio Maven Central
        gradlePluginPortal() //Repositorio del portal de Gradle
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        //Para el uso de theras
        maven { url = uri("https://jitpack.io") }
    }
}


rootProject.name = "FranJoFit"
include(":app")
