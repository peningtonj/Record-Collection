package io.github.peningtonj.recordcollection.db

actual class FirebaseDriver {
    actual fun initializeFirebase() {
        // Firebase auto-initializes on Android via google-services.json + the
        // Google Services Gradle plugin — no manual initialization required.
    }
}