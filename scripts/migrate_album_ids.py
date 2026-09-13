#!/usr/bin/env python3
"""
migrate_album_ids.py
====================
Re-keys every album from the old 32-bit `String.hashCode()` document ID to the new
SHA-256(name|primary_artist)[:24] ID (see AlbumMapper.generateAlbumId / TECH_DEBT 2.2).

Rewrites all references to the album ID:

  1. albums/{id}                              — the document itself
  2. users/{uid}/library_albums/{id}          — rating / tags / in_library
  3. users/{uid}/collections/{name}.albums[]  — {albumId, position, addedAt} entries
  4. tracks/* where album_id == {id}          — the album_id field

Idempotent: albums already at their new ID are skipped, so it is safe to re-run.

The old_id -> new_id map is built from *every* place an old-scheme id and its own
name/primary_artist projection can be found (the shared `albums` catalogue, each
library_albums doc, each collection's embedded album entries) — not just the
catalogue — so it stays complete even if some of those documents were already
migrated in an earlier, partial run.

Requirements
------------
    pip install firebase-admin

Usage
-----
    # Preview (no writes) — always run this first:
    python3 scripts/migrate_album_ids.py --cred serviceAccountKey.json

    # Apply:
    python3 scripts/migrate_album_ids.py --cred serviceAccountKey.json --execute

The service account JSON comes from the Firebase console:
  Project settings → Service accounts → Generate new private key.
"""
import argparse
import hashlib
import sys

import firebase_admin
from firebase_admin import credentials, firestore


def new_album_id(name: str, primary_artist: str | None) -> str:
    """Must stay byte-for-byte identical to Kotlin AlbumMapper.generateAlbumId."""
    artist = (primary_artist or "Unknown Artist").strip().lower()
    key = f"{(name or '').strip().lower()}|{artist}"
    return hashlib.sha256(key.encode("utf-8")).hexdigest()[:24]


def build_remap(db) -> dict[str, str]:
    """
    Collects every old_id -> new_id pair we can compute, from every place an
    old-scheme id still carries its own name/primary_artist:

      - the shared `albums` catalogue
      - each user's `library_albums` docs (denormalised projection)
      - each user's `collections/*.albums[]` entries (denormalised projection)

    Using all three (not just the catalogue) matters once any one of them has
    already been migrated in an earlier run — its old id is gone from that source,
    but may still need remapping in the others.
    """
    remap: dict[str, str] = {}

    for doc in db.collection("albums").stream():
        data = doc.to_dict() or {}
        nid = new_album_id(data.get("name", ""), data.get("primary_artist"))
        if nid != doc.id:
            remap.setdefault(doc.id, nid)

    # list_documents(), not stream(): a `users/{uid}` doc is usually just a container
    # for subcollections with no fields of its own, so Firestore doesn't return it from
    # a collection query (stream()) — only list_documents() enumerates it regardless.
    for uref in db.collection("users").list_documents():
        for doc in uref.collection("library_albums").stream():
            data = doc.to_dict() or {}
            name, artist = data.get("name"), data.get("primary_artist")
            if not name or not artist:
                continue  # blank projection (pre-backfill / removed) — nothing to hash
            nid = new_album_id(name, artist)
            if nid != doc.id:
                remap.setdefault(doc.id, nid)

        for coll in uref.collection("collections").stream():
            for entry in (coll.to_dict() or {}).get("albums") or []:
                if not isinstance(entry, dict):
                    continue
                # Firestore field is snake_case (CollectionAlbumEntry's @SerialName) —
                # entry.get("albumId") was always None, so this loop, and the rewrite
                # loop below, silently did nothing on every previous run.
                aid = entry.get("album_id")
                name, artist = entry.get("name"), entry.get("primary_artist")
                if not aid or not name or not artist:
                    continue
                nid = new_album_id(name, artist)
                if nid != aid:
                    remap.setdefault(aid, nid)

    return remap


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--cred", required=True, help="Path to the Firebase service account JSON")
    ap.add_argument("--execute", action="store_true", help="Actually write changes (default: dry run)")
    args = ap.parse_args()

    dry = not args.execute
    firebase_admin.initialize_app(credentials.Certificate(args.cred))
    db = firestore.client()

    print(f"{'DRY RUN — ' if dry else ''}Scanning for ids that need re-keying…")
    remap = build_remap(db)

    print(f"  {len(remap)} album id(s) need re-keying.")
    if not remap:
        return 0

    # 1 — album documents
    for old_id, nid in remap.items():
        src = db.collection("albums").document(old_id).get()
        if not src.exists:
            continue
        if db.collection("albums").document(nid).get().exists:
            print(f"  albums/{nid} already exists — deleting stale albums/{old_id}")
            if not dry:
                db.collection("albums").document(old_id).delete()
            continue
        print(f"  albums/{old_id} -> albums/{nid}  ({(src.to_dict() or {}).get('name')})")
        if not dry:
            db.collection("albums").document(nid).set(src.to_dict())
            db.collection("albums").document(old_id).delete()

    # 2 + 3 — per-user library entries and collections
    for uref in db.collection("users").list_documents():
        user_id = uref.id

        for old_id, nid in remap.items():
            entry = uref.collection("library_albums").document(old_id).get()
            if entry.exists:
                print(f"  users/{user_id}/library_albums/{old_id} -> {nid}")
                if not dry:
                    uref.collection("library_albums").document(nid).set(entry.to_dict())
                    uref.collection("library_albums").document(old_id).delete()

        for coll in uref.collection("collections").stream():
            albums = (coll.to_dict() or {}).get("albums") or []
            changed = False
            for e in albums:
                if isinstance(e, dict) and e.get("album_id") in remap:
                    e["album_id"] = remap[e["album_id"]]
                    changed = True
            if changed:
                print(f"  users/{user_id}/collections/{coll.id}: remapped album ids")
                if not dry:
                    uref.collection("collections").document(coll.id).set({"albums": albums}, merge=True)

    # 4 — tracks.album_id
    track_updates = 0
    for track in db.collection("tracks").stream():
        aid = (track.to_dict() or {}).get("album_id")
        if aid in remap:
            track_updates += 1
            if not dry:
                db.collection("tracks").document(track.id).set({"album_id": remap[aid]}, merge=True)
    if track_updates:
        print(f"  {track_updates} track(s) had album_id remapped")

    print("\nDone." if not dry else "\nDry run complete — re-run with --execute to apply.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
