package io.github.peningtonj.recordcollection.di.module

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore

actual class PlatformFirebaseModule {
    actual fun initialize() {
        // On Android, Firebase is initialized automatically via google-services.json
        // No manual initialization needed
    }
    
    actual fun provideAuth(): FirebaseAuth = Firebase.auth
    
    actual fun provideFirestore(): FirebaseFirestore = Firebase.firestore
}
