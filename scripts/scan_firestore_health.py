#!/usr/bin/env python3
"""
scan_firestore_health.py — READ-ONLY.

Walks the Firestore project looking for documents the Kotlin app can't decode:
  * numbers that arrived as a nested object (a boxed Kotlin/JS Long that leaked
    through GitLive's JS encoder instead of being rejected)
  * nulls inside array fields (e.g. an empty `albums` that serialised as [null])
  * `collections/*.albums[]` entries missing keys the model needs
  * numeric fields whose type doesn't match what the model expects

Nothing is written. Prints a report with each suspect doc's path + last update time.

    python3 scripts/scan_firestore_health.py --cred serviceAccountKey.json
    python3 scripts/scan_firestore_health.py --cred serviceAccountKey.json --dump path/to/doc
"""
import argparse
import sys

import firebase_admin
from firebase_admin import credentials, firestore

# field -> python type(s) the Kotlin model can decode (int and float both OK for numbers)
COLLECTION_TOP = {"name": str, "description": (str, type(None)),
                  "created_at": (int, float), "updated_at": (int, float),
                  "parent_name": (str, type(None)), "albums": list}
COLLECTION_ENTRY = {"album_id": str, "position": (int, float),
                    "added_at": (int, float), "name": str, "primary_artist": str,
                    "artists": str, "release_date": str, "album_type": str,
                    "total_tracks": (int, float), "spotify_id": str,
                    "spotify_uri": str, "image_url": (str, type(None))}
TRACK_DOC = {"name": str, "album_id": str, "track_number": (int, float),
             "duration_ms": (int, float), "disc_number": (int, float),
             "spotify_uri": str, "artists": str, "primary_artist": str,
             "is_explicit": bool}
ALBUM_DOC = {"total_tracks": (int, float), "updated_at": (int, float, type(None))}
ARTIST_DOC = {"followers": (int, float), "popularity": (int, float)}

findings = []


def note(path, msg, updated=None):
    findings.append((path, msg, updated))


def looks_like_boxed_long(v):
    return isinstance(v, dict) and (
        {"low", "high"} <= set(v) or {"low_", "high_"} <= set(v)
        or "__isLong__" in v or ("data_" in v and "kind" not in v and len(v) <= 4)
    )


def deep_scan(path, value, updated):
    """Flag structural corruption anywhere in a document's value tree."""
    if looks_like_boxed_long(value):
        note(path, f"number stored as a nested object (boxed Long): {value!r}", updated)
        return
    if isinstance(value, dict):
        for k, v in value.items():
            deep_scan(f"{path}.{k}", v, updated)
    elif isinstance(value, list):
        for i, v in enumerate(value):
            if v is None:
                note(path, f"array has a null at index {i}", updated)
            else:
                deep_scan(f"{path}[{i}]", v, updated)


def check_shape(path, data, schema, updated):
    for field, types in schema.items():
        if field not in data:
            continue  # missing optional-with-default is fine for the model
        v = data[field]
        if not isinstance(v, types):
            note(path, f"`{field}` is {type(v).__name__} = {v!r}, expected {types}", updated)


def scan_collection(coll, schema, entry_schema=None, label=None):
    label = label or coll.id
    n = 0
    for doc in coll.stream():
        n += 1
        data = doc.to_dict() or {}
        upd = getattr(doc, "update_time", None)
        p = doc.reference.path
        deep_scan(p, data, upd)
        check_shape(p, data, schema, upd)
        if entry_schema and isinstance(data.get("albums"), list):
            for i, entry in enumerate(data["albums"]):
                if isinstance(entry, dict):
                    check_shape(f"{p}.albums[{i}]", entry, entry_schema, upd)
    print(f"  scanned {n:>5}  {label}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--cred", default="serviceAccountKey.json")
    ap.add_argument("--dump", help="print one document by full path and exit")
    ap.add_argument("--repair-null-albums", action="store_true",
                    help="rewrite any collection whose `albums` array contains nulls, "
                         "dropping the null entries (WRITES)")
    args = ap.parse_args()

    firebase_admin.initialize_app(credentials.Certificate(args.cred))
    db = firestore.client()

    if args.dump:
        snap = db.document(args.dump).get()
        import json
        print(json.dumps(snap.to_dict(), indent=2, default=str))
        print("\nupdate_time:", getattr(snap, "update_time", None))
        return

    if args.repair_null_albums:
        fixed = 0
        for user in db.collection("users").list_documents():
            for doc in user.collection("collections").stream():
                data = doc.to_dict() or {}
                albums = data.get("albums")
                if isinstance(albums, list) and any(a is None for a in albums):
                    clean = [a for a in albums if a is not None]
                    print(f"  {doc.reference.path}: {len(albums)} -> {len(clean)} entries")
                    doc.reference.update({"albums": clean})
                    fixed += 1
        print(f"\nrepaired {fixed} collection(s)." if fixed else "nothing to repair.")
        return

    print("scanning (read-only)…\n")
    print("top-level catalogue:")
    scan_collection(db.collection("albums"), ALBUM_DOC)
    scan_collection(db.collection("artists"), ARTIST_DOC)
    scan_collection(db.collection("tracks"), TRACK_DOC)

    print("\nper-user:")
    for user in db.collection("users").list_documents():
        print(f" user {user.id}:")
        scan_collection(user.collection("collections"), COLLECTION_TOP,
                        entry_schema=COLLECTION_ENTRY, label="collections")
        scan_collection(user.collection("library_albums"),
                        {"total_tracks": (int, float), "in_library": bool},
                        label="library_albums")

    print("\n" + "=" * 70)
    if not findings:
        print("No structural problems found.")
        return
    findings.sort(key=lambda f: (str(f[2]) or "", f[0]))
    print(f"{len(findings)} suspect field(s):\n")
    for path, msg, updated in findings:
        when = f"  (updated {updated})" if updated else ""
        print(f"  {path}\n    {msg}{when}\n")


if __name__ == "__main__":
    sys.exit(main())
