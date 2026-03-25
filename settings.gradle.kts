pluginManagement {
    plugins {
        kotlin("jvm") version "2.3.0"
        id("com.gradleup.shadow") version "9.2.2"
    }
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "polar"

include("webhook")
