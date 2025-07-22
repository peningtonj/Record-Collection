package io.github.peningtonj.recordcollection.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriver {
    fun getDbPath(): String {
        val userHome = System.getProperty("user.home")
        val osName = System.getProperty("os.name").lowercase()

        val baseDir = when {
            osName.contains("mac") -> "$userHome/Library/Application Support"
            osName.contains("win") -> System.getenv("APPDATA") ?: "$userHome/AppData/Roaming"
            else -> System.getenv("XDG_DATA_HOME") ?: "$userHome/.local/share"
        }

        return "$baseDir/RecordCollection/recordcollection.db".also {
            File(it).parentFile.mkdirs()
        }
    }
    actual fun createDriver(): SqlDriver {
        val dbPath = getDbPath()
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