package io.github.peningtonj.recordcollection.db

import android.app.Application
import com.google.firebase.FirebasePlatform
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.util.LoggingUtils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Desktop Firebase bootstrap.
 *
 * Firebase project config is **not** hard-coded here — it is read at runtime from a
 * `google-services.json` file (the same file the Android build uses; see README).
 * Provide it via, in priority order:
 *   1. the `GOOGLE_SERVICES_JSON` env var (absolute path), or
 *   2. `composeApp/google-services.json` (relative to the repo root), or
 *   3. `google-services.json` in the working directory.
 *
 * The file is git-ignored — see `docs/TECH_DEBT.md` § 2.4.
 */
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

        Firebase.initialize(context = Application(), options = loadFirebaseOptions())
        Napier.i("Firebase Desktop initialized successfully!", tag = LoggingUtils.Category.FIREBASE.tag)
    }

    private fun loadFirebaseOptions(): FirebaseOptions {
        val file = locateGoogleServicesFile()
            ?: error(
                "google-services.json not found. Set the GOOGLE_SERVICES_JSON env var or place the " +
                    "file at composeApp/google-services.json (see README § Prerequisites)."
            )

        val root = Json.parseToJsonElement(file.readText()).jsonObject
        val projectInfo = root["project_info"]!!.jsonObject
        val client = root["client"]!!.jsonArray.first().jsonObject

        return FirebaseOptions(
            applicationId = client["client_info"]!!.jsonObject["mobilesdk_app_id"]!!.jsonPrimitive.content,
            apiKey = client["api_key"]!!.jsonArray.first().jsonObject["current_key"]!!.jsonPrimitive.content,
            projectId = projectInfo["project_id"]!!.jsonPrimitive.content,
            storageBucket = projectInfo["storage_bucket"]?.jsonPrimitive?.content,
            gcmSenderId = projectInfo["project_number"]!!.jsonPrimitive.content,
        )
    }

    private fun locateGoogleServicesFile(): File? =
        listOfNotNull(
            System.getenv("GOOGLE_SERVICES_JSON"),
            "composeApp/google-services.json",
            "google-services.json",
        ).map(::File).firstOrNull { it.isFile }
}
