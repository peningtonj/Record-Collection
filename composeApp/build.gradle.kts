import org.gradle.kotlin.dsl.implementation
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

// GitLive Firebase 2.3.0 was built against Kotlin 2.2.0 and drags kotlin-stdlib up to
// 2.2.0; this project's compiler is 2.1.21, and a 2.2.0 stdlib klib is unreadable to it
// ("Symbol for Any not found"). Pin the stdlib to the compiler version.
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
        force("org.jetbrains.kotlin:kotlin-stdlib-js:${libs.versions.kotlin.get()}")
        force("org.jetbrains.kotlin:kotlin-dom-api-compat:${libs.versions.kotlin.get()}")
    }
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "recordcollection.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting
        val jsMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.browser)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.coil.network.okhttp)
            implementation(libs.readability4j)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.napier)
            implementation(libs.coil.compose)
            implementation(libs.multiplatform.settings)
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.gitlive.firebase.auth)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.mockk.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine) // For testing Flows
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.skiko.awt.runtime.macos.arm64)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.client.java)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.coil.network.okhttp)
            implementation(libs.readability4j)
            implementation(libs.apache.httpclient)
            implementation(libs.multiplatform.settings.jvm)
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.coil.network.ktor3)
        }
    }
}

android {
    namespace = "io.github.peningtonj.recordcollection"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    lint {
        disable += "NullSafeMutableLiveData"
    }


    defaultConfig {
        applicationId = "io.github.peningtonj.recordcollection"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.firebase.crashlytics.buildtools)
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "io.github.peningtonj.recordcollection.MainKt"

            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "Record Collection"
                packageVersion = "1.0.0"
                macOS {
                bundleID = "io.github.peningtonj.recordcollection"
            }
            windows {
                packageVersion = "1.0.0"
                msiPackageVersion = "1.0.0"
                exePackageVersion = "1.0.0"
                includeAllModules = true
            }

        }
    }
}
