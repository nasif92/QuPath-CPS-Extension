pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://maven.scijava.org/content/repositories/releases")
        }
    }
}

// The QuPath release this extension is built against.
// Change this (and rebuild + retest) when upgrading QuPath.
qupath {
    version = "0.7.0"
}

plugins {
    id("io.github.qupath.qupath-extension-settings") version "0.2.1"
}

rootProject.name = "qupath-extension-cps"
