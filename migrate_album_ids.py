#!/usr/bin/env python3
"""
Migration script to update existing album IDs to use the new computed ID format.

This script:
1. Reads all albums from the database
2. Computes new IDs based on album name and primary artist
3. Updates all related tables (ratings, tags, collections) with the new IDs
4. Updates the albums table with new IDs

Run with: python3 migrate_album_ids.py <path_to_database>
"""

import sqlite3
import sys
from pathlib import Path


def generate_album_id(name: str, artist: str) -> str:
    """Generate a stable ID from album name and artist."""
    combined = f"{name.lower().strip()}|{artist.lower().strip()}"
    hash_val = hash(combined)
    # Convert to base36 and ensure positive
    base36 = format(abs(hash_val), 'X').lower()
    return base36.replace('-', '0')


def migrate_database(db_path: str):
    """Migrate the database to use computed album IDs."""
    if not Path(db_path).exists():
        print(f"❌ Database not found: {db_path}")
        sys.exit(1)
    
    # Create backup
    backup_path = f"{db_path}.backup_{Path(__file__).stem}"
    print(f"Creating backup: {backup_path}")
    Path(backup_path).write_bytes(Path(db_path).read_bytes())
    
    conn = sqlite3.connect(db_path)
    conn.execute("PRAGMA foreign_keys = OFF")
    
    try:
        cursor = conn.cursor()
        
        print("\nStarting migration...")
        
        # Step 1: Create mapping of old ID -> new ID and identify duplicates
        cursor.execute("SELECT id, name, primary_artist, added_at FROM albums ORDER BY added_at ASC")
        albums = cursor.fetchall()
        
        id_mapping = {}
        new_id_to_old_ids = {}  # Track which old IDs map to the same new ID
        
        for old_id, name, primary_artist, added_at in albums:
            new_id = generate_album_id(name, primary_artist)
            id_mapping[old_id] = new_id
            
            if new_id not in new_id_to_old_ids:
                new_id_to_old_ids[new_id] = []
            new_id_to_old_ids[new_id].append((old_id, added_at))
            
            print(f"  {name} by {primary_artist}")
            print(f"    Old ID: {old_id}")
            print(f"    New ID: {new_id}")
        
        # Identify duplicates
        duplicates = {new_id: old_ids for new_id, old_ids in new_id_to_old_ids.items() if len(old_ids) > 1}
        
        print(f"\nFound {len(id_mapping)} albums to migrate")
        if duplicates:
            print(f"\n⚠️  Found {len(duplicates)} albums with duplicate entries:")
            for new_id, old_ids in duplicates.items():
                cursor.execute("SELECT name, primary_artist FROM albums WHERE id = ?", (old_ids[0][0],))
                name, artist = cursor.fetchone()
                print(f"  {name} by {artist}")
                print(f"    Will merge {len(old_ids)} entries into ID: {new_id}")
                print(f"    Keeping earliest entry from: {min(dt for _, dt in old_ids)}")
        else:
            print("No duplicate albums found")
        
        # Step 2: Create temporary tables
        print("\nCreating temporary tables...")
        
        cursor.execute("""
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
        """)
        
        cursor.execute("""
            CREATE TABLE album_ratings_new (
                album_id TEXT NOT NULL PRIMARY KEY,
                rating INTEGER NOT NULL,
                FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE CASCADE
            )
        """)
        
        cursor.execute("""
            CREATE TABLE album_tags_new (
                album_id TEXT NOT NULL,
                tag_id TEXT NOT NULL,
                PRIMARY KEY (album_id, tag_id),
                FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE CASCADE,
                FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
            )
        """)
        
        cursor.execute("""
            CREATE TABLE collection_albums_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                collection_name TEXT NOT NULL,
                album_id TEXT NOT NULL,
                position INTEGER NOT NULL,
                added_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                FOREIGN KEY (collection_name) REFERENCES album_collections(name) ON DELETE CASCADE,
                FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE CASCADE,
                UNIQUE(collection_name, album_id)
            )
        """)
        
        # Step 3: Migrate data (handling duplicates by keeping the earliest entry)
        print("\nMigrating albums table...")
        migrated_albums = 0
        skipped_duplicates = 0
        
        for new_id, old_ids_with_dates in new_id_to_old_ids.items():
            # Sort by added_at to keep the earliest entry
            old_ids_with_dates.sort(key=lambda x: x[1])
            primary_old_id = old_ids_with_dates[0][0]  # Keep the earliest one
            
            cursor.execute("""
                INSERT INTO albums_new 
                SELECT ?, name, primary_artist, artists, release_date, total_tracks,
                       spotify_uri, added_at, album_type, images, updated_at,
                       external_ids, release_group_id, in_library
                FROM albums WHERE id = ?
            """, (new_id, primary_old_id))
            migrated_albums += 1
            
            if len(old_ids_with_dates) > 1:
                skipped_duplicates += len(old_ids_with_dates) - 1
        
        print(f"  Migrated {migrated_albums} albums")
        if skipped_duplicates > 0:
            print(f"  Merged {skipped_duplicates} duplicate entries")
        
        print("\nMigrating album_ratings table...")
        migrated_ratings = 0
        for new_id, old_ids_with_dates in new_id_to_old_ids.items():
            # Merge ratings from all duplicate entries (keep the highest rating)
            all_old_ids = [old_id for old_id, _ in old_ids_with_dates]
            placeholders = ','.join('?' * len(all_old_ids))
            
            cursor.execute(f"""
                SELECT MAX(rating) FROM album_ratings 
                WHERE album_id IN ({placeholders})
            """, all_old_ids)
            
            max_rating = cursor.fetchone()[0]
            if max_rating is not None:
                cursor.execute(
                    "INSERT INTO album_ratings_new (album_id, rating) VALUES (?, ?)",
                    (new_id, max_rating)
                )
                migrated_ratings += 1
        print(f"  Migrated {migrated_ratings} ratings")
        
        print("\nMigrating album_tags table...")
        migrated_tags = 0
        for new_id, old_ids_with_dates in new_id_to_old_ids.items():
            # Merge tags from all duplicate entries (union of all tags)
            all_old_ids = [old_id for old_id, _ in old_ids_with_dates]
            placeholders = ','.join('?' * len(all_old_ids))
            
            cursor.execute(f"""
                SELECT DISTINCT tag_id FROM album_tags 
                WHERE album_id IN ({placeholders})
            """, all_old_ids)
            
            tag_ids = [row[0] for row in cursor.fetchall()]
            for tag_id in tag_ids:
                cursor.execute(
                    "INSERT OR IGNORE INTO album_tags_new (album_id, tag_id) VALUES (?, ?)",
                    (new_id, tag_id)
                )
                migrated_tags += 1
        print(f"  Migrated {migrated_tags} tag associations")
        
        print("\nMigrating collection_albums table...")
        migrated_collections = 0
        for new_id, old_ids_with_dates in new_id_to_old_ids.items():
            # Merge collection memberships from all duplicate entries
            all_old_ids = [old_id for old_id, _ in old_ids_with_dates]
            placeholders = ','.join('?' * len(all_old_ids))
            
            cursor.execute(f"""
                SELECT DISTINCT collection_name, MIN(added_at) as added_at, MIN(position) as position
                FROM collection_albums 
                WHERE album_id IN ({placeholders})
                GROUP BY collection_name
            """, all_old_ids)
            
            for collection_name, added_at, position in cursor.fetchall():
                cursor.execute("""
                    INSERT OR IGNORE INTO collection_albums_new 
                    (collection_name, album_id, added_at, position) 
                    VALUES (?, ?, ?, ?)
                """, (collection_name, new_id, added_at, position))
                migrated_collections += 1
        print(f"  Migrated {migrated_collections} collection associations")
        
        # Step 4: Replace old tables with new tables
        print("\nReplacing old tables with new tables...")
        cursor.execute("DROP TABLE collection_albums")
        cursor.execute("DROP TABLE album_tags")
        cursor.execute("DROP TABLE album_ratings")
        cursor.execute("DROP TABLE albums")
        
        cursor.execute("ALTER TABLE albums_new RENAME TO albums")
        cursor.execute("ALTER TABLE album_ratings_new RENAME TO album_ratings")
        cursor.execute("ALTER TABLE album_tags_new RENAME TO album_tags")
        cursor.execute("ALTER TABLE collection_albums_new RENAME TO collection_albums")
        
        conn.commit()
        
        print("\n✅ Migration completed successfully!")
        print("\nSummary:")
        print(f"  Albums: {migrated_albums}")
        print(f"  Ratings: {migrated_ratings}")
        print(f"  Tags: {migrated_tags}")
        print(f"  Collections: {migrated_collections}")
        print(f"\nBackup saved to: {backup_path}")
        
    except Exception as e:
        conn.rollback()
        print(f"\n❌ Migration failed: {e}")
        import traceback
        traceback.print_exc()
        print(f"\nRestoring from backup: {backup_path}")
        Path(db_path).write_bytes(Path(backup_path).read_bytes())
        sys.exit(1)
    finally:
        conn.close()


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 migrate_album_ids.py <path_to_database>")
        print("Example: python3 migrate_album_ids.py ~/Library/Application\\ Support/Record\\ Collection/recordcollection.db")
        sys.exit(1)
    
    db_path = sys.argv[1]
    print(f"Migrating database: {db_path}")
    migrate_database(db_path)
