package io.github.peningtonj.recordcollection.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.sql.SQLException

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

    private fun setUserVersion(driver: SqlDriver, version: Long) {
        driver.execute(null, "PRAGMA user_version = $version;", 0)
    }

    private fun getCurrentVersion(driver: JdbcSqliteDriver): Long {
        return try {
            driver.executeQuery(
                identifier = null,
                sql = "PRAGMA user_version;",
                parameters = 0,
                mapper = { cursor ->
                    cursor.next()
                    QueryResult.Value(cursor.getLong(0))
                }
            ).value ?: 0L
        } catch (e: Exception) {
            0L
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

        val dbVersion = getCurrentVersion(driver)
        val schemaVersion = RecordCollectionDatabase.Schema.version
        if (schemaVersion > dbVersion) {
            println("Migrating database from version ${RecordCollectionDatabase.Schema.version} to $dbVersion")
            RecordCollectionDatabase.Schema.migrate(
                driver,
                dbVersion,
                schemaVersion,
            )
            setUserVersion(driver, schemaVersion)
        }
        return driver
    }
}