# Firebase Migration Guide

## Overview

This guide documents the migration from SQLDelight to Firebase for the Record Collection app, starting with the desktop implementation.

## Migration Strategy

### Phase 1: Database Analysis ✅ COMPLETE
**Status**: Complete  
**Deliverable**: Migration Database Reporter tool

Created a standalone utility that analyzes the existing SQLDelight database:
- Reports on all 12 tables
- Shows row counts (11,412 total rows)
- Displays schema details (columns, types, constraints)

**Usage**:
```bash
./report-db.sh
```

### Phase 2: Firebase Schema Design 🔄 NEXT
**Status**: Planned  
**Goals**:
- Design Firestore data model
- Map SQLDelight tables to Firestore collections
- Plan data structure optimizations for Firebase
- Consider denormalization strategies

**Key Considerations**:
- Firestore doesn't support joins (denormalization may be needed)
- Need to handle many-to-many relationships (album_tags, album_collections)
- Consider subcollections vs root collections
- Plan for offline support
- Query patterns for collection filtering

### Phase 3: Data Migration Tool 🔄 PLANNED
**Status**: Planned  
**Goals**:
- Export data from SQLDelight
- Transform data for Firebase format
- Import data to Firestore
- Validate migration

**Features**:
- Batch processing for large datasets
- Error handling and retry logic
- Progress reporting
- Rollback capability
- Data validation

### Phase 4: Desktop App Update 🔄 PLANNED
**Status**: Planned  
**Goals**:
- Replace SQLDelight with Firebase SDK
- Update repository layer
- Implement real-time listeners
- Test with migrated data

**Components to Update**:
- Database driver
- Repository implementations
- Data models (if needed)
- Dependency injection

## Current Database Structure

### Tables Summary

| Table | Rows | Key Relationships |
|-------|------|-------------------|
| **albums** | 841 | Primary entity |
| **tracks** | 5,116 | album_id → albums.id |
| **album_tags** | 3,729 | album_id → albums.id, tag_id → tags.tag_id |
| **collection_albums** | 690 | album_id → albums.id, collection_name → album_collections.name |
| **artists** | 466 | Referenced by albums.artists (JSON) |
| **tags** | 394 | Referenced by album_tags |
| **album_ratings** | 153 | album_id → albums.id |
| **album_collections** | 19 | Referenced by collection_albums |
| **collection_folders** | 2 | Hierarchical structure |
| **collection_filters** | 1 | collection_name → album_collections.name |
| **auths** | 1 | Spotify OAuth tokens |
| **profiles** | 0 | User profile (unused) |

### Data Relationships

```
albums (841)
  ├── tracks (5,116) - via album_id
  ├── album_ratings (153) - via album_id
  ├── album_tags (3,729) - via album_id
  └── collection_albums (690) - via album_id
      └── album_collections (19) - via collection_name
          └── collection_folders (2) - via parent hierarchy

artists (466) - referenced in albums.artists JSON

tags (394)
  └── album_tags (3,729) - via tag_id

auths (1) - standalone
profiles (0) - standalone
collection_filters (1) - references album_collections
```

## Tools & Scripts

### Database Reporter
**Path**: `migration-reporter/`  
**Command**: `./report-db.sh`  
**Purpose**: Analyze current database structure

### Next: Migration Tool
**Path**: TBD  
**Command**: TBD  
**Purpose**: Migrate data from SQLDelight to Firebase

## Firebase Setup

### Prerequisites
- Firebase project created ✅
- `google-services.json` configured ✅
- Firebase dependencies added to build

### Configuration Files
- Desktop: `composeApp/google-services.json`
- Android: TBD

## Notes & Decisions

### Data Model Considerations

1. **Albums Collection**
   - Store as root collection `/albums/{albumId}`
   - Denormalize artist names for quick display
   - Keep full artist data in separate `/artists/{artistId}` collection

2. **Tracks**
   - Option A: Subcollection `/albums/{albumId}/tracks/{trackId}`
   - Option B: Root collection with album_id field
   - Recommendation: Subcollection for better organization

3. **Tags & Ratings**
   - Store as subcollections under albums
   - `/albums/{albumId}/tags/{tagId}`
   - `/albums/{albumId}/rating` (single document)

4. **Collections**
   - Store as root collection `/collections/{collectionName}`
   - Album references as array or subcollection
   - Need to support ordering

5. **Authentication**
   - Use Firebase Auth instead of storing tokens
   - Store Spotify tokens securely in user profile

### Migration Order

1. Static data first (tags, artists)
2. Core entities (albums, tracks)
3. Relationships (ratings, collections)
4. User data (auth, profiles)

## Timeline

- ✅ Phase 1: Database Analysis (Complete)
- 🔄 Phase 2: Schema Design (Next - 1-2 days)
- 🔄 Phase 3: Migration Tool (3-5 days)
- 🔄 Phase 4: App Update (5-7 days)

**Total Estimated**: 2-3 weeks

## Resources

- [Firestore Data Model Guide](https://firebase.google.com/docs/firestore/data-model)
- [SQLDelight Documentation](https://cashapp.github.io/sqldelight/)
- [Firebase Desktop SDK](https://firebase.google.com/docs/admin/setup)

## Getting Started

1. Run database analysis:
   ```bash
   ./report-db.sh
   ```

2. Review current schema in [migration-reporter/README.md](migration-reporter/README.md)

3. Design Firebase schema (Phase 2)

4. Implement migration tool (Phase 3)

5. Update desktop app (Phase 4)
