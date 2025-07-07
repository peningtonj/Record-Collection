import app.cash.sqldelight.ColumnAdapter
import io.github.peningtonj.recordcollection.db.Artist_entity
import io.github.peningtonj.recordcollection.db.DatabaseDriver
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import kotlinx.serialization.json.Json

class DatabaseHelper(databaseDriver: DatabaseDriver) {
    private val driver = databaseDriver.createDriver()

    // Create the column adapter for the genres list
    private val artistEntityAdapter = Artist_entity.Adapter(
        genresAdapter = object : ColumnAdapter<List<String>, String> {
            override fun decode(databaseValue: String): List<String> {
                return if (databaseValue.isBlank()) {
                    emptyList()
                } else {
                    try {
                        Json.decodeFromString<List<String>>(databaseValue)
                    } catch (e: Exception) {
                        // Fallback: split by comma if it's not JSON
                        databaseValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    }
                }
            }

            override fun encode(value: List<String>): String {
                return Json.encodeToString(value)
            }
        }
    )

    val database = RecordCollectionDatabase(
        driver = driver,
        artist_entityAdapter = artistEntityAdapter
    )

    fun close() {
            driver.close()
    }

}