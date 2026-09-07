# Database Migration Quick Reference

## Available Commands

### 1. Report on Current Database
```bash
./report-db.sh
```
Shows all 12 tables, row counts (11,412 total), and complete schema details.

### 2. Test Firebase Connection
```bash
cd migration-reporter
../gradlew testFirebase
```
Verifies Firebase credentials and connectivity.

### 3. Migrate Data to Firebase
```bash
./migrate-to-firebase.sh
```
**⚠️ WARNING**: This copies all data to Firebase. See [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) first!

Or run directly:
```bash
cd migration-reporter
../gradlew migrate
```

## Current Database Stats

- **Total Tables**: 12
- **Total Rows**: 11,412
- **Location**: `~/Library/Application Support/RecordCollection/recordcollection.db` (macOS)

### Top Tables by Row Count

1. `tracks` - 5,116 rows
2. `album_tags` - 3,729 rows
3. `albums` - 841 rows
4. `collection_albums` - 690 rows
5. `artists` - 466 rows

## Documentation

- **[MIGRATION_GUIDE.md](MIGRATION_GUIDE.md)** - Complete migration documentation with schema details
- **[FIREBASE_MIGRATION.md](FIREBASE_MIGRATION.md)** - Overall migration strategy and phases
- **[migration-reporter/README.md](migration-reporter/README.md)** - Database reporter tool docs

## Migration Process

1. ✅ **Report Database** - Understand current data structure
2. ✅ **Test Firebase** - Verify Firebase connection works  
3. ⚠️  **Read Migration Guide** - Understand what will happen
4. 🔥 **Run Migration** - Copy data to Firebase
5. ✅ **Verify in Console** - Check Firebase Console for data
6. 🚀 **Update Desktop App** - Switch to Firebase SDK

## Project Structure

```
Record Collection/
├── report-db.sh                    # Quick database report
├── migrate-to-firebase.sh          # Run full migration (with confirmation)
├── DATABASE_MIGRATION_README.md    # This file
├── MIGRATION_GUIDE.md              # Detailed migration docs
├── FIREBASE_MIGRATION.md           # Strategy and phases
└── migration-reporter/             # Migration tools
    ├── README.md
    ├── build.gradle.kts
    └── src/main/kotlin/.../
        ├── MigrationDbReporter.kt  # Database analyzer
        ├── Firebase.kt             # Firebase initialization
        └── DataMigrator.kt         # Migration logic
```

## Quick Tips

**Before migrating:**
- ✅ Run `./report-db.sh` to see what you have
- ✅ Run `cd migration-reporter && ../gradlew testFirebase` to verify Firebase works
- ✅ Read [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md)
- ✅ Ensure your Firebase project is ready
- ⚠️  The original SQLite database is NOT modified (read-only)

**After migrating:**
- Check Firebase Console: https://console.firebase.google.com
- Look for collections: `albums`, `artists`, `tags`, `collections`, etc.
- Verify document counts match the report
