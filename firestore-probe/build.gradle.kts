plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

/**
 * Tiny standalone module: verifies what GitLive's Kotlin/JS serializer actually hands
 * the Firebase JS SDK for the write shapes `:composeApp` uses. Kept separate so the test
 * runs in plain Node (`jsNodeTest`) without dragging Compose/Skiko into the bundle.
 *
 *   ./gradlew :firestore-probe:jsNodeTest
 */
kotlin {
    js(IR) {
        nodejs()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(libs.gitlive.firebase.firestore)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation("dev.gitlive:firebase-common-internal:2.3.0")
            }
        }
    }
}
