#!/usr/bin/env python3
"""
backfill_library_projection.py
==============================
Populates the denormalised projection on each `users/{uid}/library_albums/{albumId}`
document from the matching `albums/{albumId}` doc, so the library renders without joining
the shared `albums` catalogue (see docs/DATA_MODEL.md, TECH_DEBT 2.7).

Copies these stable fields onto the library entry:
    name, primary_artist, artists, release_date, album_type, total_tracks,
    spotify_id, spotify_uri, image_url, projection_fetched_at

Idempotent: entries that already have a non-empty `name` are skipped unless --force.
Only touches entries with `in_library == true`.

Run this immediately after deploying the projection change — until it runs, the app
falls back to the old join for un-backfilled entries (blank `name`).

Requirements
------------
    pip install firebase-admin

Usage
-----
    python3 scripts/backfill_library_projection.py --cred serviceAccountKey.json          # preview
    python3 scripts/backfill_library_projection.py --cred serviceAccountKey.json --execute # apply
    python3 scripts/backfill_library_projection.py --cred serviceAccountKey.json --execute --force  # re-copy all
"""
import argparse
import json
import sys
import time

import firebase_admin
from firebase_admin import credentials, firestore

PROJECTION_FIELDS = (
    "name", "primary_artist", "artists", "release_date", "album_type",
    "total_tracks", "spotify_id", "spotify_uri",
)


def first_image_url(album: dict) -> str | None:
    raw = album.get("images")
    if not raw:
        return None
    try:
        images = json.loads(raw) if isinstance(raw, str) else raw
        return images[0].get("url") if images else None
    except (ValueError, AttributeError, IndexError):
        return None


def main() -> int:
    ap = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    ap.add_argument("--cred", required=True, help="Firebase service account JSON")
    ap.add_argument("--execute", action="store_true", help="Write changes (default: dry run)")
    ap.add_argument("--force", action="store_true", help="Re-copy even entries that already have a projection")
    args = ap.parse_args()

    dry = not args.execute
    firebase_admin.initialize_app(credentials.Certificate(args.cred))
    db = firestore.client()

    # Cache the shared album docs so N users sharing an album cost one read.
    album_cache: dict[str, dict | None] = {}

    def album_doc(album_id: str) -> dict | None:
        if album_id not in album_cache:
            snap = db.collection("albums").document(album_id).get()
            album_cache[album_id] = snap.to_dict() if snap.exists else None
        return album_cache[album_id]

    updated = skipped = missing = 0
    for user in db.collection("users").stream():
        for entry in user.reference.collection("library_albums").stream():
            data = entry.to_dict() or {}
            if not data.get("in_library", False):
                continue
            if data.get("name") and not args.force:
                skipped += 1
                continue

            album = album_doc(entry.id)
            if album is None:
                missing += 1
                print(f"  ⚠  users/{user.id}/library_albums/{entry.id}: no albums/{entry.id} — skipped")
                continue

            projection = {f: album.get(f) for f in PROJECTION_FIELDS if album.get(f) is not None}
            projection["image_url"] = first_image_url(album)
            projection["projection_fetched_at"] = int(time.time() * 1000)

            print(f"  {'[dry] ' if dry else ''}users/{user.id}/library_albums/{entry.id} ← "
                  f"{projection.get('name')!r} / {projection.get('primary_artist')!r}")
            if not dry:
                entry.reference.set(projection, merge=True)
            updated += 1

    print(f"\n{'DRY RUN — ' if dry else ''}{updated} entries backfilled, "
          f"{skipped} already done, {missing} missing their album doc.")
    if dry:
        print("Re-run with --execute to apply.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
