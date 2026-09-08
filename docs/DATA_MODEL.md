# Firestore Data Model & Spotify Caching

`docs/ARCHITECTURE.md` still documents the old SQLDelight schema. This is the actual
Firestore layout, plus an evaluation of the "mirror Spotify metadata into Firestore"
approach.

---

## Layout

Two tiers: a **shared catalogue** and **per-user subtrees**.

### Shared / global — one copy, every client reads *and writes* it

| Collection | Doc key | Contents | Written by |
|---|---|---|---|
| `albums/{albumId}` | `sha256(name\|artist).take(24)` * | name, `primary_artist`, `artists` (JSON), `release_date`, `total_tracks`, `spotify_uri`, `spotify_id`, `album_type`, `images` (JSON of URLs), `external_ids` (JSON), `release_group_id`, `added_at`, `updated_at` | any client that fetches an album from Spotify (library sync, playlist import, release-group swap) |
| `artists/{artistId}` | Spotify artist id | `name`, `genres[]`, `images` (JSON), `followers`, `popularity`, `href`, `uri` | `AlbumProcessingHandler` on `AlbumAdded`; library sync `batchArtistsThenSaveLocalAlbums` |
| `tracks/{trackId}` | Spotify track id | `name`, `album_id`, `track_number`, `disc_number`, `duration_ms`, `artists` (JSON), `primary_artist`, `preview_url`, `is_explicit`, **`is_saved`** | first time an album detail view is opened (`checkAndUpdateTracksIfNeeded`); "save track" actions |

\* the SHA-256 scheme is in code (TECH_DEBT 2.2) but the re-key migration hasn't run, so
the live DB is still keyed by the old 32-bit `String.hashCode()`.

### Per-user — `users/{userId}/…`

`userId` is the **Spotify user id** (`GET /me` → `profile.id`), **not** the Firebase uid.
Resolved once by `LibraryService.initUserSession()` → `UserSessionRepository.setUserId()`,
persisted to local Settings, then read via `awaitUserId()` (writes) / `userIdFlow` (reads).

| Sub-collection | Doc | Contents |
|---|---|---|
| `library_albums/{albumId}` | album id | `{ in_library, rating, tag_ids[], added_at }` — the join table. "My library" = `library_albums where in_library`, joined against the shared `albums`. |
| `tags/{tagId}` | tag id | `{ tag_key, tag_value, tag_type }` |
| `collections/{collectionName}` | collection name | `{ name, description, parent_name, created_at, updated_at, albums: [{album_id, position, added_at}] }` — collection membership is an **embedded array** referencing shared `albums` |
| `collection_folders/{folderName}` | folder name | `{ folder_name, parent }` |

### Not in Firestore — device-local (`multiplatform-settings`)

Spotify tokens, OpenAI key, **all settings** (`defaultSortOrder`, `collectionAddToLibrary`,
theme, …), saved filter state, cached `spotify_user_id`. So preferences **do not sync**
across devices/web.

### Not stored anywhere as bytes

`images` fields are JSON **URLs**. Image bytes are fetched from Spotify's CDN and cached
by Coil's own disk/memory cache — no blob storage in Firestore.

---

## Multi-user status

Structured for multi-user (per-user subtrees), **not secured** for it — see TECH_DEBT 2.1:

- Rules are the stopgap `if request.auth != null`; the client signs in **anonymously**.
- Rules **never check** the path's `{userId}` against the caller → any signed-in client
  can read/write **any** `users/{spotifyId}/…` subtree.
- Shared `albums`/`artists`/`tracks` are **world-writable** by any signed-in client — no
  ownership, no validation.
- **`tracks.is_saved` leaks across users** (TECH_DEBT 2.6) — it's on the global doc, so
  every user shares one "liked songs" set.

---

## Evaluation — mirroring Spotify metadata into Firestore

**The goal is right.** Browsing / sorting / filtering a library must not hit the Spotify
API (rolling ~30 s rate-limit window; a sort of 500 albums would be hundreds of calls).
Some persistent cache is necessary.

**The framing to correct: storage cost is a non-issue.** The catalogue is a *shared*,
deduped set (union of all users' libraries), not per-user copies, and Spotify metadata is
small text + a few URLs:

| | per doc (incl. Firestore overhead) | 2 500 albums / 30 k tracks / 1 500 artists |
|---|---|---|
| album | ~1 KB | 2.5 MB |
| track | ~0.4 KB | 12 MB |
| artist | ~0.5 KB | 0.75 MB |
| **total** | | **~15 MB → ~$0.003/month** |

Even at 1 000 users / 50 k albums / 600 k tracks it's ~250 MB → ~$0.05/month. Storage
will never be the problem.

**The actual problems, ranked:**

### 1. Spotify Developer Terms (highest risk)

The Developer Terms restrict caching Spotify Content: broadly, cache only for performance,
**refresh within ~24 h**, delete on request, and **don't create a standalone
dataset/database** of Spotify content. A full, **shared, indefinitely-retained** mirror
across many users is squarely in the grey/red zone — worth a proper read of the current
terms before any public launch. The current code has **no `fetched_at` and never
refreshes** metadata (`checkAndUpdateTracksIfNeeded` only fetches if tracks are *absent*;
`saveAlbumIfNotPresent` skips existing).

### 2. Read cost, not storage

The library list uses **realtime `.snapshots`**, not one-shot `.get()`:

- `getAllLibraryEntries()` → a listener on the whole `users/{uid}/library_albums`
- `getAlbumsByIds()` → `ceil(N/30)` listeners on `albums`
- `SharingStarted.WhileSubscribed(5000)` detaches after 5 s off-screen and **re-attaches
  = full re-read** on return
- `LibraryViewModel` is instantiated more than once (nav panel + screens — TECH_DEBT 1.2
  follow-up), multiplying the listener sets

So a 500-album library ≈ ~1 000–1 500 reads to open, again on every navigation
round-trip > 5 s. Fine on the free tier for one user; this is the cost that grows with
usage, and it's exactly what the cache was meant to avoid — it just moved the cost from
Spotify rate limits to Firestore reads. Browse data does **not** need to be realtime.

### 3. Staleness / correctness

No refresh means: `artist.popularity` is stale immediately; images can 404 over time;
Spotify corrects album/artist names and re-releases albums; `total_tracks` can change.
Most album metadata *is* stable, so this is low-severity — but it compounds the ToS point
(a 24 h refresh fixes both).

### 4. Write amplification

Adding one album to the library fans out: `saveAlbum` (1 write) → `AlbumAdded` event →
fetch + `saveArtist` (1 read + 1 write) → generate tags → `arrayUnion` + `insertTag` per
tag; first detail view → ~12 track writes. A 100-album sync is hundreds of writes on top
of the Spotify calls it was avoiding.

### 5. Shared mutable catalogue = multi-user coupling

One user's stale or malformed write to `albums`/`artists`/`tracks` is visible to
everyone. This is the same root issue as TECH_DEBT 2.1 — the shared catalogue is the
thing that makes "isolate the users" hard.

---

## Options

| | Approach | Fixes | Costs |
|---|---|---|---|
| **A** | **Status quo** — full shared mirror, no TTL | — | ToS exposure, stale data, read cost, coupling |
| **B** | **Add `fetched_at` + refresh-if-stale (24 h)** to A | ToS, staleness | a bit more API traffic; small effort |
| **C** | **Stop persisting `tracks`** — fetch per album-detail view into a memory / on-device cache | ~80 % of stored volume + the biggest ToS liability; detail view already calls Spotify | detail view slightly slower offline |
| **D** | **Cache the browse *projection* only** in Firestore (name, primary_artist, release_date, one image URL, album_type) — full detail on demand | reduces stored surface ~60 %, less ToS surface | second fetch path for detail |
| **E** | **Library list off realtime** — one-shot `.get()` + a local reactive cache, or a single app-scoped listener | read cost (the money leak) | lose live cross-device updates to the list (acceptable) |
| **F** | **Per-user cache** — `users/{uid}/album_cache/…` instead of shared collections | unambiguously a "client cache" under ToS; kills the shared-mutation problem | loses dedup + shared release-group data; more storage (still cheap) |
| **G** | **Backend proxy** — Cloud Function caches Spotify responses with a TTL; one rate-limit budget for all users | ToS (server-side cache with refresh is the intended pattern), coupling, rate limits | real infrastructure — this is roadmap Phase 4 |

---

## Chosen design — denormalised per-user library + short-TTL cache

Combine **D + E + F** (with **C** for tracks): the per-user join table carries a small
projection of stable fields so the library renders from one read, and everything volatile
is a short-lived cache refreshed per Spotify's terms.

**Status — v1 landed** (`getAllAlbumsInLibrary` reads the projection; one `library_albums`
listener, no `albums` fan-out):
- `LibraryAlbumDocument` carries the projection; `AlbumMapper.toLibraryProjection` /
  `libraryProjectionToDomain` map it. Written on `addToLibrary` and on sync.
- **Migration**: `scripts/backfill_library_projection.py` copies the projection onto
  existing `library_albums` entries. Run it right after deploying — until then the app
  falls back to the old `albums` join for un-backfilled entries (blank `name`), so nothing
  breaks, but the read-cost win only lands once the backfill runs.
- **Still v2**: the volatile-metadata TTL cache (genres / popularity / full images /
  tracklist still come from the shared `albums`/`artists`/`tracks` collections with no
  TTL); dropping `tracks` as a permanent collection; collections still join `albums`.

### `users/{uid}/library_albums/{albumId}`

```
// ── user's own data (already here) ──
in_library, rating, tag_ids[], added_at

// ── denormalised projection: stable fields, written on "add to library" ──
name                 // Spotify re-titles rarely; refreshed on sync / 404
primary_artist
artists              // JSON, for multi-artist display
release_date
album_type
total_tracks
spotify_id
spotify_uri
image_url            // one medium URL; art rots — see refresh triggers

// ── freshness ──
projection_fetched_at // epoch millis; drives the opportunistic refresh
```

~15 fields, ~1 KB/doc. The library grid renders entirely from this collection: **one
listener on `users/{uid}/library_albums`, no join, no fan-out.** Sorting, filtering and
search-within-library are client-side on loaded data.

**Refresh triggers for the projection** (no dedicated loop — piggyback on work that
already happens):
- library sync re-writes the projection for every album it touches;
- an image load failure → re-fetch that one album's metadata and update `image_url`;
- (optional) a lazy "older than 30 d" check on scroll-into-view.

### Volatile metadata → short-TTL cache (**not** a permanent Firestore collection)

Genres, popularity, full image set, `external_ids`, and the **full tracklist** are fetched
from Spotify on demand (album-detail view, artist-detail view) into a cache with a
`fetched_at` and a **~24 h TTL**, per the Developer Terms.

- **v1**: in-memory cache (a `Map` in a repository), lost on app restart — simplest, and
  matches the "album detail hits Spotify anyway" reality.
- **v2**: on-device persistence (SQLDelight / DataStore; IndexedDB on web). Client-side
  caching is explicitly fine under the terms.
- The shared `albums` / `artists` collections are **demoted to an optional cache** — only
  the release-group cross-album feature still reads them, and a stale/bad write there
  self-heals on next refresh instead of corrupting the library view.
- `tracks` as a permanent Firestore collection is **removed** (C).

### Collections

`collections/{name}.albums[]` currently joins against shared `albums`. Resolve collection
entries against the local `library_albums` cache instead; for the "in a collection but not
in the library" case, denormalise the same minimal projection into the collection entry.

### What this fixes

| Concern | Effect |
|---|---|
| Read cost (#2) | library grid = 1 collection read, no `ceil(N/30)` fan-out; big latency + offline-first win too |
| Spotify ToS (#1) | stored-forever data is now minimal, per-user, and written as a side effect of the user's own action; volatile data is TTL'd |
| Staleness (#3) | only stable fields are stored; volatile fields refresh |
| Coupling (#5) | shared catalogue demoted to a self-healing cache; the per-user table is exactly where 2.1's isolation rules apply |
| Write amplification (#4) | roughly neutral — one richer doc instead of a thin doc + a shared-collection write |

### Open decisions

1. Where the volatile cache lives long-term (in-memory v1 → on-device v2 → backend).
2. Whether to keep a thin shared `albums` at all, or fold release-group data elsewhere.

### Strategic

When roadmap Phase 4 (backend) lands, move Spotify fetching + caching behind a Cloud
Function (**G**) — one rate-limit budget for all users, server-side TTL cache (the
intended pattern under the terms), and it also solves the shared-write isolation problem.

---

## Observability — measure before and after

Before migrating anything, land instrumentation so the change is measurable. Track, and
**attribute to the screen / background source that caused it**:

**Firestore**
- reads (documents returned), by collection and by source
- writes / deletes, by collection and by source
- realtime listener attaches (each `.snapshots` subscription = an initial full read)

**Spotify Web API**
- requests, by endpoint group (`/albums`, `/me/player`, `/search`, …) and by source
- responses by status; **429 count**, cumulative `Retry-After` seconds ("time throttled"),
  and the running `X-RateLimit-Remaining` low-water mark

**Sources** include screens (`LibraryScreen`, `AlbumScreen`, …) *and* background workers
(`PlaybackPoller` — polls `/me/player` every 1.5–8 s, `LibrarySync`, `startup`,
`AlbumProcessingHandler`).

Output: a periodic (and on-demand) formatted report — per-source breakdown for both
Firestore and Spotify — logged under a `Traffic` category so it lands in the file antilogs
on desktop. Implemented in `util/TrafficMetrics.kt`; see `AGENTS.md` for how sources are
tagged.
