import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        compilerOptions {
            moduleName.set("composeApp")
        }
        // Node.js version and download configuration are handled in gradle.properties
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer(
                    open = false
                )
            }
        }
        binaries.executable()
    }

    js {
        compilerOptions {
            moduleName.set("composeApp")
        }
        // Node.js version and download configuration are handled in gradle.properties
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer(
                    open = false
                )
            }
        }
        binaries.executable()
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.sqldelight.runtime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation("androidx.activity:activity-compose:1.9.3")
            implementation("androidx.credentials:credentials:1.3.0")
            implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
            implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
            implementation(libs.sqldelight.android.driver)
        }
        wasmJsMain.dependencies {
            implementation(libs.sqldelight.web.worker.driver)
        }
    }
}

sqldelight {
    databases {
        create("TikMarketDatabase") {
            packageName.set("com.tik_market.db")
        }
    }
}

android {
    namespace = "com.tik_market"
    compileSdk = 34

    val versionProps = Properties()
    val versionFile = project.rootProject.file("version.properties")
    if (versionFile.exists()) {
        versionFile.inputStream().use { 
            versionProps.load(it) 
        }
    }
    val verCode = (versionProps.getProperty("VERSION_CODE") ?: "1").toInt()
    val verName = versionProps.getProperty("VERSION_NAME") ?: "1.0.0"

    defaultConfig {
        applicationId = "com.tik_market.app"
        minSdk = 21
        targetSdk = 34
        versionCode = verCode
        versionName = verName

        // Supporte absolument toutes les architectures mobiles
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
        // Force l'inclusion des bibliothèques natives pour une meilleure compatibilité
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

// Tâche pour incrémenter la version avant le build
tasks.register("incrementVersion") {
    doLast {
        val vProps = Properties()
        val vFile = project.rootProject.file("version.properties")
        if (vFile.exists()) {
            vFile.inputStream().use { vProps.load(it) }
        }
        val currentCode = (vProps.getProperty("VERSION_CODE") ?: "0").toInt()
        val nextCode = currentCode + 1
        
        // On incrémente le code et on met à jour le nom (ex: 1.0.0 -> 1.0.1)
        // Pour simplifier, on garde le préfixe 1.0. et on ajoute le code
        val nextName = "1.0.$nextCode"
        
        vProps.setProperty("VERSION_CODE", nextCode.toString())
        vProps.setProperty("VERSION_NAME", nextName)
        vFile.outputStream().use { vProps.store(it, null) }
        println("Version mise à jour : $nextName (Code: $nextCode)")
    }
}
