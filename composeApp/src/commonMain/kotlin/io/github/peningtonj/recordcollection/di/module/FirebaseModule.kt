package io.github.peningtonj.recordcollection.di.module

import dev.gitlive.firebase.firestore.FirebaseFirestore

interface FirebaseModule {
    fun provideFirebaseFirestore(): FirebaseFirestore
}
