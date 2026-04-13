package io.github.peningtonj.recordcollection.db

import android.app.Application
import com.google.firebase.FirebasePlatform
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.util.LoggingUtils


actual class FirebaseDriver {
    actual fun initializeFirebase() {
        FirebasePlatform.initializeFirebasePlatform(object : FirebasePlatform() {
            val storage = mutableMapOf<String, String>()
            override fun store(key: String, value: String) = storage.set(key, value)
            override fun retrieve(key: String) = storage[key]
            override fun clear(key: String) { storage.remove(key) }
            override fun log(msg: String) {
                if (!msg.contains("SQLiteCursor")) {
                    Napier.d(msg, tag = LoggingUtils.Category.FIREBASE.tag)
                }
            }
        })

        Firebase.initialize(
            context = Application(),
            options = FirebaseOptions(
                applicationId = "1:543971105284:android:498cd233c9a57ceb517351",
                apiKey = "AIzaSyD2Y_pz7QDFXgPEsgoBb4jbGov8qyq8JSQ",
                projectId = "record-collection-66c1d",
                storageBucket = "record-collection-66c1d.firebasestorage.app",
                gcmSenderId = "543971105284"
            )
        )
        Napier.i("Firebase Desktop initialized successfully!", tag = LoggingUtils.Category.FIREBASE.tag)
    }
}