#!/bin/bash
# restore_ratings.sh - Restore ratings data

if [ -z "$1" ]; then
    echo "Usage: $0 <ratings_backup.csv>"
    exit 1
fi

RATINGS_FILE="$1"
DB_FILE="$2"

# Check if files exist
if [ ! -f "$RATINGS_FILE" ]; then
    echo "Error: Ratings file $RATINGS_FILE not found"
    exit 1
fi

if [ ! -f "$DB_FILE" ]; then
    echo "Error: Database file $DB_FILE not found"
    exit 1
fi

# Create temporary table and import ratings
sqlite3 "$DB_FILE" <<EOF
CREATE TEMP TABLE temp_ratings (
    album_id TEXT PRIMARY KEY,
    rating INTEGER
);

.mode csv
.import "$RATINGS_FILE" temp_ratings

-- Remove header row if it exists
DELETE FROM temp_ratings WHERE album_id = 'album_id';

-- Update existing albums with ratings
INSERT OR REPLACE INTO album_ratings(album_id, rating)
SELECT album_id, rating FROM temp_ratings;

DROP TABLE temp_ratings;
EOF

echo "Ratings restored from $RATINGS_FILE"

