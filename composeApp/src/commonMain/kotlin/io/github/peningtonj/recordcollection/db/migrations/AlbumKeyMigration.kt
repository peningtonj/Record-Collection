package io.github.peningtonj.recordcollection.db.migrations

import app.cash.sqldelight.db.SqlDriver
import io.github.aakira.napier.Napier

/**
 * Migration to change albums table from using spotify_id as primary key
 * to using composite key (name, primary_artist).
 * 
 * This handles the case where Spotify sometimes changes album IDs.
 */
object AlbumKeyMigration {
    
    fun migrate(driver: SqlDriver) {
        Napier.i("Starting album key migration...")
        
        try {
            driver.execute(null, "BEGIN TRANSACTION", 0)
            
            // Step 1: Rename existing tables to _old
            renameExistingTables(driver)
            
            // Step 2: Create new tables with composite keys
            createNewTables(driver)
            
            // Step 3: Migrate data from old tables to new tables
            migrateData(driver)
            
            // Step 4: Drop old tables
            dropOldTables(driver)
            
            driver.execute(null, "COMMIT", 0)
            Napier.i("Album key migration completed successfully")
            
        } catch (e: Exception) {
            Napier.e("Migration failed: ${e.message}", e)
            driver.execute(null, "ROLLBACK", 0)
            throw e
        }
    }
    
    private fun renameExistingTables(driver: SqlDriver) {
        Napier.d("Renaming existing tables...")
        driver.execute(null, "ALTER TABLE albums RENAME TO albums_old", 0)
        driver.execute(null, "ALTER TABLE tracks RENAME TO tracks_old", 0)
        driver.execute(null, "ALTER TABLE album_ratings RENAME TO album_ratings_old", 0)
        driver.execute(null, "ALTER TABLE album_tags RENAME TO album_tags_old", 0)
        driver.execute(null, "ALTER TABLE collection_albums RENAME TO collection_albums_old", 0)
    }
    
    private fun createNewTables(driver: SqlDriver) {
        Napier.d("Creating new tables with composite keys...")
        
        // Create new albums table
        driver.execute(null, """
            CREATE TABLE albums (
                name TEXT NOT NULL,
                primary_artist TEXT NOT NULL,
                spotify_id TEXT,
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
                in_library INTEGER NOT NULL,
                PRIMARY KEY (name, primary_artist)
            )
        """.trimIndent(), 0)
        
        driver.execute(null, "CREATE INDEX idx_albums_spotify_id ON albums(spotify_id)", 0)
        
        // Create new tracks table
        driver.execute(null, """
            CREATE TABLE tracks (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                album_name TEXT NOT NULL,
                album_artist TEXT NOT NULL,
                primary_artist TEXT NOT NULL,
                artists TEXT NOT NULL,
                duration_ms INTEGER NOT NULL,
                is_explicit INTEGER NOT NULL,
                track_number INTEGER NOT NULL,
                disc_number INTEGER NOT NULL,
                spotify_uri TEXT NOT NULL,
                preview_url TEXT,
                popularity INTEGER,
                is_saved INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(album_name, album_artist) REFERENCES albums(name, primary_artist) ON DELETE CASCADE
            )
        """.trimIndent(), 0)
        
        // Create new album_ratings table
        driver.execute(null, """
            CREATE TABLE album_ratings (
                album_name TEXT NOT NULL,
                album_artist TEXT NOT NULL,
                rating INTEGER,
                PRIMARY KEY (album_name, album_artist),
                FOREIGN KEY (album_name, album_artist) REFERENCES albums(name, primary_artist) ON DELETE CASCADE
            )
        """.trimIndent(), 0)
        
        // Create new album_tags table
        driver.execute(null, """
            CREATE TABLE album_tags (
                album_name TEXT NOT NULL,
                album_artist TEXT NOT NULL,
                tag_id TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                PRIMARY KEY (album_name, album_artist, tag_id),
                FOREIGN KEY(album_name, album_artist) REFERENCES albums(name, primary_artist) ON DELETE CASCADE,
                FOREIGN KEY(tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
            )
        """.trimIndent(), 0)
        
        // Create new collection_albums table
        driver.execute(null, """
            CREATE TABLE collection_albums (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                collection_name TEXT NOT NULL,
                album_name TEXT NOT NULL,
                album_artist TEXT NOT NULL,
                position INTEGER NOT NULL,
                added_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                FOREIGN KEY (collection_name) REFERENCES album_collections(name) ON DELETE CASCADE,
                FOREIGN KEY (album_name, album_artist) REFERENCES albums(name, primary_artist) ON DELETE CASCADE,
                UNIQUE(collection_name, album_name, album_artist)
            )
        """.trimIndent(), 0)
        
        driver.execute(null, "CREATE INDEX idx_collection_albums_collection_name ON collection_albums(collection_name)", 0)
        driver.execute(null, "CREATE INDEX idx_collection_albums_position ON collection_albums(collection_name, position)", 0)
    }
    
    private fun migrateData(driver: SqlDriver) {
        Napier.d("Migrating data to new tables...")
        
        // Migrate albums - store old spotify ID as spotify_id column
        driver.execute(null, """
            INSERT INTO albums (
                name, primary_artist, spotify_id, artists, release_date, total_tracks,
                spotify_uri, added_at, album_type, images, updated_at, external_ids,
                release_group_id, in_library
            )
            SELECT 
                name, primary_artist, id, artists, release_date, total_tracks,
                spotify_uri, added_at, album_type, images, updated_at, external_ids,
                release_group_id, in_library
            FROM albums_old
        """.trimIndent(), 0)
        
        // Migrate tracks - join with albums_old to get name and artist
        driver.execute(null, """
            INSERT INTO tracks (
                id, name, album_name, album_artist, primary_artist, artists,
                duration_ms, is_explicit, track_number, disc_number, spotify_uri,
                preview_url, popularity, is_saved
            )
            SELECT 
                t.id, t.name, a.name, a.primary_artist, t.primary_artist, t.artists,
                t.duration_ms, t.is_explicit, t.track_number, t.disc_number, t.spotify_uri,
                t.preview_url, t.popularity, t.is_saved
            FROM tracks_old t
            JOIN albums_old a ON t.album_id = a.id
        """.trimIndent(), 0)
        
        // Migrate ratings
        driver.execute(null, """
            INSERT INTO album_ratings (album_name, album_artist, rating)
            SELECT a.name, a.primary_artist, r.rating
            FROM album_ratings_old r
            JOIN albums_old a ON r.album_id = a.id
        """.trimIndent(), 0)
        
        // Migrate tags
        driver.execute(null, """
            INSERT INTO album_tags (album_name, album_artist, tag_id, created_at)
            SELECT a.name, a.primary_artist, t.tag_id, t.created_at
            FROM album_tags_old t
            JOIN albums_old a ON t.album_id = a.id
        """.trimIndent(), 0)
        
        // Migrate collection albums
        driver.execute(null, """
            INSERT INTO collection_albums (collection_name, album_name, album_artist, position, added_at)
            SELECT ca.collection_name, a.name, a.primary_artist, ca.position, ca.added_at
            FROM collection_albums_old ca
            JOIN albums_old a ON ca.album_id = a.id
        """.trimIndent(), 0)
    }
    
    private fun dropOldTables(driver: SqlDriver) {
        Napier.d("Dropping old tables...")
        driver.execute(null, "DROP TABLE IF EXISTS albums_old", 0)
        driver.execute(null, "DROP TABLE IF EXISTS tracks_old", 0)
        driver.execute(null, "DROP TABLE IF EXISTS album_ratings_old", 0)
        driver.execute(null, "DROP TABLE IF EXISTS album_tags_old", 0)
        driver.execute(null, "DROP TABLE IF EXISTS collection_albums_old", 0)
    }
}
