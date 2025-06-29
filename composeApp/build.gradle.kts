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
    alias(libs.plugins.sqlDelight)
}

sqldelight {
    databases {
        create("RecordCollectionDatabase") {
            packageName.set("io.github.peningtonj.recordcollection.db")
        }
    }
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.sqlDelight.driver.android)

        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation("io.ktor:ktor-client-core:3.0.3")
            implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
            implementation("io.ktor:ktor-client-okhttp:3.0.3")
            implementation(libs.sqlDelight.runtime)
            implementation(libs.sqlDelight.coroutines)
            implementation("io.github.aakira:napier:2.7.1")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("io.mockk:mockk:1.13.8")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            implementation("app.cash.turbine:turbine:1.0.0") // For testing Flows
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:0.7.89")
            implementation("io.ktor:ktor-server-core:3.0.3")
            implementation("io.ktor:ktor-client-java:3.0.3")
            implementation("org.apache.httpcomponents:httpclient:4.5.14")
            implementation(libs.sqlDelight.driver.sqlite)

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
            packageName = "io.github.peningtonj.recordcollection"
            packageVersion = "1.0.0"
        }
    }
}
