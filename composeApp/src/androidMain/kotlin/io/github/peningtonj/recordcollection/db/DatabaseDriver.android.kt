package io.github.peningtonj.recordcollection.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriver(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = RecordCollectionDatabase.Schema,
            context = context,
            name = "record_collection.db"
        )
    }
}
