package io.github.peningtonj.recordcollection.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriver {
    actual fun createDriver(): SqlDriver {
        val databasePath = "record_collection.db"
        val dbFile = File(databasePath)

        // Create the driver with the file path
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")

        // Create the schema if it doesn't exist
        if (!dbFile.exists()) {
            RecordCollectionDatabase.Schema.create(driver)
        }

        return driver
    }
}
