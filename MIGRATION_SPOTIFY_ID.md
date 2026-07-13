# Spotify ID Migration

## Overview

This migration adds a `spotify_id` column to the `albums` table. Previously, we only stored Spotify IDs within the `spotify_uri` field (format: `spotify:album:<id>`), but we need direct access to Spotify IDs for API calls.

## Changes Made

### Database Schema (`Albums.sq`)
- Added `spotify_id TEXT` column to the `albums` table
- Updated `insert` statement to include `spotify_id` parameter
- Added `getAlbumBySpotifyId` query to retrieve albums by their Spotify ID

### Domain Model (`Album.kt`)
- Added `spotifyId: String?` field to the `Album` data class

### Mappers
- `AlbumMapper.kt`: Updated all `toDomain()` methods to extract and populate `spotifyId` from DTOs
- `CollectionAlbumMapper.kt`: Updated to include `spotify_id` when constructing `Albums` entities

### Repository (`AlbumRepository.kt`)
- Updated `saveAlbum(AlbumDto)` to store the Spotify ID
- Updated `saveAlbum(Album)` to store the Spotify ID from the domain model

### Tests (`AlbumRepositoryTest.kt`)
- Updated all test mocks to use 15 parameters (was 14) for the insert statement
- Added `spotify_id` to verification assertions

## Migration Script

A Python migration script has been created: `add_spotify_id_column.py`

### Usage

```bash
# Automatic detection (looks in standard macOS location)
python3 add_spotify_id_column.py

# Or specify database path
python3 add_spotify_id_column.py /path/to/recordcollection.db
```

### What It Does

1. Adds the `spotify_id` column if it doesn't exist
2. Extracts Spotify IDs from existing `spotify_uri` values (format: `spotify:album:<id>`)
3. Updates all albums with their Spotify IDs
4. Reports statistics about the migration

### Expected Output

```
🔧 Starting migration for: /path/to/recordcollection.db
➕ Adding spotify_id column...
✅ Column added successfully
🔄 Extracting Spotify IDs from spotify_uri...

✅ Migration completed successfully!
📊 Total albums: 817
📊 Albums with Spotify ID: 817
📊 Albums updated: 817
```

## Why This Change?

Previously, our ID system used a hash of album name and artist to create stable IDs across different Spotify ID changes. This worked well for our internal storage, but:

1. **API Calls**: When interacting with the Spotify API, we need the actual Spotify ID
2. **Performance**: Extracting the ID from `spotify_uri` every time is inefficient
3. **Clarity**: Having both IDs explicitly stored makes the code more maintainable

Now we have:
- `id`: Our internal stable ID (hash of name + artist)
- `spotifyId`: Spotify's ID for API calls
- `spotifyUri`: Full Spotify URI (for opening in Spotify app)

## Running After Migration

After running the migration script:

1. Rebuild the project: `./gradlew compileKotlinDesktop`
2. Run the app: `./gradlew run`
3. All existing albums will now have their Spotify IDs populated
4. New albums will automatically get both IDs when saved

## Rollback (if needed)

If you need to rollback:

```sql
-- Remove the column (SQLite doesn't support DROP COLUMN in older versions)
-- You'll need to recreate the table without the column
-- Or just leave it as nullable - it won't cause issues
```

Note: Since `spotify_id` is nullable, having the column without data is harmless.
