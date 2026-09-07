# SQLite to Firebase Migration

## Overview

This migration tool copies all data from your local SQLDelight/SQLite database to Firebase Firestore.

## What Gets Migrated

### ✅ Data Structure

The migration transforms your relational SQLite data into a Firestore document structure:

#### Root Collections:
- **`albums/`** (841 documents)
  - Contains: album metadata, ratings, tag_ids
  - Subcollection: `tracks/` (5,116 total across all albums)
  
- **`artists/`** (466 documents)
  - Contains: artist metadata, genres, images
  
- **`tags/`** (394 documents)
  - Contains: tag definitions
  
- **`collections/`** (19 documents)
  - Contains: collection metadata, albums array, filters
  
- **`collection_folders/`** (2 documents)
  - Contains: folder hierarchy
  
- **`auth_tokens/`** (1 document)
  - Contains: Spotify OAuth tokens

### 🔄 Data Transformations

1. **Tracks**: Moved from flat table to subcollections under albums
   - SQLite: `tracks` table with `album_id` FK
   - Firebase: `/albums/{albumId}/tracks/{trackId}`

2. **Ratings**: Merged into album documents
   - SQLite: `album_ratings` table
   - Firebase: `rating` field in album documents

3. **Album Tags**: Stored as array in album documents
   - SQLite: `album_tags` join table
   - Firebase: `tag_ids` array field in album documents

4. **Collection Albums**: Stored as array in collection documents
   - SQLite: `collection_albums` table
   - Firebase: `albums` array field in collection documents

5. **Integers to Booleans**: SQLite INTEGER (0/1) converted to Firebase Boolean
   - `in_library`, `is_explicit`, `is_saved`

## Running the Migration

### Prerequisites

1. ✅ Firebase project created
2. ✅ Service account key downloaded
3. ✅ SQLite database exists with data
4. ⚠️  **IMPORTANT**: Ensure your Firebase project is ready (this writes real data!)

### Quick Start

```bash
./migrate-to-firebase.sh
```

Or directly with Gradle:

```bash
cd migration-reporter
../gradlew migrate
```

### What Happens

The migration runs in this order:
1. 📋 Tags (394 rows)
2. 🎤 Artists (466 rows)
3. 💿 Albums (841 rows)
4. 🎵 Tracks (5,116 rows as subcollections)
5. ⭐ Album Ratings (153 rows, merged into albums)
6. 🏷️ Album Tags (3,729 relationships, stored as arrays)
7. 📚 Collections (19 rows)
8. 📖 Collection Albums (690 relationships, stored as arrays)
9. 📁 Collection Folders (2 rows)
10. 🔍 Collection Filters (1 row, merged into collections)
11. 🔐 Auth Tokens (1 row)

### Progress Reporting

The migration shows real-time progress:
```
📂 Connecting to SQLite database...
🔥 Starting Data Migration: SQLite → Firebase
================================================================================

📋 Migrating tags...
  ✓ Committed 394 tags so far...
  ✅ Migrated 394 tags

🎤 Migrating artists...
  ✅ Migrated 466 artists

💿 Migrating albums...
  ✓ Committed 500 albums so far...
  ✅ Migrated 841 albums

... and so on ...

✅ Migration Complete!
```

## Batch Processing

Firebase Firestore has a limit of **500 operations per batch**. The migration automatically:
- Commits batches every 500 operations
- Shows progress for large datasets
- Handles errors gracefully

## Safety Features

1. **Non-destructive**: Original SQLite database is untouched (read-only)
2. **Confirmation prompt**: Script asks for confirmation before running
3. **Error handling**: Catches and reports errors with stack traces
4. **Idempotent**: Uses `set()` which overwrites existing data (safe to re-run)

## Firestore Schema

### Albums Document Structure
```javascript
{
  // Basic metadata
  "name": "Album Name",
  "spotify_id": "abc123",
  "primary_artist": "Artist Name",
  "artists": "[JSON array]",
  "release_date": "2024-01-01",
  "album_type": "album",
  
  // Images and URIs
  "images": "[JSON array]",
  "spotify_uri": "spotify:album:...",
  
  // Metadata
  "total_tracks": 12,
  "added_at": "2024-01-01T...",
  "updated_at": 1234567890,
  "in_library": true,
  
  // Merged from other tables
  "rating": 5,              // from album_ratings
  "tag_ids": ["tag1", ...], // from album_tags
  
  // Optional fields
  "external_ids": "[JSON]",
  "release_group_id": "mbid-..."
}
```

### Tracks Subcollection
```javascript
/albums/{albumId}/tracks/{trackId}
{
  "name": "Track Name",
  "album_id": "album_id",
  "primary_artist": "Artist",
  "artists": "[JSON array]",
  "duration_ms": 240000,
  "is_explicit": false,
  "track_number": 1,
  "disc_number": 1,
  "spotify_uri": "spotify:track:...",
  "preview_url": "https://...",
  "popularity": 75,
  "is_saved": true
}
```

### Collections Document Structure
```javascript
{
  "name": "My Collection",
  "description": "Description text",
  "created_at": 1234567890,
  "updated_at": 1234567890,
  "parent_name": "parent_collection",
  
  // Merged data
  "filter": "filter_json",    // from collection_filters
  "albums": [                 // from collection_albums
    {
      "album_id": "album_id",
      "position": 1,
      "added_at": 1234567890
    }
  ]
}
```

## Troubleshooting

### "Database file not found"
Your SQLite database doesn't exist yet. Run the main app first to create data.

### "Service account key not found"
Update the path in `Firebase.kt` to point to your downloaded JSON key.

### Firebase Permission Denied
Check your Firebase project permissions and ensure the service account has write access.

### Out of Memory
The migration processes large datasets. If you encounter memory issues:
- Increase JVM heap size: `../gradlew migrate -Xmx2g`
- Or migrate tables individually (modify the code)

## Verification

After migration, you can verify the data in:
1. **Firebase Console**: https://console.firebase.google.com
2. Navigate to Firestore Database
3. Browse collections to see your migrated data

## Rollback

To rollback (delete migrated data), you can:
1. Use Firebase Console to delete collections manually
2. Or write a cleanup script (not included)

⚠️ **Note**: There's no automatic rollback - plan carefully!

## Next Steps

After successful migration:
1. ✅ Verify data in Firebase Console
2. Update desktop app to use Firebase SDK
3. Replace SQLDelight repository with Firebase repository
4. Test app with migrated data
5. Consider keeping SQLite as backup until fully tested

## Performance Notes

- **Migration time**: ~1-2 minutes for 11,412 rows
- **Batch size**: 500 operations per commit
- **Network dependent**: Speed depends on your internet connection
- **Firebase quotas**: Free tier should be sufficient for this dataset

## Cost Estimate

Firebase Free Tier includes:
- 50,000 document reads/day
- 20,000 document writes/day
- 20,000 document deletes/day
- 1 GB storage

This migration uses ~11,500 writes (well within free tier).
