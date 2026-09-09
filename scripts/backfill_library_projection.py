#!/usr/bin/env python3
"""
backfill_library_projection.py
==============================
Populates the denormalised stable-field projection from `albums/{albumId}` onto:
  - every `users/{uid}/library_albums/{albumId}` document
  - every entry in `users/{uid}/collections/{name}.albums[]`

so the library and collection screens render without joining the shared `albums`
catalogue (see docs/DATA_MODEL.md, TECH_DEBT 2.7).

Copies these stable fields:
    name, primary_artist, artists, release_date, album_type, total_tracks,
    spotify_id, spotify_uri, image_url  (+ projection_fetched_at on library entries)

Idempotent: entries that already have a non-empty `name` are skipped unless --force.
Library entries are only touched when `in_library == true`.

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

    def projection_for(album_id: str) -> dict | None:
        album = album_doc(album_id)
        if album is None:
            return None
        p = {f: album.get(f) for f in PROJECTION_FIELDS if album.get(f) is not None}
        p["image_url"] = first_image_url(album)
        return p

    lib_updated = lib_skipped = missing = 0
    col_updated = col_skipped = 0

    # `users/{uid}` parent docs are often phantom (never explicitly written), so
    # `.stream()` skips them — `.list_documents()` returns their refs regardless.
    for user in db.collection("users").list_documents():
        # ── library_albums ──
        for entry in user.collection("library_albums").stream():
            data = entry.to_dict() or {}
            if not data.get("in_library", False):
                continue
            if data.get("name") and not args.force:
                lib_skipped += 1
                continue
            p = projection_for(entry.id)
            if p is None:
                missing += 1
                print(f"  ⚠  users/{user.id}/library_albums/{entry.id}: no albums/{entry.id}")
                continue
            p["projection_fetched_at"] = int(time.time() * 1000)
            print(f"  {'[dry] ' if dry else ''}library_albums/{entry.id} ← {p.get('name')!r}")
            if not dry:
                entry.reference.set(p, merge=True)
            lib_updated += 1

        # ── collections[].albums[] ──
        for coll in user.collection("collections").stream():
            entries = (coll.to_dict() or {}).get("albums") or []
            changed = False
            for e in entries:
                if e.get("name") and not args.force:
                    col_skipped += 1
                    continue
                p = projection_for(e.get("album_id", ""))
                if p is None:
                    missing += 1
                    continue
                e.update(p)
                changed = True
                col_updated += 1
                print(f"  {'[dry] ' if dry else ''}collections/{coll.id}[{e.get('album_id')}] ← {p.get('name')!r}")
            if changed and not dry:
                coll.reference.set({"albums": entries}, merge=True)

    print(f"\n{'DRY RUN — ' if dry else ''}library: {lib_updated} backfilled / {lib_skipped} skipped; "
          f"collections: {col_updated} backfilled / {col_skipped} skipped; {missing} missing an album doc.")
    if dry:
        print("Re-run with --execute to apply.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
