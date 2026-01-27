#!/usr/bin/env kotlin

/**
 * Migration script to update existing album IDs to use the new computed ID format.
 * 
 * This script:
 * 1. Reads all albums from the database
 * 2. Computes new IDs based on album name and primary artist
 * 3. Updates all related tables (ratings, tags, collections) with the new IDs
 * 4. Updates the albums table with new IDs
 * 
 * Run with: ./migrate_album_ids.kts <path_to_database>
 */

import java.sql.DriverManager
import java.sql.Connection

fun generateAlbumId(name: String, artist: String): String {
    return "${name.lowercase().trim()}|${artist.lowercase().trim()}"
        .hashCode()
        .toString(36)
        .replace("-", "0")
}

fun migrateDatabase(dbPath: String) {
    val url = "jdbc:sqlite:$dbPath"
    
    DriverManager.getConnection(url).use { conn ->
        conn.autoCommit = false
        
        println("Starting migration...")
        
        try {
            // Step 1: Create a mapping table of old ID -> new ID
            val idMapping = mutableMapOf<String, String>()
            
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery("SELECT id, name, primary_artist FROM albums")
            
            while (rs.next()) {
                val oldId = rs.getString("id")
                val name = rs.getString("name")
                val primaryArtist = rs.getString("primary_artist")
                val newId = generateAlbumId(name, primaryArtist)
                
                idMapping[oldId] = newId
                println("  $name by $primaryArtist")
                println("    Old ID: $oldId")
                println("    New ID: $newId")
            }
            rs.close()
            stmt.close()
            
            println("\nFound ${idMapping.size} albums to migrate")
            
            // Step 2: Create temporary tables with new structure
            conn.createStatement().use { it.execute(
                """
                CREATE TABLE albums_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    primary_artist TEXT NOT NULL,
                    artists TEXT NOT NULL,
                    release_date TEXT,
                    total_tracks INTEGER NOT NULL,
                    spotify_uri TEXT NOT NULL,
                    added_at TEXT NOT NULL,
                    album_type TEXT NOT NULL,
                    images TEXT NOT NULL,
                    updated_at INTEGER NOT NULL,
                    external_ids TEXT,
                    release_group_id TEXT,
                    in_library INTEGER NOT NULL
                )
                """
            )}
            
            conn.createStatement().use { it.execute(
                """
                CREATE TABLE ratings_new (
                    album_id TEXT NOT NULL PRIMARY KEY,
                    rating INTEGER NOT NULL,
                    FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE CASCADE
                )
                """
            )}
            
            conn.createStatement().use { it.execute(
                """
                CREATE TABLE album_tags_new (
                    album_id TEXT NOT NULL,
                    tag_id TEXT NOT NULL,
                    PRIMARY KEY (album_id, tag_id),
                    FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE CASCADE,
                    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
                )
                """
            )}
            
            conn.createStatement().use { it.execute(
                """
                CREATE TABLE collection_albums_new (
                    collection_id TEXT NOT NULL,
                    album_id TEXT NOT NULL,
                    added_at TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    PRIMARY KEY (collection_id, album_id),
                    FOREIGN KEY (collection_id) REFERENCES album_collections(id) ON DELETE CASCADE,
                    FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE CASCADE
                )
                """
            )}
            
            println("\nMigrating albums table...")
            val albumInsert = conn.prepareStatement(
                """
                INSERT INTO albums_new 
                SELECT ?, name, primary_artist, artists, release_date, total_tracks, 
                       spotify_uri, added_at, album_type, images, updated_at, 
                       external_ids, release_group_id, in_library
                FROM albums WHERE id = ?
                """
            )
            
            var migratedAlbums = 0
            for ((oldId, newId) in idMapping) {
                albumInsert.setString(1, newId)
                albumInsert.setString(2, oldId)
                albumInsert.executeUpdate()
                migratedAlbums++
            }
            println("  Migrated $migratedAlbums albums")
            
            println("\nMigrating ratings table...")
            val ratingsInsert = conn.prepareStatement(
                "INSERT INTO ratings_new SELECT ?, rating FROM ratings WHERE album_id = ?"
            )
            var migratedRatings = 0
            for ((oldId, newId) in idMapping) {
                ratingsInsert.setString(1, newId)
                ratingsInsert.setString(2, oldId)
                val count = ratingsInsert.executeUpdate()
                if (count > 0) migratedRatings += count
            }
            println("  Migrated $migratedRatings ratings")
            
            println("\nMigrating album_tags table...")
            val tagsInsert = conn.prepareStatement(
                "INSERT INTO album_tags_new SELECT ?, tag_id FROM album_tags WHERE album_id = ?"
            )
            var migratedTags = 0
            for ((oldId, newId) in idMapping) {
                tagsInsert.setString(1, newId)
                tagsInsert.setString(2, oldId)
                val count = tagsInsert.executeUpdate()
                if (count > 0) migratedTags += count
            }
            println("  Migrated $migratedTags tag associations")
            
            println("\nMigrating collection_albums table...")
            val collectionInsert = conn.prepareStatement(
                """
                INSERT INTO collection_albums_new 
                SELECT collection_id, ?, added_at, position 
                FROM collection_albums WHERE album_id = ?
                """
            )
            var migratedCollections = 0
            for ((oldId, newId) in idMapping) {
                collectionInsert.setString(1, newId)
                collectionInsert.setString(2, oldId)
                val count = collectionInsert.executeUpdate()
                if (count > 0) migratedCollections += count
            }
            println("  Migrated $migratedCollections collection associations")
            
            println("\nReplacing old tables with new tables...")
            conn.createStatement().use { 
                it.execute("DROP TABLE collection_albums")
                it.execute("DROP TABLE album_tags")
                it.execute("DROP TABLE ratings")
                it.execute("DROP TABLE albums")
                
                it.execute("ALTER TABLE albums_new RENAME TO albums")
                it.execute("ALTER TABLE ratings_new RENAME TO ratings")
                it.execute("ALTER TABLE album_tags_new RENAME TO album_tags")
                it.execute("ALTER TABLE collection_albums_new RENAME TO collection_albums")
            }
            
            conn.commit()
            println("\n✅ Migration completed successfully!")
            println("\nSummary:")
            println("  Albums: $migratedAlbums")
            println("  Ratings: $migratedRatings")
            println("  Tags: $migratedTags")
            println("  Collections: $migratedCollections")
            
        } catch (e: Exception) {
            conn.rollback()
            println("\n❌ Migration failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}

// Main execution
if (args.isEmpty()) {
    println("Usage: ./migrate_album_ids.kts <path_to_database>")
    println("Example: ./migrate_album_ids.kts ~/Library/Application\\ Support/Record\\ Collection/recordcollection.db")
    System.exit(1)
}

val dbPath = args[0]
println("Migrating database: $dbPath")
migrateDatabase(dbPath)
