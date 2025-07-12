import io.github.aakira.napier.Napier
import java.sql.*

class DatabaseMigration {
    
    fun migrateDatabases(backupDbPath: String, targetDbPath: String) {
        try {
            // Connect to target database
            val targetConn = DriverManager.getConnection("jdbc:sqlite:$targetDbPath")
            
            // Attach backup database
            val stmt = targetConn.createStatement()
            stmt.execute("ATTACH DATABASE '$backupDbPath' AS backup")
            println("Attached backup database")

            // Collect all table names first
            val tableNames = mutableListOf<String>()
            val tables = stmt.executeQuery("SELECT name FROM backup.sqlite_master WHERE type='table'")
            val skip = listOf("sqlite_sequence", "auths")
            while (tables.next()) {
                val tableName = tables.getString("name")
                if (!skip.contains(tableName)) {
                    tableNames.add(tableName)
                }
            }
            tables.close() // Close the ResultSet

            // Now process each table
            tableNames.forEach { tableName ->
                println(tableName)
                try {
                    stmt.execute("INSERT OR REPLACE INTO main.$tableName SELECT * FROM backup.$tableName")
                    println("Migrated table: $tableName")
                } catch (e: SQLException) {
                    println("Error migrating table: $tableName - ${e.message}")
                }
            }
            
            // Detach backup database
            stmt.execute("DETACH DATABASE backup")
            
            stmt.close()
            targetConn.close()
            
            println("Migration completed successfully!")
            
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }
}