# Record Collection — Tech Debt & Remediation Plan

**Status**: Sections 1–4 complete. Remaining work is Section 5 hygiene, the deferred
security follow-ups in Section 2, and a handful of "left as-is" notes on otherwise-done
items.

**Scope**: Concrete, file-level fixes derived from a full codebase review. This is the
practical companion to [PRODUCTION_ROADMAP.md](PRODUCTION_ROADMAP.md). When the two
disagree, this document is the source of truth for *what is actually wrong today*.

---

## How to use this document

- Each item has a checkbox, a short description of **what was done** (with the commit),
  or — for open items — **why it's wrong**, **where**, and **the fix**.
- Don't add new code that reintroduces a fixed pattern — the anti-patterns are also
  listed in `AGENTS.md` § Anti-Patterns.

---

## Section 1 — Bad Patterns  *(complete)*

### 1.1 — `runBlocking` in repositories (blocks the UI thread)

- [x] **DONE** (`dd5bc34`) — `TagRepository` + `AlbumTagRepository` write methods
  (`insertTag`/`deleteTag`/`updateTag`, `addTagToAlbum`/`removeTagFromAlbum`) are now
  `suspend`; the `runBlocking { }` wrappers are gone. `TagService` and
  `AlbumProcessingHandler.processAlbumTags` propagate `suspend`; `AlbumViewModel` wraps
  the calls in `viewModelScope.launch`. Tests moved to `coEvery`/`coVerify`.

### 1.2 — ViewModels created with `remember { }` instead of a ViewModel factory

- [x] **DONE** (`09eab02`) — `rememberXxxViewModel` now use `viewModel { }` against a
  `LocalViewModelStoreOwner`, so `onCleared()` fires (cancelling `viewModelScope` +
  Firestore listeners) when a screen is popped.
  - `navigation/ScreenViewModelStores.kt` — one `ViewModelStore` per back-stack `Screen`.
    Both navigators hold one and call `retainOnly(backStack + current)` after every
    `navigate()`, which `.clear()`s the stores of popped screens.
    `Navigator.viewModelStoreOwnerFor(screen)` exposes it.
  - `NavigationHost` (both actuals) wraps `content(screen)` in
    `CompositionLocalProvider(LocalViewModelStoreOwner provides …)`.
  - `App.kt` provides an app-session `ViewModelStoreOwner` (cleared in a
    `DisposableEffect onDispose`) for the always-on VMs (playback / search / settings /
    auth) and for components rendered outside `NavigationHost` (the desktop nav panel).
  - `ViewModelFactoryExtensions.kt` rewritten to `viewModel { … }` — except
    `rememberSettingsViewModel`, which returns the DI-container singleton (it backs the
    theme, process lifetime).
  - `lifecycle-viewmodel-compose` added to the catalog + `commonMain`.
  - Verified by `ScreenViewModelStoresTest` + `DesktopNavigatorTest`.
    `AlbumDetailViewModel` / `ArtistDetailViewModel` keep a one-line `onCleared` log for
    leak-spotting.
- **Known follow-ups** (pre-existing multiplicity now just visible — not regressions):
  - `LibraryViewModel` / `CollectionsViewModel` / `AlbumViewModel` are `rememberXxx()`'d
    from several screens *and* the always-on nav panel → multiple live instances, each
    with its own Firestore listeners. They are arguably app-scoped singletons; consider
    hoisting them into the DI container like `settingsViewModel`.
  - Desktop `PopUpTo` trims the back stack but doesn't move `currentScreen` (Android's
    does) — `popUpTo` to a screen you're "below" is a visual no-op.

### 1.3 — Inconsistent error handling (throw vs `Result` vs null vs silent)

- [x] **DONE** — done in three slices. The *harm* (crashes from unhandled
  `viewModelScope.launch`, silently swallowed failures, `runCatching` eating
  `CancellationException`) is fixed everywhere. Converting every remaining throwing
  repo/service method to `Result<T>` is now opportunistic (their ViewModel callers all
  catch).
  - **Slice 1 — the sync path** (`9ab5512`):
    - `util/ResultExt.kt` — `resultOf { }` (a `runCatching` that re-throws
      `CancellationException`), `List<Result<T>>.aggregate()`, `AggregateException`.
    - `ProfileRepository.add/removeAlbumsFromSpotifyLibrary` → `Result<Unit>`, every
      chunk attempted, partial failure reported not swallowed (**4.7**).
    - `LibraryService.applySync` / `addAlbumToLibrary` / `removeAlbumFromLibrary`
      `.getOrThrow()` those results so a failed Spotify write propagates.
    - `LibraryViewModel.launchSync` / `startTrackSync` wrapped → `SyncState.Error`
      (a failed sync is now visible, not silent).
    - `LoginViewModel` surfaces `AuthState.Error` in its UI state (**4.8**).
    - Tests: `ResultExtTest`, `ProfileRepositoryTest`, a `LibraryServiceTest` case.
  - **Slice 2 — `AlbumRepository` fetch/API operations** (`85cfb70`):
    - `fetchAlbum(id)` → `Result<Album>` (was `Album?` that *threw* on API error). Built
      on `spotifyApi.library.getAlbum(...).map { toDomain(it) }`; internal→Spotify id
      lookup extracted to `resolveSpotifyId()`.
    - `fetchMultipleAlbums` — `runCatching` → `resultOf`.
    - `fetchReleaseGroupId(album)` → `Result.failure(IllegalArgumentException)` instead
      of `throw` when the album has no UPC.
    - `GetAlbumDetailUseCase.getApiAlbumData` uses `.getOrElse { … }`;
      `ReleaseGroupUseCase.getReleaseFromAlbum` uses `.firstOrNull()` (was `.first()` —
      `NoSuchElementException` on an empty release list).
  - **Slice 3 — the ViewModel layer + convention** (`28464bd`):
    - `viewmodel/ViewModelExt.kt` — `ViewModel.launchSafely(operation, onError) { }`:
      `viewModelScope.launch` with a uniform policy (cancellation propagates, everything
      else is logged and passed to `onError`, never escapes).
    - Every bare `viewModelScope.launch { }` command across **all** ViewModels converted
      (~55 call sites): `LibraryViewModel`, `AlbumViewModel`, `SettingsViewModel` (via an
      `edit()` helper), `CollectionsViewModel`, `CollectionDetailViewModel`,
      `CollectionImportViewModel`, `AuthViewModel`, `SearchViewModel`,
      `AlbumDetailViewModel`, `ArtistDetailViewModel`.
    - `PlaybackViewModel.executeWithLoading` / `executePlaybackAction`, and the remaining
      `catch (e: Exception)` blocks, re-throw `CancellationException`.
    - `AGENTS.md` § Anti-Patterns + § Conventions updated.
- **Left**: `LibraryService` / `CollectionsService` / `TrackRepository` internals still
  `throw` — a documented contract (the ViewModel boundary catches). Convert to `Result`
  opportunistically.

### 1.4 — `throw` inside `Flow` operators

- [x] **DONE** (`dd5bc34`) — `AlbumRepository.getAlbumById` returns `Flow<Album?>` and
  emits `null` (with a warning log) instead of throwing on a missing/undeserializable
  doc. `GetAlbumDetailUseCase.getDatabaseAlbum` applies `.filterNotNull()` before the
  combine.

### 1.5 — `System.currentTimeMillis()` / `System.getenv` in `commonMain`

- [x] **DONE** (`dd5bc34`) — all 6 real occurrences swapped to
  `Clock.System.now().toEpochMilliseconds()` (`SpotifyAuthRepository`, `AlbumRepository`
  ×2, `LoggingUtils` ×2). The `addedAt` (ISO string) vs `updatedAt` (epoch millis) type
  split on the album document still exists — deliberate for now (see 4.1).

### 1.6 — Default-package (no `package`) top-level declarations

- [x] **DONE** (`dd5bc34`) — `ArtistDetailViewModel` → `…viewmodel`; `PlaybackQueueService`
  (+ its trigger consts) → `…service`; `ArtistDetailScreen` → `…ui.screens`;
  `Playlist` / `AlbumResult` / `PlaylistAlbumExtractor` → `…network.spotify`. All import
  sites updated. Two dead default-package files deleted: `migration/DatabaseMigrationUtil.kt`
  and `desktopMain/kotlin/test.kt`.

### 1.7 — Package name ≠ directory

- [x] **DONE** (`dd5bc34`) — `AlbumTagRepository` moved from `…db.repository` to
  `…repository` (12 sites); `CollectionDetailViewModel` from `…ui.collection` to
  `…viewmodel` (3 sites).

### 1.8 — Ad-hoc `CoroutineScope`s that are never cancelled

- [x] **DONE** (`dd5bc34`) — `SettingsRepository` no longer spawns a background scope
  (`Settings` is synchronous; initial state is read eagerly in the property initializer).
  `ModularDependencyContainer.close()` now calls `eventScope.cancel()`.

### 1.9 — Debug noise left in production code paths

- [x] **DONE** (`dd5bc34`, `cea7054`) — `ProductionNetworkModule` generic client:
  `println` / `printStackTrace()` → `LoggingUtils` under `Category.NETWORK`. Stray
  `println(album)` removed from `SearchResultComponents`. `DatabaseMigrationUtil` deleted.
  `util/Logger.kt` (unused custom `Antilog` with a `println`; `Logger.initialize` had
  zero callers) deleted.

### 1.10 — `libs.versions.toml` is not actually the single source of truth

- [x] **DONE** (`6dd4c89`) — every dependency in `composeApp/build.gradle.kts` resolves
  via `libs.*`; no version string literals remain. Reconciled:
  - **ktor** → single `ktor = "3.1.0"` (was a split classpath: `client-core`/`client-auth`
    at 3.1.0, everything else resolving to 3.0.3).
  - **coil** → single `coil = "3.2.0"` (`coil-network-okhttp` was pinned at
    `3.0.0-alpha07` while `coil-core` had been bumped to 3.2.0 — a latent mismatch).
    Guarded by `CoilNetworkSmokeTest` (`5f78c90`).
  - **kotlinx-serialization-json** → `1.9.0` (declaration now matches what it resolved to).
  - **coroutines test** → uses the `kotlinx-coroutines` version (`1.10.2`); dead
    `coroutines = "1.7.3"` entry removed.
  - Pruned 8 unused KMP-template catalog entries.
- **Left**: the explicit `skiko-awt-runtime-macos-arm64` desktop dep is arch-locked and
  probably redundant against `compose.desktop.currentOs` — flagged in the catalog, not
  removed.

### 1.11 — Unused dependencies

- [x] **DONE** (`dd5bc34`) — `io.insert-koin:koin-core` and `koin-compose` removed
  (zero Koin references in `*.kt`).

### 1.12 — The Android target does not compile (pre-existing)

- [x] **DONE** (`d10d724`) — `./gradlew :composeApp:compileDebugKotlinAndroid` passes and
  the CI `android` job is green. Two causes fixed:
  1. Added `androidx.browser:browser:1.8.0` (catalog + `androidMain`) —
     `AndroidAuthHandler` uses `CustomTabsIntent`, which wasn't on the classpath.
  2. Raised `androidTarget` + `jvm("desktop")` Kotlin `jvmTarget` and the Android
     `compileOptions` to `JVM_17` (was 11). The Kotlin 2.1.21 stdlib inline functions are
     compiled for target 17, so JVM 11 failed with *"Cannot inline bytecode built with
     JVM target 17…"*. `minSdk 24` + AGP 8.9 desugaring make 17 safe.

---

## Section 2 — Critical (security & data integrity)

### 2.1 — Firestore authentication

- [x] **DONE — anonymous-auth stopgap** (`07ab6ab`). Require *any* authenticated client
  now; full per-user custom-token auth deferred.
  - `dev.gitlive:firebase-auth:2.3.0` in `commonMain` (matches `firebase-firestore:2.3.0`).
  - `db/FirebaseAuth.kt` → `ensureAnonymousAuth()`; called (blocking, 15 s timeout) at
    startup from both `DependencyContainerFactory` (desktop) and the Android factory,
    after Firebase init, before any repository runs.
  - `firestore.rules` + `firebase.json` at repo root:
    `match /{document=**} { allow read, write: if request.auth != null; }`
  - **Manual steps**: Anonymous sign-in is enabled in the Firebase console. Still need
    `firebase deploy --only firestore:rules` (or paste the rules in the console) if not
    already done.
- [ ] **NEXT UP — real per-user auth + isolation.** The anonymous UID gives **no user
  isolation**: rules never check the path's `{uid}` against `request.auth.uid`, so any
  signed-in client can read/write any `users/{spotifyId}/…` subtree, and the shared
  `albums`/`artists`/`tracks` collections are world-writable. Web makes this internet-wide
  (public Firebase config), so this must land before/with the web target. Planned work:
  1. Cloud Function verifies a Spotify access token and mints a Firebase **custom token**
     with `uid == spotifyUserId`; client calls `signInWithCustomToken`.
  2. Replace the catch-all rule with `match /users/{uid}/{doc=**} { allow read, write:
     if request.auth.uid == uid }` (sketched, commented, in `firestore.rules`).
  3. Fix **2.6** (`tracks.is_saved` leaks across users) as part of the same pass.
  4. Decide the shared-catalogue write policy — direct client writes with field-validation
     rules, or move catalogue writes behind a Cloud Function.
  5. (Optional) sync `users/{uid}/settings` so sort order / filters / theme follow the
     user across devices + web.
  - See `docs/DATA_MODEL.md` for the current Firestore layout this changes.

### 2.2 — Album document IDs were a 32-bit `String.hashCode()`

- [x] **DONE (code)** (`07ab6ab`) — `generateAlbumId(name, artist)` returns
  `sha256Hex("<name>|<artist>".normalized()).take(24)` (96 bits — collision-safe).
  `util/Hashing.kt` `expect fun sha256Hex` + JVM `actual`s (`java.security.MessageDigest`).
  `GenerateAlbumIdTest` locks the contract, cross-checked against `openssl` / the Python
  script.
- [x] **Migration script** — `scripts/migrate_album_ids.py` (firebase-admin, `--dry-run`
  default, `--execute` to apply). Re-keys `albums/{id}`,
  `users/{uid}/library_albums/{id}`, `users/{uid}/collections/*.albums[].albumId`, and
  `tracks.*.album_id`. Idempotent. Normalization kept byte-for-byte in sync with Kotlin.
- [ ] **Not yet run** — run it once against Firestore
  (`python3 scripts/migrate_album_ids.py --cred key.json` to preview, then `--execute`).
  Until then, existing albums keep their old IDs and won't match new writes.
- **Identity model** (deliberate): `id = f(name, primary_artist)`; `spotifyId` is a
  separate field. name+artist collapses remasters / deluxe editions on purpose
  (release-group swapping relies on it). Should be spelled out in `ARCHITECTURE.md`.

### 2.3 — Query logs committed to git

- [x] **DONE (working tree)** (`efb4bb6`) — `composeApp/logs/` + `*.db` in `.gitignore`;
  `git rm --cached composeApp/logs`. Logs stay on disk, untracked.
- [ ] **Still to do — history rewrite** (destructive, needs explicit go-ahead): the big
  blobs are still in history — `composeApp/logs/spotify_queries.log` ≈ **19.7 MB**,
  `firebase_queries.log` ≈ 1.1 MB, and several `composeApp/record_collection*.db`
  (1–1.4 MB each, contain real pre-Firebase album/rating data). Purge with
  `git filter-repo --path composeApp/logs --path-glob 'composeApp/*.db' --invert-paths`
  then force-push. Solo repo → safe, but rewrites every later SHA.
- [ ] **Optional**: point the file antilogs outside the repo, or cap size / rotate.

### 2.4 — Secrets committed / hardcoded

- [x] **DONE (working tree)** (`efb4bb6`) — `composeApp/google-services.json` +
  `composeApp/firebase.properties` in `.gitignore`; `git rm --cached
  composeApp/google-services.json`. `FirebaseDriver.desktop.kt` reads the Firebase config
  at runtime from `google-services.json` (`GOOGLE_SERVICES_JSON` env var →
  `composeApp/google-services.json` → cwd) instead of hard-coding it.
- [ ] **Still to do — history rewrite**: the old `google-services.json` blob is still in
  history (bundle it into the 2.3 filter-repo run). Firebase config is not a true secret
  (security is rules + auth), so low urgency.

### 2.5 — Secrets stored at rest in plaintext

- [ ] **Why**: Spotify access/refresh tokens (`SpotifyAuthRepository`) and the OpenAI API
  key (`SettingsRepository`) are stored via `multiplatform-settings` (Java `Preferences` /
  `SharedPreferences`) unencrypted.
- **Fix**: Android → `EncryptedSharedPreferences` (or DataStore + Tink). Desktop → OS
  keychain (JNA to `secret-service`/Keychain/Credential Manager). At minimum, encrypt
  with a key derived from a machine secret.

### 2.6 — "Saved tracks" is not user-scoped

- [ ] **Why**: `is_saved` is written onto the **global** `tracks/{trackId}` document and
  `getSavedTracks()` does `tracks.where(is_saved == true)`. There is no per-user scoping —
  every user shares one "liked songs" set. (`library_albums` is correctly per-user; this
  isn't.)
- **Fix**: move to `users/{uid}/saved_tracks/{trackId}` — a per-user join like
  `library_albums`. Do it with the 2.1 auth pass.
- **Note**: since the tracklist mirror was removed (2.7), `tracks/{id}` docs written by
  `addTrackToLibrary` carry only `{is_saved:true}` (no track body). The album-tracklist
  heart indicator no longer depends on that — it comes from `GET /me/tracks/contains`
  (`TrackRepository.markSavedStatus`) — so this item is now purely a data-model cleanup
  (drop the global `tracks` liked-store, move to `users/{uid}/saved_tracks/`).

### 2.7 — Spotify metadata cache: no TTL, ToS exposure

- [x] **v1 + collections landed + backfilled** — the library list *and* collection views
  render from a denormalised stable-field projection (on `users/{uid}/library_albums` and
  on each `collections/{name}.albums[]` entry) — one `library_albums` listener, no
  `albums` fan-out. See `docs/DATA_MODEL.md`. `scripts/backfill_library_projection.py`
  **run 2026-09-09** (460 library + 679 collection entries); a cold library open now
  shows **zero** `getAlbumsByIds` reads (was ~16k `albums` doc reads/session).
- [~] **tracklist cache landed** — `tracks` is no longer a permanent Firestore mirror of
  album tracklists. `TrackRepository.getAlbumTracks(album)` fetches from Spotify into an
  in-memory 24 h-TTL `Map` (`TRACKLIST_TTL`), dropped on restart. Album detail, the
  play-queue builder and "save all album songs" read through it.
- [ ] **v2 remaining**: `albums`/`artists` are still a shared, indefinitely-retained
  mirror with no `fetched_at`. Add a ~24 h TTL + refresh; and/or move the volatile cache
  to `users/{uid}/…` or a backend proxy. `tracks` still exists only as the (broken, 2.6)
  global liked-songs store. Traffic before/after is measurable via `util/TrafficMetrics`
  (`Traffic` log tag).

### 2.8 — Playback poller runs unconditionally

- [~] **Idle back-off landed** — `PlaybackPoller` now ramps its delay
  (`PLAYBACK_IDLE_BACKOFF_STEPS` = 8 s → 20 s → 45 s → 60 s) on each consecutive poll that
  sees nothing playing, resetting to the active rate the moment playback resumes. App left
  open with nothing playing settled to ~3 `/me/player` req/min (was ~30–40), confirmed via
  `TrafficMetrics`. `+PlaybackPollerTest`.
- [~] **Active rate 1.5 s → 2.5 s** (`PLAYBACK_ACTIVE_POLLING_DELAY`) — the now-playing bar
  interpolates progress client-side and the last 4 s of a track drops to
  `TRANSITIONING_POLLING_DELAY_MS`, so this only bounds how fast an external skip/pause
  shows. ~40 % fewer `/me/player` calls while playing.
- [ ] **Still open**: no window-focus / lifecycle signal — a desktop window in the
  background or an Android app that's backgrounded still polls (just at the idle rate).
  Wiring an `isForeground` expect/actual into the poller would let it pause entirely.

---

## Section 3 — High  *(complete)*

### 3.1 — User-scoped writes threw before the session was initialised

- [x] **DONE** (`76104ca`) — `UserSessionRepository.awaitUserId()` (`suspend`, first
  non-null from the flow, returns immediately for a returning user). The five write-path
  `xxxRef()` helpers (`UserLibraryRepository`, `TagRepository`, `AlbumTagRepository`,
  `AlbumCollectionRepository` ×2, `CollectionAlbumRepository`) are `suspend` and use it.
  `requireUserId()` deleted.

### 3.2 — Read-modify-write without transactions (lost updates)

- [x] **DONE** (`76104ca`) — `UserLibraryRepository.addTagId` / `removeTagId` use
  `set(mapOf("tag_ids" to FieldValue.arrayUnion/arrayRemove(tagId)), merge = true)` —
  server-side atomic merge.
- [ ] **Left as-is**: the `added_at` guard in `setInLibrary` (get → conditional set). The
  race is benign (concurrent adds write the same ~timestamp) and a proper fix needs a
  transaction. Low priority.

### 3.3 — Rate-limit retry: `Retry-After` + spurious 403 retry

- [x] **DONE** (`76104ca`) — both clients pass `respectRetryAfterHeader = true` to
  `exponentialDelay`. Removed `HttpStatusCode.Forbidden` from the generic client's retry
  predicate — 403 is auth/permission, never throttling.
- [ ] **Left**: no hard cap on cumulative retry wall-time (worst case ≈ `maxRetries ×
  maxDelayMs`). Polling requests opt out via `X-No-Retry`; user-initiated ones can still
  park ~90 s. Minor.

### 3.4 — `getAllAlbums()` read the entire global `albums` collection

- [x] **DONE** (`76104ca`) — `getAllArtists()`, `getEarliestReleaseDate()` and
  `LibraryService.getLibraryStats` build on `getAllAlbumsInLibrary()` (the
  `library_albums` join). `getAllAlbums()` keeps a doc-comment warning it is the whole
  catalogue; no UI caller subscribes to it any more.

### 3.5 — HTTP client lifecycle

- [x] **DONE** (`76104ca`) — the generic client is a thread-safe `lazy`; `close()` closes
  **both** clients (skipping the generic one if it was never created). Re-calling
  `provideSpotifyApi` closes the prior client first.

### 3.6 — Non-cryptographic RNG for PKCE verifier and OAuth `state`

- [x] **DONE** (`76104ca`) — `util/SecureRandom.kt` `expect fun secureRandomHex` + JVM
  `actual`s (`java.security.SecureRandom`). `generateCodeVerifier` → `secureRandomHex(48)`;
  `generateState` → `secureRandomHex(16)`. `kotlin.random.Random` dropped.

### 3.7 — CI runs no tests

- [x] **DONE** (`76104ca`, `e4c48e7`, `d10d724`) — `.github/workflows/ci.yml`: a `test`
  job (`compileKotlinDesktop` + `compileTestKotlinDesktop` + `desktopTest`, uploads the
  report) and an `android` job (stub `google-services.json` + `compileDebugKotlinAndroid`)
  on every push/PR to `main`. Both jobs are green.
- [ ] **Follow-up**: consider `detekt`/`ktlint`. The 3-OS installer matrix (`build.yml`)
  should move to tags-only.

---

## Section 4 — Medium (correctness)  *(complete)*

- [x] **4.1 `saveAlbum` overwrote `addedAt` on every write** (`41e5494`) — both `saveAlbum`
  overloads route through `writeAlbumDocument(album)`, which reads the existing doc's
  `addedAt` and preserves it (`existingAddedAt ?: now`); `updatedAt` still bumps. Costs
  one extra `get()` per save — acceptable vs. losing `SortOrder.DATE_ADDED`.
- [x] **4.2 Double event dispatch** (`e4c48e7`) — `fetchMultipleAlbums` maps once and
  dispatches once (`saveAlbum` owns the dispatch when `saveToDb = true`, else the loop
  dispatches directly).
- [x] **4.3 `parseReleaseDate` unguarded** (`41e5494`) — wraps its body in `runCatching`
  and returns `AlbumMapper.UNKNOWN_RELEASE_DATE` (`1900-01-01`) with a warning on any
  unparseable value — fixes every call site at once. Covered by `AlbumMapperTest`.
- [x] **4.4 `LibraryService.matchesDateRange`** (`41e5494`) — `<=`/`>=` → `<`/`>` so
  albums released exactly on the (inclusive) range boundary are kept. Boundary test in
  `LibraryServiceTest`.
- [x] **4.5 `getFilteredAlbums` sort didn't react to settings** (`41e5494`) — now
  `combine(getAllAlbumsEnriched(), settingsRepository.settings)` so a sort-order change
  re-emits.
- [x] **4.6 `ModularDependencyContainer.albumEventDispatcher`** (`41e5494`) — dropped a
  shadowing local `albumTagRepository` that built a second instance; the block uses the
  `by lazy` member throughout.
- [x] **4.7 `ProfileRepository` fire-and-forget** (`9ab5512`) —
  `add/removeAlbumsFromSpotifyLibrary` return `Result<Unit>`; every 20-album chunk is
  attempted and a partial failure is reported via `AggregateException`. `LibraryService`
  `.getOrThrow()`s them so `LibraryViewModel` can show `SyncState.Error`.
- [x] **4.8 `AuthState.Error` was a dead end** (`9ab5512`) — `LoginViewModel` observes
  `AuthState.Error` and puts it in `uiState` with `showRetry = true`; the existing
  "Try Again" button calls `startAuth()` → `Authenticating`.

---

## Section 5 — Low / hygiene

- [x] **5.1 Test bug** (`e4c48e7`) — renamed to
  `"test apply sync, use spotify removes local-only albums"` and switched to
  `SyncAction.UseSpotify` (what the assertions describe).
- [~] **5.2 Thin test coverage** — ~1,100 lines across 4 real test files for ~21k LOC.
  - [x] **All 16 pre-existing `desktopTest` failures fixed** (`e4c48e7`). Root causes:
    (a) `AlbumRepositoryTest` ×9 — `setup()` stubbed `DocumentReference.set(any<Any>())`;
    `set` is `inline reified` so recording it runs `serializer<Any>()`. Fixed with relaxed
    mocks (the inline `set` becomes a no-op) + asserting on observable effects.
    (b) `LibraryServiceTest` "get library differences" — 2.2's SHA-256 id broke the
    fixture's literal ids; fixed by deriving domain fixtures from the same DTOs.
    (c) `LibraryServiceTest` "combine and deduplicate" — missing
    `artistRepository.fetchArtistsWithEnhancedGenres` stub. (d) = 5.1.
    (e) `AlbumViewModelTest` ×3 — `addAlbumToCollection` reads `settings.first()`; stubbed
    a real `StateFlow`. (f) `CollectionImportServiceTest` "no tracks have albums" — built
    tracks *with* an album then asserted empty; now `track(...).copy(album = null)`.
  - [x] New tests added along the way: `AlbumMapperTest`, `CoilNetworkSmokeTest`,
    `ResultExtTest`, `ProfileRepositoryTest`, `ScreenViewModelStoresTest`,
    `DesktopNavigatorTest`.
  - Still priority: `SpotifyAuthRepository` token refresh/expiry, `PlaybackSessionManager`
    state machine, `CollectionsService`.
- [~] **5.3 Duplicated docs** — root `PRODUCTION_ROADMAP.md` / `MIGRATION_SPOTIFY_ID.md`
  are now one-line stubs pointing at the `docs/` copies (`cea7054`); `README.md` link
  updated. **Still to do**: consolidate the 4 overlapping migration docs
  (`docs/DATABASE_MIGRATION_README.md`, `FIREBASE_MIGRATION.md`, `MIGRATION_GUIDE.md`,
  `MIGRATION_SPOTIFY_ID.md`) into one `docs/MIGRATIONS.md` with a section per migration.
- [x] **5.4 Untracked clutter in the working tree** (`cea7054`) — deleted the `records/`
  Python venv, the `backups/` SQLite ratings dump, and 9 completed one-off root scripts
  (all pre-Firebase/SQLite-era). `migration-reporter/` (the SQLite→Firestore tool) kept
  on disk but `.gitignore`d (it is not part of `settings.gradle.kts`). `.gitignore` also
  covers `records/`, `.venv/`, `venv/`, `/backups/`, `.vscode/`. `add_spotify_id_column.py`
  (tracked, doc-referenced) left for the 5.3 consolidation.
- [ ] **5.5 Release build** — `composeApp/build.gradle.kts`: `isMinifyEnabled = false` for
  `release`; `versionCode = 1` hardcoded. Enable R8/proguard for Android release; derive
  `versionCode`/`versionName` from the git tag in CI.
- [ ] **5.6 `MainActivity.enableEdgeToEdge()` is called before `super.onCreate()`** —
  move it after, per the Android docs.
- [ ] **5.7 `desktopMain/.../FirebaseDriver.desktop.kt`** uses an `android.app.Application()`
  stub + `FirebasePlatform` shim to run the Android Firestore SDK on desktop — a known
  `dev.gitlive` limitation. Document the risk in `ARCHITECTURE.md`. (The `dev.gitlive`
  versions are now aligned at 2.3.0 — see 1.10 — so the "mismatched major lines" concern
  is resolved; keep them pinned tightly.)
- [x] **5.8 `OpenAiApi.prompt` logged the full prompt + response** (`cea7054`) — now logged
  as `"(<n> chars): <first 500 chars>"` (`LOG_PREVIEW_CHARS`).
- [x] **5.9 Stale doc comments** (`cea7054`) — `OpenAiApi` KDoc no longer says
  `gpt-3.5-turbo` / `System.getenv`; `SpotifyApi` comment points at
  `ProductionNetworkModule.provideSpotifyApi` instead of the non-existent
  `HttpClientProvider`.

---

## Commit log

Ordered oldest → newest. Docs-only commits (progress-log updates, link fixes) omitted.

| Commit | Items | Summary |
|--------|-------|---------|
| `dd5bc34` | 1.1, 1.4–1.9, 1.11 | Section 1 bad patterns — see the checkboxes |
| `efb4bb6` | 2.3, 2.4 | logs + `google-services.json` untracked; desktop config from file |
| `07ab6ab` | 2.1, 2.2 | anonymous Firebase auth (`firebase-auth:2.3.0`, `ensureAnonymousAuth()`, `firestore.rules`); SHA-256 album IDs + `scripts/migrate_album_ids.py` + `GenerateAlbumIdTest` |
| `76104ca` | 3.1–3.7 | `awaitUserId`; `arrayUnion`; `Retry-After`; library-scoped queries; client lifecycle; `SecureRandom`; `ci.yml` |
| `e4c48e7` | 4.2, 5.1, 5.2 | fixed all 16 pre-existing `desktopTest` failures; `test` CI job green; 4.2 double-dispatch as a prerequisite |
| `d10d724` | 1.12 | Android compiles again — `androidx.browser:browser` + JVM 17 for android & desktop; `android` CI job green |
| `cea7054` | 5.3 (partial), 5.4, 5.8, 5.9, 1.9 | deleted dead `util/Logger.kt` + 9 one-off scripts + venv + backups; gitignored `migration-reporter/`; root doc stubs; `OpenAiApi`/`SpotifyApi` comment + log fixes |
| `6dd4c89` | 1.10 | all deps via the version catalog; ktor → 3.1.0, coil → 3.2.0, serialization-json → 1.9.0, coroutines-test → 1.10.2; pruned 8 dead entries |
| `5f78c90` | 5.2 | `CoilNetworkSmokeTest` — real fetch + decode through the coil pipeline |
| `41e5494` | 4.1, 4.3, 4.4, 4.5, 4.6 | `addedAt` preserved on re-sync; `parseReleaseDate` defensive; date-range boundary inclusive; sort reacts to settings; DI double-instance fixed; `+AlbumMapperTest` |
| `09eab02` | 1.2 | ViewModels via `viewModel { }` + per-screen `ViewModelStore` owned by the navigator; `onCleared()` fires on pop; `+ScreenViewModelStoresTest`, `+DesktopNavigatorTest` |
| `9ab5512` | 1.3 (slice 1), 4.7, 4.8 | `ResultExt` helper; `ProfileRepository` → `Result<Unit>` w/ aggregation; sync failures → `SyncState.Error`; `LoginViewModel` surfaces `AuthState.Error`; `+ResultExtTest`, `+ProfileRepositoryTest` |
| `85cfb70` | 1.3 (slice 2) | `AlbumRepository.fetchAlbum` → `Result<Album>`; `fetchReleaseGroupId` → `Result.failure` not `throw`; `fetchMultipleAlbums` `resultOf`; caller `.first()` → `.firstOrNull()` |
| `28464bd` | 1.3 (slice 3) | `ViewModelExt.launchSafely`; every bare `viewModelScope.launch` across all 10 VMs converted; `PlaybackViewModel` catches re-throw `CancellationException`; `AGENTS.md` updated |
| `d82a21e` | 2.7 (observability) | `util/TrafficMetrics` + `TrafficSource` — lock-free per-screen Firestore/Spotify call counters, periodic `Traffic` log report; wired through `LoggingUtils` and screen navigation |
| `a1705c9` | 2.7 (v1) | denormalised stable-field projection on `users/{uid}/library_albums`; `getAllAlbumsInLibrary` renders from one listener, no `albums` fan-out; `scripts/backfill_library_projection.py`; `+AlbumMapperTest` |
| `c4c2453` | 2.7 (collections) | same projection on `collections/{name}.albums[]`; `getAlbumsInCollection` no longer joins `albums`; `addAlbumToCollection(name, album)`; backfill script extended; `+AlbumMapperTest` |
| `f2551fe` | 2.7 (tracklists) | `TrackRepository.getAlbumTracks` — in-memory 24 h-TTL cache; removed the permanent `tracks` tracklist mirror (`getTracksForAlbum` / `checkAndUpdateTracksIfNeeded` / `fetchAndSaveTracks`); `combine(5)`→`(4)` in `GetAlbumDetailUseCase` |
| `be0e015` | 2.8 | `PlaybackPoller` progressive idle back-off (`PLAYBACK_IDLE_BACKOFF_STEPS` 8→20→45→60 s); ~3 `/me/player` req/min while idle, was ~30–40; `+PlaybackPollerTest` |
| `59679e6` | 2.7 (tracklists) | album-tracklist heart indicator restored via `GET /me/tracks/contains` (`markSavedStatus`) + `savedOverrides` for optimistic toggle; `+TrackRepositoryTest` |
| `c33b8a7` | 2.7 (backfill) | `backfill_library_projection.py` iterates users via `list_documents()` (phantom parent docs); **run against prod** — cold library open now does 0 `getAlbumsByIds` reads (was ~16k `albums` docs/session) |
| `983d1ae` | traffic | liked-tracks sync diffs by track id (was `List<Track>` equality → ~4,400 Firestore writes/sync churning the whole set); `initUserSession` skips `GET /me` once the id is known; `+LibraryServiceTest` |
| `47d28fa` | traffic | new-releases feed: page 1 only + 30-min session cache in `AlbumRepository` (was ~5 `/browse` calls + a 100-id whereIn per Search init); `+AlbumRepositoryTest` |
| `35c0209` | 2.8 | `PLAYBACK_ACTIVE_POLLING_DELAY` 1.5 s → 2.5 s |

**Verification**: `./gradlew :composeApp:compileKotlinDesktop :composeApp:compileTestKotlinDesktop`
and `:composeApp:compileDebugKotlinAndroid` pass. `desktopTest` = **82 tests / 0 failing**.
Desktop app boots & runs.
