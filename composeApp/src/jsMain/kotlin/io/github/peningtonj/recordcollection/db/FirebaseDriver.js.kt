package io.github.peningtonj.recordcollection.db

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.util.LoggingUtils
import kotlinx.browser.window

/** Config baked into index.html as `window.firebaseConfig` (public by design on web). */
private external interface FirebaseWebConfig {
    val apiKey: String
    val authDomain: String?
    val projectId: String
    val appId: String
    val storageBucket: String?
    val messagingSenderId: String?
}

actual class FirebaseDriver {
    actual fun initializeFirebase() {
        val firebaseConfig = window.asDynamic().firebaseConfig.unsafeCast<FirebaseWebConfig?>()
            ?: error("window.firebaseConfig missing — set it in index.html")
        Firebase.initialize(
            context = null,
            options = FirebaseOptions(
                applicationId = firebaseConfig.appId,
                apiKey = firebaseConfig.apiKey,
                projectId = firebaseConfig.projectId,
                authDomain = firebaseConfig.authDomain,
                storageBucket = firebaseConfig.storageBucket,
                gcmSenderId = firebaseConfig.messagingSenderId,
            ),
        )
        Napier.i("Firebase Web initialized", tag = LoggingUtils.Category.FIREBASE.tag)
    }
}
