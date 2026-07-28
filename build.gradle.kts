plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

// Configuration for Kotlin JS/Wasm Node.js version
// Using a stable LTS version to avoid compatibility issues with Node 22+
rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java).configureEach {
    rootProject.extensions.configure(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension::class.java) {
        nodeVersion = "20.15.0"
    }
}

// REMOVED: KotlinNpmInstallTask with --ignore-scripts
// This can cause "Build cancelled" if essential Node tools fail to install their binaries.
// If you encounter issues with Node 22+, the fixed version above (20.15.0) should resolve them.
