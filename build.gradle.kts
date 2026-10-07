plugins {
    id("com.gradleup.shadow") version "8.3.5"
    id("qupath-conventions")
}

qupathExtension {
    name = "qupath-extension-cps"
    group = "ca.ualberta.cps"
    version = "0.1.0"
    description = "Timed PD-L1 CPS scoring tool for QuPath"
    automaticModule = "ca.ualberta.cps"
}

dependencies {
    // Provided by QuPath at runtime, so not bundled into the jar
    shadow(libs.bundles.qupath)
    shadow(libs.bundles.logging)
    shadow(libs.qupath.fxtras)
}
