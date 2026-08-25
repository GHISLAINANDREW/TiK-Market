plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

// Configuration for Kotlin JS/Wasm Node.js version is handled via gradle.properties

// Fix for "Build cancelled" during npm install and development run on Windows
// This prevents npm from running arbitrary scripts that might hang or fail.
rootProject.tasks.withType(org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask::class.java).configureEach {
    args.add("--ignore-scripts")
    args.add("--no-audit")
    args.add("--no-fund")
}
