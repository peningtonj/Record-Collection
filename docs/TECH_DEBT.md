# Record Collection — Tech Debt & Remediation Plan

**Last Updated**: 2026-09-07
**Status**: Active — work top-down
**Scope**: Concrete, file-level fixes derived from a full codebase review. This is the
practical companion to [PRODUCTION_ROADMAP.md](../PRODUCTION_ROADMAP.md) (which is the
higher-level 16-week plan). When the two disagree, this document is the source of truth
for *what is actually wrong today*.

---

## How to use this document

- Sections are ordered by priority. **Do Section 1 (Bad Patterns) first** — those are
  cross-cutting and every later fix is easier once they're gone.
- Each item has: **why it's wrong**, **where**, **the fix**, and a checkbox.
- Check the box and add the commit SHA when done.
- Don't add new code that reintroduces a fixed pattern — the anti-patterns are also
  listed in `AGENTS.md` § Conventions & Gotchas.

---

## Section 1 — Bad Patterns to Fix First

These are systemic. Fix the pattern everywhere it appears, not just one instance.

### 1.1 — `runBlocking` in repositories (blocks the UI thread) ⚠️

- [x] **DONE** (2026-09-07) — `TagRepository` + `AlbumTagRepository` write methods are now
  `suspend`; `TagService.addTagToAlbum/removeTagFromAlbum` and
  `AlbumProcessingHandler.processAlbumTags` propagate `suspend`; `AlbumViewModel` wraps
  the calls in `viewModelScope.launch`. Tests updated to `coEvery`/`coVerify`.
- **Why**: `runBlocking` on the caller's thread parks it until network I/O
  completes. On Android that's an ANR; on desktop the UI freezes.
- **Where**:
  - `repository/TagRepository.kt` — `insertTag`, `deleteTag`, `updateTag`
  - `repository/AlbumTagRepository.kt` — `addTagToAlbum`, `removeTagFromAlbum`
- **Fix**: make each function `suspend`, delete the `runBlocking { }` wrapper, and push
  the call into a coroutine at the ViewModel layer (`viewModelScope.launch { }`).
  Remove the `kotlinx.coroutines.runBlocking` imports.

### 1.2 — ViewModels created with `remember { }` instead of a ViewModel factory ⚠️

- [ ] **NOT STARTED** — needs a navigator change first (see below); deferred as a
  standalone piece of work. This is the highest-impact remaining Section 1 item.
- **Why**: All 12 `androidx.lifecycle.ViewModel` subclasses are built in
  `viewmodel/ViewModelFactoryExtensions.kt` via `remember { XxxViewModel(...) }`.
  They are never registered with a `ViewModelStore`, so `onCleared()` is **never
  called**. Every navigation leaks a `viewModelScope` and its Firestore `.snapshots`
  listeners — they keep running (and keep billing reads) forever.
- **Where**: `viewmodel/ViewModelFactoryExtensions.kt` (every `rememberXxxViewModel`),
  and `viewmodel/PlaybackViewModel.kt:169` (`onCleared()` override that never fires).
- **Blocker**: navigation is fully custom (`navigation/NavigationScreen.kt` +
  `DesktopNavigator`/`AndroidNavigator`; `NavigationHost` just does
  `content(currentScreen)`). There is **no per-screen `ViewModelStoreOwner`**, so
  `viewModel { }` on its own would only dedupe to one app/window-level store — still
  never cleared. A correct fix requires the navigator to own a `ViewModelStore` per
  back-stack entry and `.clear()` it on pop.
- **Fix (sequenced)**:
  1. Add `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` to the catalog.
  2. In the navigator, keep a `Map<Screen, ViewModelStore>`; on `NavigateBack` /
     `PopUpTo` (inclusive) call `store.clear()` for the popped entries.
  3. Wrap each rendered screen in a `CompositionLocalProvider(LocalViewModelStoreOwner
     provides <entry owner>)`.
  4. Rewrite `rememberXxxViewModel` as `viewModel { XxxViewModel(deps...) }` reading
     `LocalDependencyContainer`.
  5. Verify `onCleared()` fires on back-navigation (add a log / test).
- **Interim mitigation** (if the full switch is deferred): give each VM a private
  `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` instead of
  `viewModelScope`, expose a `dispose()`, and call it from a
  `DisposableEffect(Unit) { onDispose { vm.dispose() } }` in the screen.

### 1.3 — Inconsistent error handling (throw vs `Result` vs null vs silent) ⚠️

- [ ] **NOT STARTED** — large, cross-cutting; tackle incrementally after 1.2. Item 1.4
  (one instance) is done.
- **Why**: `AGENTS.md` already flags this. The mix means callers can't know whether
  to `try/catch`, check for null, or inspect a `Result`. Sync operations currently
  **swallow failures entirely** — a failed sync looks identical to a successful one.
- **Where** (representative, not exhaustive):
  - `repository/AlbumRepository.kt` — `fetchAlbum` returns `Album?` but `throw`s on
    failure; `getAlbumById` `throw`s *inside* a `Flow.map` operator.
  - `service/LibraryService.kt` — `applySync`, `updateLibraryTracksFromSpotify`,
    `batchArtistsThenSaveLocalAlbums` have no error handling.
  - `viewmodel/LibraryViewModel.kt` — `startTrackSync`, `launchSync` launch into
    `viewModelScope` with no `try/catch`; exceptions vanish.
  - `repository/ProfileRepository.kt` — `addAlbumsToSpotifyLibrary` /
    `removeAlbumsFromSpotifyLibrary` ignore the `Result` from the API call.
- **Fix**: adopt one convention (per `AGENTS.md`: **`Result<T>` for new code**).
  - Repository suspend functions that can fail → return `Result<T>`.
  - Flows → never `throw` in an operator; emit a sealed `LoadState` or use `.catch { }`.
  - ViewModels → every `viewModelScope.launch` that calls a fallible operation wraps it
    and routes failure to a `StateFlow<UiState>` the screen can render.
  - Add a shared `runCatchingResult { }` helper if it reduces boilerplate.

### 1.4 — `throw` inside `Flow` operators

- [x] **DONE** (2026-09-07) — `AlbumRepository.getAlbumById` now returns `Flow<Album?>`
  and emits `null` (with a warning log) instead of throwing on a missing/undeserializable
  doc. `GetAlbumDetailUseCase.getDatabaseAlbum` applies `.filterNotNull()` before the
  combine.
- **Why**: an exception in `.map { }` propagates to the collector and crashes it
  unless every call site adds `.catch`. Callers don't.

### 1.5 — `System.currentTimeMillis()` / `System.getenv` in `commonMain`

- [x] **DONE** (2026-09-07) — all 6 real occurrences swapped to
  `Clock.System.now().toEpochMilliseconds()` (`SpotifyAuthRepository`,
  `AlbumRepository` ×2, `LoggingUtils` ×2). Only a stale KDoc comment in `OpenAiApi`
  still mentions `System.getenv` (tracked in 5.9). The `addedAt` (ISO string) vs
  `updatedAt` (epoch millis) type split still exists — see 4.1.
- **Why**: compiles only because both targets are JVM. It's not real KMP and will
  break the moment a native/wasm target is added. Also mixes time representations.
- **Where**: `repository/SpotifyAuthRepository.kt` (`StoredToken.isExpired`,
  `saveToken`), `repository/AlbumRepository.kt:56` (`updatedAt = System.currentTimeMillis()`
  alongside `addedAt = Clock.System.now().toString()` — two different types for two
  timestamps on the same write), `util/LoggingUtils.kt` (`measureAndLog`).
- **Fix**: use `kotlinx.datetime.Clock.System.now().toEpochMilliseconds()` everywhere.
  Pick **one** stored representation for timestamps (recommend epoch millis `Long`, or
  ISO-8601 `String` — not both).

### 1.6 — Default-package (no `package`) top-level declarations

- [x] **DONE** (2026-09-07) — `ArtistDetailViewModel` → `...viewmodel`;
  `PlaybackQueueService` (+ its `TRANSITION_TRIGGER_MS` / `NEXT_ALBUM_TRIGGER_MS`
  consts) → `...service`; `ArtistDetailScreen` → `...ui.screens`;
  `Playlist` / `AlbumResult` / `PlaylistAlbumExtractor` (`ImportCollections.kt`) →
  `...network.spotify`. All import sites updated. `grep '^import [A-Za-z_]*$'` is now
  clean. Two dead default-package files deleted outright: `migration/DatabaseMigrationUtil.kt`
  (old SQLite/JDBC code, no longer used) and `desktopMain/kotlin/test.kt` (scratch).

### 1.7 — Package name ≠ directory

- [x] **DONE** (2026-09-07) — `AlbumTagRepository` moved from `...db.repository` to
  `...repository` (12 import sites updated); `CollectionDetailViewModel` moved from
  `...ui.collection` to `...viewmodel` (3 sites). Both packages now match their
  directory.

### 1.8 — Ad-hoc `CoroutineScope`s that are never cancelled

- [x] **DONE** (2026-09-07) — `SettingsRepository` no longer spawns a background scope:
  `Settings` is synchronous, so the initial state is read eagerly in the property
  initializer (`init` block + `CoroutineScope`/`Dispatchers`/`launch` imports removed).
  `ModularDependencyContainer.close()` now calls `eventScope.cancel()`.

### 1.9 — Debug noise left in production code paths

- [x] **DONE** (2026-09-07) — `ProductionNetworkModule` generic client: all `println` /
  `printStackTrace()` replaced with `LoggingUtils.w/e/i` under `Category.NETWORK`.
  Stray `println(album)` removed from `SearchResultComponents`. `DatabaseMigrationUtil`
  (8 more `println`) deleted (see 1.6). `util/Logger.kt` (unused custom `Antilog` with a
  `println`, never wired up — `Logger.initialize` had zero callers) deleted 2026-09-07.

### 1.10 — `libs.versions.toml` is not actually the single source of truth

- [ ] **PARTIAL** (2026-09-07) — koin removed (see 1.11). Still to do: move the remaining
  inline string-literal deps into the catalog and reconcile ktor `3.1.0` vs `3.0.3`,
  and the test-coroutines version.
- **Why**: `AGENTS.md` says it is, but `composeApp/build.gradle.kts` hardcodes many
  versions as string literals that disagree with the catalog:
  - ktor `3.1.0` inline vs `3.0.3` in the catalog
  - `kotlinx-serialization-json:1.8.1` inline
  - koin `3.5.0` / `1.1.0` inline (and **koin is unused** — see 1.11)
  - test: `coroutines = "1.7.3"` in the catalog vs `kotlinx-coroutines = "1.10.2"` used elsewhere
- **Fix**: move every dependency + version into `libs.versions.toml`, reference via
  `libs.*` only. Reconcile ktor to one version. Align the coroutines test version.

### 1.11 — Unused dependencies

- [x] **DONE** (2026-09-07) — `io.insert-koin:koin-core` and `koin-compose` removed from
  `build.gradle.kts` (grep confirms zero Koin references in `*.kt`).

### 1.12 — The Android target does not compile ⚠️⚠️ (pre-existing, discovered 2026-09-07)

- [x] **DONE** (2026-09-07) — `./gradlew :composeApp:compileDebugKotlinAndroid` now passes.
  Both causes fixed: (1) `androidx.browser:browser:1.8.0` added to the version catalog and
  `androidMain.dependencies`; (2) `androidTarget` + `jvm("desktop")` Kotlin `jvmTarget` and
  the Android `compileOptions` all raised to `JVM_17` (was 11). The CI `android` job
  (`compileDebugKotlinAndroid`) should now be green. The `debug`→`release` R8 / `versionCode`
  work is still 5.5.
- [ ] **Why** (original): `./gradlew :composeApp:compileDebugKotlinAndroid` failed on a clean
  checkout (verified by stashing all Section-1 work — the failure was unrelated to it).
  Two independent causes:
  1. **Missing dependency** — `androidMain/.../oauth/spotify/AndroidAuthHandler.kt`
     uses `androidx.browser.customtabs.CustomTabsIntent` but `androidx.browser:browser`
     is not on the `androidMain` classpath → `Unresolved reference 'browser'`,
     `CustomTabsIntent`, `launchUrl`.
  2. **JVM target mismatch** — `androidTarget { compilerOptions { jvmTarget = JVM_11 } }`
     + Java-11 `compileOptions`, but the Kotlin 2.1.21 stdlib inline functions
     (`buildMap`, `fold`, …) are compiled for target 17:
     `Cannot inline bytecode built with JVM target 17 into bytecode that is being built
     with JVM target 11` — fires across `AlbumCollectionRepository.kt`,
     `UserLibraryRepository.kt`, etc.
- **Impact**: one of the two advertised platforms is dead. CI only builds the desktop
  distributable (3.7), so nothing caught this. `desktopTest` and `compileKotlinDesktop`
  are green — this is Android-only.
- **Fix**:
  1. Add `androidx.browser:browser` (via the version catalog) to `androidMain.dependencies`.
  2. Raise the Android JVM target to 17: `jvmTarget.set(JvmTarget.JVM_17)` and
     `compileOptions { sourceCompatibility / targetCompatibility = VERSION_17 }`
     (`minSdk = 24` + AGP 8.9 desugaring make this safe). Align `desktop` and
     `androidTarget` on the same target. This also resolves the commonMain half of 1.5.
  3. Then get Android into CI (see 3.7) so it can't regress silently again.

---

## Section 2 — Critical (security & data integrity)

### 2.1 — Firestore has no authentication ⚠️⚠️

- [x] **DONE — anonymous-auth stopgap** (2026-09-07). Chosen approach: require *any*
  authenticated client now; full per-user custom-token auth deferred.
  - `dev.gitlive:firebase-auth:2.3.0` added to `commonMain` (replaces the stale
    desktop-only `:1.12.0`).
  - `db/FirebaseAuth.kt` → `ensureAnonymousAuth()`; called (blocking, 15 s timeout) at
    startup from both `DependencyContainerFactory` (desktop) and
    `AndroidDependencyContainerFactory` after Firebase init, before any repository runs.
  - `firestore.rules` + `firebase.json` committed at repo root:
    `match /{document=**} { allow read, write: if request.auth != null; }`
  - **Manual steps still required** (see README → Firebase setup):
    1. Firebase console → Authentication → Sign-in method → **enable Anonymous**.
    2. `firebase deploy --only firestore:rules` (or paste the rules in the console).
- [ ] **Follow-up — real per-user auth** (deferred): Cloud Function verifies a Spotify
  token and mints a Firebase **custom token**; client calls `signInWithCustomToken` so
  `request.auth.uid == spotifyUserId`. Then replace the catch-all rule with the
  per-collection rules already sketched (commented) in `firestore.rules`. The anonymous
  UID gives no user isolation — any signed-in client can still read/write any
  `users/{uid}/…` path.
- **Note**: Android still can't exercise this until 1.12 is fixed (Android doesn't
  compile, and `AndroidDependencyContainerFactory` also never called `initializeFirebase()`
  — now it does).

### 2.2 — Album document IDs are a 32-bit `String.hashCode()` ⚠️⚠️

- [x] **DONE (code)** (2026-09-07) — `generateAlbumId(name, artist)` now returns
  `sha256Hex("<name>|<artist>".normalized()).take(24)` (96 bits — collision-safe).
  - `util/Hashing.kt` `expect fun sha256Hex` + JVM `actual`s (desktop + android) using
    `java.security.MessageDigest` — no hand-rolled crypto, no `System.*` in commonMain.
  - `GenerateAlbumIdTest` locks the contract, incl. the exact digest cross-checked
    against `openssl` / the Python script.
- [x] **Migration script** — `scripts/migrate_album_ids.py` (firebase-admin, `--dry-run`
  default, `--execute` to apply). Re-keys `albums/{id}`, `users/{uid}/library_albums/{id}`,
  `users/{uid}/collections/*.albums[].albumId`, and `tracks.*.album_id`. Idempotent.
  Normalization is kept byte-for-byte in sync with the Kotlin function.
- [ ] **Not yet run** — the user runs it once against Firestore
  (`python3 scripts/migrate_album_ids.py --cred key.json` to preview, then `--execute`).
  Until then, existing albums keep their old IDs and won't match new writes.
- **Identity model** (unchanged, deliberate): `id = f(name, primary_artist)` per
  `MIGRATION_SPOTIFY_ID.md` — `spotifyId` is a separate field. name+artist collapses
  remasters / deluxe editions on purpose (release-group swapping relies on it). Should be
  spelled out in `ARCHITECTURE.md` once that file's DB section is rewritten (5.3).

### 2.3 — Query logs committed to git and growing every commit ⚠️

- [x] **DONE (working tree)** (2026-09-07) — `composeApp/logs/` added to `.gitignore`
  (plus `*.db` — the old pre-Firebase SQLite files are also in history, see below);
  `git rm --cached composeApp/logs`. Logs stay on disk, untracked.
- [ ] **Still to do — history rewrite** (destructive, needs explicit go-ahead):
  the big blobs are still in history — `composeApp/logs/spotify_queries.log` ≈ **19.7 MB**,
  `firebase_queries.log` ≈ 1.1 MB, and several `composeApp/record_collection*.db`
  (1–1.4 MB each, contain real album/rating data from the pre-Firebase era). Purge with
  `git filter-repo --path composeApp/logs --path-glob 'composeApp/*.db' --invert-paths`
  then force-push. Solo repo → safe, but it rewrites every later SHA.
- [ ] **Optional**: point the file antilogs at a path outside the repo, or cap size / rotate.

### 2.4 — Secrets committed / hardcoded

- [x] **DONE (working tree)** (2026-09-07) — `composeApp/google-services.json` +
  `composeApp/firebase.properties` added to `.gitignore`; `git rm --cached
  composeApp/google-services.json`. `FirebaseDriver.desktop.kt` no longer hard-codes the
  Firebase `apiKey`/`applicationId`/`projectId`/`gcmSenderId`/`storageBucket` — it reads
  them at runtime from `google-services.json` (`GOOGLE_SERVICES_JSON` env var →
  `composeApp/google-services.json` → cwd), with a clear error if the file is missing.
- [ ] **Still to do — history rewrite**: the old `google-services.json` blob is still in
  history (bundle it into the 2.3 filter-repo run). Firebase config is not a true secret
  (security is rules + auth — see 2.1), so this is low urgency.

### 2.5 — Secrets stored at rest in plaintext

- [ ] **Why**: Spotify access/refresh tokens (`SpotifyAuthRepository`) and the OpenAI
  API key (`SettingsRepository.kt:51`) are stored via `multiplatform-settings`
  (Java `Preferences` / `SharedPreferences`) unencrypted.
- **Fix**: Android → `EncryptedSharedPreferences` (or DataStore + Tink). Desktop →
  OS keychain (`keytar`-style JNA, or the `secret-service`/Keychain/Credential Manager
  APIs). At minimum, encrypt with a key derived from a machine secret.

---

## Section 3 — High

### 3.1 — User-scoped **writes** throw before the session is initialised

- [x] **DONE** (2026-09-07) — `UserSessionRepository.awaitUserId()` (`suspend`, first
  non-null from the flow, returns immediately for a returning user). The five write-path
  `xxxRef()` helpers (`UserLibraryRepository`, `TagRepository`, `AlbumTagRepository`,
  `AlbumCollectionRepository` ×2, `CollectionAlbumRepository`) are now `suspend` and use
  it. `requireUserId()` deleted.

### 3.2 — Read-modify-write without transactions (lost updates)

- [x] **DONE** (2026-09-07) — `UserLibraryRepository.addTagId` / `removeTagId` now use
  `set(mapOf("tag_ids" to FieldValue.arrayUnion/arrayRemove(tagId)), merge = true)` —
  server-side atomic merge, so the `AlbumProcessingHandler` tag loop can't lose tags to
  a stale `.get()`.
- [ ] **Left as-is**: the `added_at` guard in `setInLibrary` (get → conditional set). The
  race is benign (concurrent adds write the same ~timestamp) and fixing it properly needs
  a transaction. Low priority.

### 3.3 — Rate-limit retry: `Retry-After` + spurious 403 retry

- [x] **DONE** (2026-09-07) — both clients pass `respectRetryAfterHeader = true` to
  `exponentialDelay` explicitly (Ktor honours the server's `Retry-After` when present,
  else backs off exponentially). Removed `HttpStatusCode.Forbidden` from the generic
  client's retry predicate — 403 is auth/permission, never throttling.
- [ ] **Left**: no hard cap on cumulative retry wall-time (worst case ≈ `maxRetries ×
  maxDelayMs`). Polling requests already opt out via `X-No-Retry`; user-initiated ones
  are bounded but can still park ~90 s. Minor.

### 3.4 — `getAllAlbums()` reads the entire global `albums` collection

- [x] **DONE** (2026-09-07) — `getAllArtists()` and `getEarliestReleaseDate()` now build
  on `getAllAlbumsInLibrary()` (the `library_albums` join); `LibraryService.getLibraryStats`
  too (and drops the now-redundant `getLibraryCount()` combine input). `getAllAlbums()`
  keeps a doc-comment warning it is the whole catalogue, not a user view. No caller
  subscribes to it from the UI any more.

### 3.5 — HTTP client lifecycle

- [x] **DONE** (2026-09-07) — generic client is a thread-safe `lazy`; the Spotify client
  is stored and `close()` now closes **both** (and skips the generic one if it was never
  created, via `lazy.isInitialized()`). Re-calling `provideSpotifyApi` closes the prior
  client first.

### 3.6 — Non-cryptographic RNG for PKCE verifier and OAuth `state`

- [x] **DONE** (2026-09-07) — `util/SecureRandom.kt` `expect fun secureRandomHex` + JVM
  `actual`s (`java.security.SecureRandom`). `BaseAuthHandler.generateCodeVerifier` →
  `secureRandomHex(48)`; `DesktopAuthHandler` / `AndroidAuthHandler` `generateState` →
  `secureRandomHex(16)`. `kotlin.random.Random` import dropped.

### 3.7 — CI runs no tests

- [x] **DONE — workflow added** (2026-09-07) — `.github/workflows/ci.yml`: a `test` job
  (`compileKotlinDesktop` + `compileTestKotlinDesktop` + `desktopTest`, uploads the
  report) and an `android` job (writes a stub `google-services.json`, runs
  `compileDebugKotlinAndroid`) on every push/PR to `main`.
- [x] **`test` job is now green** (2026-09-07) — all 16 pre-existing `desktopTest`
  failures fixed (see 5.2). `test` can be made a required check.
- [x] **`android` job green** (2026-09-07) — 1.12 fixed.
- [ ] **Follow-up**: consider `detekt`/`ktlint`. The 3-OS installer matrix (`build.yml`)
  should move to tags-only.

---

## Section 4 — Medium (correctness)

- [ ] **4.1 `saveAlbum` overwrites `addedAt` on every write**
  (`AlbumRepository.kt:55,74`) — re-sync resets every album's "date added", breaking
  `SortOrder.DATE_ADDED`. Only set `addedAt` when the doc doesn't already exist.
- [x] **4.2 Double event dispatch** — DONE (2026-09-07). `fetchMultipleAlbums` now maps
  once and dispatches once: `saveAlbum` owns the dispatch when `saveToDb = true`, else the
  loop dispatches directly. (Fixed alongside the test repair — the old code dispatched 2×
  per album, which made `AlbumRepositoryTest` uncheckable.)
- [ ] **4.3 `parseReleaseDate` unguarded in `toDomain(AlbumDto)`**
  (`AlbumMapper.kt`) — a malformed Spotify `release_date` throws and aborts the whole
  mapping/sync. Wrap it (the `AlbumDocument` path already does) and fall back to a
  sentinel date + a warning.
- [ ] **4.4 `LibraryService.matchesDateRange`** uses `<=` / `>=` to *exclude*, so albums
  released exactly on a range boundary are filtered out. Confirm intent; likely should
  be `<` / `>` or the boundary comparison is inverted.
- [ ] **4.5 `getFilteredAlbums` sort doesn't react to settings changes** — it reads
  `settingsRepository.settings.value.defaultSortOrder` inside `.map`, so changing the
  sort order doesn't re-emit. `combine` with the settings flow.
- [ ] **4.6 `ModularDependencyContainer.albumEventDispatcher` lazy block**
  (lines ~64–73) — references `albumTagRepository` on line 65 before its local
  declaration on line 66 (resolves to the member and shadows), and builds a **second**
  `AlbumTagRepository` instance distinct from the member property. Use the member
  everywhere; remove the local.
- [ ] **4.7 `ProfileRepository` fire-and-forget** — `addAlbumsToSpotifyLibrary` /
  `removeAlbumsFromSpotifyLibrary` discard the API `Result`. Aggregate failures and
  return `Result<Unit>` / a partial-failure report.
- [ ] **4.8 `AuthState.Error` is a dead end** — nothing transitions out of it; a
  transient auth failure wedges the app until restart. Add a retry path / timeout back
  to `NotAuthenticated`.

---

## Section 5 — Low / hygiene

- [x] **5.1 Test bug** — DONE (2026-09-07). Renamed to
  `"test apply sync, use spotify removes local-only albums"` and switched to
  `SyncAction.UseSpotify` (which is what the assertions describe).
- [ ] **5.2 Thin test coverage** — ~1,100 lines across 4 real test files for ~21k LOC.
  - [x] **All 16 pre-existing `desktopTest` failures fixed** (2026-09-07). Root causes:
    (a) `AlbumRepositoryTest` ×9 — `setup()` stubbed `DocumentReference.set(any<Any>())`;
    `set` is `inline reified` so recording it runs `serializer<Any>()`. Fixed by making
    `albumDocRef`/`albumDocSnapshot` relaxed mocks (the inline `set` becomes a no-op) and
    asserting on observable effects (`document(id)`, `userLibraryRepository.setInLibrary`,
    event dispatch) instead of on `set()`. (b) `LibraryServiceTest` "get library
    differences" — TECH_DEBT 2.2 made `AlbumMapper.toDomain(dto)` compute a SHA-256 id, so
    the fixture's literal-id domain albums no longer matched the DTO-derived ones; fixed by
    deriving the domain fixtures from the same DTOs. (c) `LibraryServiceTest` "combine and
    deduplicate" — missing `artistRepository.fetchArtistsWithEnhancedGenres` stub. (d) 5.1.
    (e) `AlbumViewModelTest` ×3 — `addAlbumToCollection` reads `settingsRepository.settings
    .first()` and the relaxed mock's flow never emits; stubbed a real `StateFlow`.
    (f) `CollectionImportServiceTest` "no tracks have albums" — built tracks *with* an
    album then asserted empty; now builds `track(...).copy(album = null)`.
  - Still priority additions: `generateAlbumId` (collisions — partly covered),
    `AlbumMapper` round-trips, `SpotifyAuthRepository` token refresh/expiry,
    `PlaybackSessionManager` state machine, `CollectionsService`.
- [ ] **5.3 Duplicated docs** — root `PRODUCTION_ROADMAP.md` / `MIGRATION_SPOTIFY_ID.md`
  are now one-line stubs pointing at the `docs/` copies (2026-09-07); `README.md` link
  updated. **Still to do**: consolidate the 4 overlapping migration docs
  (`docs/DATABASE_MIGRATION_README.md`, `FIREBASE_MIGRATION.md`, `MIGRATION_GUIDE.md`,
  `MIGRATION_SPOTIFY_ID.md`) into one `docs/MIGRATIONS.md` with a section per migration.
- [x] **5.4 Untracked clutter in the working tree** — DONE (2026-09-07). Deleted the
  `records/` Python venv, the `backups/` SQLite ratings dump, and 9 completed one-off
  root scripts (`backup.sh`, `db.py`, `fix_firestore_tracks.py`, `migrate-to-firebase.sh`,
  `migrate_to_multiuser.py`, `report-db.sh`, `restore_ratings.sh`,
  `run_migration_reporter.sh`, `test-firebase-app.sh` — all pre-Firebase/SQLite-era).
  `migration-reporter/` (the SQLite→Firestore tool) kept on disk but `.gitignore`d — it
  is not part of `settings.gradle.kts`. `.gitignore` also now covers `records/`, `.venv/`,
  `venv/`, `/backups/`, `.vscode/`. `add_spotify_id_column.py` (tracked, doc-referenced)
  left for the 5.3 migration-doc consolidation.
- [ ] **5.5 Release build** — `composeApp/build.gradle.kts`: `isMinifyEnabled = false`
  for `release`; `versionCode = 1` hardcoded. Enable R8/proguard for Android release;
  derive `versionCode`/`versionName` from the git tag in CI.
- [ ] **5.6 `MainActivity.enableEdgeToEdge()` is called before `super.onCreate()`** —
  move it after, per the Android docs.
- [ ] **5.7 `desktopMain/.../FirebaseDriver.desktop.kt`** uses an `android.app.Application()`
  stub + `FirebasePlatform` shim to run the Android Firestore SDK on desktop. This is a
  known `dev.gitlive` limitation; document the risk in `ARCHITECTURE.md` and pin the
  `dev.gitlive` versions tightly (currently `firebase-firestore:2.3.0` +
  `firebase-auth:1.12.0` — mismatched major lines).
- [x] **5.8 `OpenAiApi.prompt`** — DONE (2026-09-07). Prompt + raw response are now logged
  as `"(<n> chars): <first 500 chars>"` (`LOG_PREVIEW_CHARS`).
- [x] **5.9 Stale doc comments** — DONE (2026-09-07). `OpenAiApi` KDoc no longer says
  `gpt-3.5-turbo` / `System.getenv`; `SpotifyApi` comment now points at
  `ProductionNetworkModule.provideSpotifyApi` (the Ktor `Auth` install) instead of the
  non-existent `HttpClientProvider`.

---

## Progress log

| Date | Section | Item | Commit | Notes |
|------|---------|------|--------|-------|
| 2026-09-07 | 1 | 1.1, 1.4–1.9, 1.11 | dd5bc34 | See Section 1 checkboxes |
| 2026-09-07 | 2 | 2.3, 2.4 | efb4bb6 | logs + google-services.json untracked; desktop config from file |
| 2026-09-07 | 2 | 2.1 anon-auth stopgap | 07ab6ab | firebase-auth 2.3.0, ensureAnonymousAuth(), firestore.rules |
| 2026-09-07 | 2 | 2.2 SHA-256 IDs + migration | 07ab6ab | sha256Hex expect/actual; scripts/migrate_album_ids.py; GenerateAlbumIdTest |
| 2026-09-07 | 3 | 3.1–3.7 | 76104ca | awaitUserId; arrayUnion; Retry-After; library-scoped queries; client lifecycle; SecureRandom; ci.yml |
| 2026-09-07 | 4/5 | 4.2, 5.1, 5.2 | e4c48e7 | Fixed all 16 pre-existing desktopTest failures; `test` CI job now green. Also fixed 4.2 (double dispatch) as a prerequisite. |
| 2026-09-07 | 1 | 1.12 | d10d724 | Android compiles again: androidx.browser:browser + JVM 17 for android & desktop. `android` CI job now green. |
| 2026-09-07 | 5 | 5.3 (partial), 5.4, 5.8, 5.9, 1.9 | cea7054 | Deleted dead util/Logger.kt + 9 one-off scripts + records venv + backups; gitignored migration-reporter. Root doc stubs. OpenAiApi/SpotifyApi comment + log fixes. |

**Verification**: `./gradlew :composeApp:compileKotlinDesktop :composeApp:compileTestKotlinDesktop`
passes. `desktopTest` = **47 tests / 0 failing** (as of 2026-09-07 — the 16 pre-existing
failures are fixed, see 5.2).

`compileDebugKotlinAndroid` now **passes** (see 1.12 — fixed 2026-09-07).

**Not done in Section 1**: 1.2 (ViewModel factory — needs navigator work), 1.3 (error
handling — large, incremental), 1.10 (rest of version-catalog consolidation), 1.12
(Android build — pre-existing, newly documented).
