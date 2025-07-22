package io.github.peningtonj.recordcollection.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriver {
    actual fun createDriver(): SqlDriver {
        val dbPath = "${System.getProperty("user.home")}/Library/Application Support/RecordCollection/recordcollection.db"
        val dbFile = File(dbPath)
        dbFile.parentFile.mkdirs()

        // Create the driver with the file path
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")

        // Create the schema if it doesn't exist
        if (!dbFile.exists()) {
            RecordCollectionDatabase.Schema.create(driver)
        }

        return driver
    }
}