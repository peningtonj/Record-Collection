#!/usr/bin/env python3
"""
Migration script to add spotify_id column to albums table.
This extracts the Spotify ID from the spotify_uri field.

The spotify_uri format is: spotify:album:<spotify_id>
"""

import sqlite3
import sys
from pathlib import Path

def migrate_database(db_path: str):
    """Add spotify_id column and populate it from spotify_uri"""
    
    print(f"🔧 Starting migration for: {db_path}")
    
    # Connect to database
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    try:
        # Check if column already exists
        cursor.execute("PRAGMA table_info(albums)")
        columns = [col[1] for col in cursor.fetchall()]
        
        if 'spotify_id' in columns:
            print("⚠️  Column 'spotify_id' already exists.")
            print("🔄 Rebuilding table with correct column order...")
            
            # Create new table with correct column order
            cursor.execute("""
                CREATE TABLE albums_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    spotify_id TEXT,
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
            """)
            
            # Copy data to new table with correct column order
            cursor.execute("""
                INSERT INTO albums_new 
                SELECT id, spotify_id, name, primary_artist, artists, release_date, 
                       total_tracks, spotify_uri, added_at, album_type, images, 
                       updated_at, external_ids, release_group_id, in_library
                FROM albums
            """)
            
            # Drop old table and rename new one
            cursor.execute("DROP TABLE albums")
            cursor.execute("ALTER TABLE albums_new RENAME TO albums")
            
            conn.commit()
            print("✅ Table rebuilt with correct column order")
        
        # Verify results
        cursor.execute("SELECT COUNT(*) FROM albums")
        total_albums = cursor.fetchone()[0]
        
        cursor.execute("SELECT COUNT(*) FROM albums WHERE spotify_id IS NOT NULL")
        albums_with_spotify_id = cursor.fetchone()[0]
        
        print(f"\n✅ Migration completed successfully!")
        print(f"📊 Total albums: {total_albums}")
        print(f"📊 Albums with Spotify ID: {albums_with_spotify_id}")
        
        if albums_with_spotify_id < total_albums:
            missing = total_albums - albums_with_spotify_id
            print(f"⚠️  {missing} albums don't have a Spotify ID (might be manually added)")
        
    except Exception as e:
        print(f"❌ Error during migration: {e}")
        conn.rollback()
        raise
    finally:
        conn.close()

def main():
    # Default database path (macOS desktop app location)
    default_db = Path.home() / "Library" / "Application Support" / "RecordCollection" / "recordcollection.db"
    
    if len(sys.argv) > 1:
        db_path = sys.argv[1]
    elif default_db.exists():
        db_path = str(default_db)
    else:
        print("❌ Database not found!")
        print(f"Expected location: {default_db}")
        print("\nUsage: python3 add_spotify_id_column.py [path/to/database.db]")
        sys.exit(1)
    
    if not Path(db_path).exists():
        print(f"❌ Database file not found: {db_path}")
        sys.exit(1)
    
    migrate_database(db_path)

if __name__ == "__main__":
    main()
