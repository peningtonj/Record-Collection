package io.github.peningtonj.recordcollection.db

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.util.LoggingUtils

/**
 * Signs the app into Firebase anonymously if it isn't already.
 *
 * Firestore security rules require `request.auth != null` (see `firestore.rules`), so
 * this must succeed before any Firestore read/write. Anonymous auth does **not** give
 * per-user isolation — the anonymous UID is unrelated to the Spotify user ID — it only
 * keeps the database from being world-writable by unauthenticated clients.
 * See `docs/TECH_DEBT.md` § 2.1 for the eventual custom-token plan.
 *
 * Called once at startup from the platform `DependencyContainerFactory`.
 */
suspend fun ensureAnonymousAuth() {
    val auth = Firebase.auth
    if (auth.currentUser != null) {
        Napier.d("Firebase auth: already signed in (${auth.currentUser?.uid})", tag = LoggingUtils.Category.FIREBASE.tag)
        return
    }
    val result = auth.signInAnonymously()
    Napier.i("Firebase auth: signed in anonymously (${result.user?.uid})", tag = LoggingUtils.Category.FIREBASE.tag)
}
