package io.github.peningtonj.recordcollection.di.module.impl

import io.github.peningtonj.recordcollection.di.module.FirebaseModule
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

class ProductionFirebaseModule : FirebaseModule {
    override fun provideFirebaseFirestore() = Firebase.firestore
}