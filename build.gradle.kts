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

// Fix for "Build cancelled" during npm install and development run on Windows
// This prevents npm from running arbitrary scripts that might hang or fail.
rootProject.tasks.withType(org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask::class.java).configureEach {
    args.add("--ignore-scripts")
    args.add("--no-audit")
    args.add("--no-fund")
}
