#!/bin/bash
# backup_ratings.sh - Backup only ratings data

DB_FILE="$1"
BACKUP_DIR="backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p "$BACKUP_DIR"

# Export only ratings data
sqlite3 "$DB_FILE" "SELECT * FROM album_ratings WHERE rating IS NOT NULL;" > "$BACKUP_DIR/ratings_data.csv"

echo "Ratings data backed up to $BACKUP_DIR/"

