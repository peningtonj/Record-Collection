package io.github.peningtonj.recordcollection.db

import dev.gitlive.firebase.firestore.FieldValue

/**
 * Why every Firestore write from shared code goes through a hand-built
 * `Map<String, Any?>` of plain primitives instead of `set(someSerializableModel)`:
 *
 * GitLive's **Kotlin/JS** serializer has two failure modes the desktop/Android
 * (JVM) path doesn't:
 *
 *  1. A Kotlin `Long` reaches the Firebase JS SDK as a boxed object →
 *     `setDoc(): Unsupported field value: a custom Long object`.
 *  2. `set(model)` runs the *generated* serializer, whose `List` descriptor reports
 *     `elementsCount == 1`, so an **empty** list field is written as `[null]`
 *     instead of `[]`. The doc then fails every future read
 *     (`Value was null for non-nullable type`). Confirmed in prod on a collection
 *     with no albums.
 *
 * A raw `Map` avoids both: numbers are written as `Int`/`Double` (never `Long`),
 * and list values go through GitLive's size-aware list serializer, so `[]` stays
 * `[]`. Reads are unaffected — the models keep `Long` and GitLive coerces
 * `Number -> Long` on the way back on every platform.
 *
 * [isFirestoreRawMapSafe] locks this in tests: a write map that still contains a
 * `Long` (or a nested model) is a bug.
 */
fun isFirestoreRawValueSafe(value: Any?): Boolean = when (value) {
    null, is String, is Int, is Double, is Boolean, is FieldValue -> true
    is Long, is Float -> false
    is Map<*, *> -> value.keys.all { it is String } && value.values.all(::isFirestoreRawValueSafe)
    is List<*> -> value.all(::isFirestoreRawValueSafe)
    else -> false
}

/** True if every value in [map] is a Firestore-JS-safe primitive (see [isFirestoreRawValueSafe]). */
fun isFirestoreRawMapSafe(map: Map<String, Any?>): Boolean = map.values.all(::isFirestoreRawValueSafe)
