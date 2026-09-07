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
  (8 more `println`) deleted (see 1.6). Remaining `println` in `util/Logger.kt` is
  inside an unused custom `Antilog` — that whole object is dead code (candidate for
  deletion; not a production path).

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

- [ ] **Why**: `./gradlew :composeApp:compileDebugKotlinAndroid` fails on a clean
  checkout (verified by stashing all Section-1 work — the failure is unrelated to it).
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

- [ ] **Why**: there is **no Firebase Auth anywhere** in the app (grep: no `signIn`,
  `FirebaseAuth`, `Firebase.auth`). Data is stored under `users/{spotifyUserId}/…` and
  the Spotify user ID is not secret. With no `request.auth`, Firestore rules can only be
  allow-all or deny-all; the app works, so the database is effectively open. Anyone with
  the Firebase config (committed in `composeApp/google-services.json` and hardcoded in
  `desktopMain/.../db/FirebaseDriver.desktop.kt`) can read every user's library and
  overwrite any user's data by supplying their Spotify ID.
  `PRODUCTION_ROADMAP.md` Phase 4 lists "Security rules in place" — they are **not**.
- **Fix**:
  1. Add Firebase Auth. Cleanest path: a small Cloud Function / backend that verifies a
     Spotify access token and mints a Firebase **custom token**; the client calls
     `signInWithCustomToken`. Then `request.auth.uid` == Spotify user ID.
  2. Write and deploy `firestore.rules`:
     - `users/{uid}/{doc=**}` → `allow read, write: if request.auth.uid == uid`
     - shared `albums`, `artists`, `tracks` collections → `allow read: if request.auth != null`,
       `allow write: if request.auth != null` (tighten later; consider moving writes
       behind the backend).
  3. Commit `firestore.rules` + `firebase.json` to the repo; deploy via `firebase deploy`
     in CI.
  4. Gate every `firestore` access on an authenticated session (the
     `UserSessionRepository` gate becomes an *auth* gate, not just an ID cache).

### 2.2 — Album document IDs are a 32-bit `String.hashCode()` ⚠️⚠️

- [ ] **Why**: `db/mapper/AlbumMapper.kt` → `generateAlbumId()` does
  `"$name|$artist".hashCode().toString(36).replace("-", "0")`.
  - 32-bit space → collisions become likely in the tens-of-thousands range; a collision
    silently overwrites a different album in the shared `albums` collection.
  - `.replace("-", "0")` maps distinct negative hashes onto the same string (e.g. hash
    `-1abc` and `01abc`), *raising* the collision rate.
- **Fix**: use a real hash. `SHA-256(normalized("$name|$artist"))` hex, truncated to
  ~16–24 chars, via `kotlinx-crypto` or a small multiplatform SHA-256. Keep a migration
  path: `MIGRATION_SPOTIFY_ID.md` already documents an ID migration; add a new step that
  rewrites doc IDs and updates every reference (`library_albums`, `collection_albums`,
  `release_group_id` links).
- **Also**: decide the identity model deliberately — name+artist collapses remasters and
  deluxe editions. Document it in `ARCHITECTURE.md` § Database.

### 2.3 — Query logs committed to git and growing every commit ⚠️

- [ ] **Why**: `composeApp/logs/firebase_queries.log` and `spotify_queries.log` are
  tracked. The current uncommitted diff is **+250k lines**. `.git` is already ~22 MB.
  They contain user activity (playlist IDs, timestamps, album operations).
- **Fix**:
  1. Add `composeApp/logs/` to `.gitignore`.
  2. `git rm --cached composeApp/logs/*.log`.
  3. Rewrite history to purge them (`git filter-repo --path composeApp/logs --invert-paths`)
     — coordinate if anyone else has a clone.
  4. Point the file antilogs at a path outside the repo, or cap file size / rotate.

### 2.4 — Secrets committed / hardcoded

- [ ] **Why**: `composeApp/google-services.json` is tracked; the same Firebase
  `apiKey`, `applicationId`, `projectId`, `gcmSenderId` are hardcoded in
  `FirebaseDriver.desktop.kt`. Firebase config isn't a password, but once 2.1 is fixed
  the project should still not ship its config in source.
- **Fix**: `.gitignore` `google-services.json`, load desktop Firebase options from a
  local untracked properties file or env vars, document setup in `README.md` (it already
  half-does). `git rm --cached` + history rewrite alongside 2.3.

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

- [ ] **Where**: `repository/UserLibraryRepository.kt` `libraryRef()`,
  `repository/TagRepository.kt` `tagsCollection()`,
  `repository/AlbumTagRepository.kt` `tagsRef()` — all call
  `userSession.requireUserId()` which `throw`s. Reads already wait on `userIdFlow`.
  `LibraryService.initUserSession()` runs from `LibraryViewModel.init`, so an early
  rating/tag/library write on first login throws `IllegalStateException`.
- **Fix**: give `UserSessionRepository` a `suspend fun awaitUserId(): String` (first
  non-null from `userIdFlow`) and use it in the write paths. Or make session init a
  hard gate before the library UI renders.

### 3.2 — Read-modify-write without transactions (lost updates)

- [ ] **Where**: `UserLibraryRepository.addTagId` / `removeTagId` (get list → set list),
  and the `added_at` guard in `setInLibrary` (get doc → conditional set).
- **Fix**: `FieldValue.arrayUnion(tagId)` / `arrayRemove(tagId)` for tags.
  For `added_at`: write it only on create (`set(merge=false)` on first add) or use a
  transaction. Same for any other get-then-set.

### 3.3 — Rate-limit handling ignores `Retry-After`

- [ ] **Where**: `di/module/impl/ProductionNetworkModule.kt` — both HTTP clients read
  `Retry-After` only to log it, then use blind `exponentialDelay`. The generic client
  also retries **HTTP 403** as if it were rate-limiting (403 is auth/permission —
  retrying 3× is wrong).
- **Fix**: in `retryIf`/`delayMillis`, honour `Retry-After` (seconds or HTTP-date) when
  present. Remove `Forbidden` from the retry predicate on the generic client. Cap total
  retry wall-time so the UI coroutine can't be parked for minutes.

### 3.4 — `getAllAlbums()` reads the entire global `albums` collection

- [ ] **Where**: `repository/AlbumRepository.kt` `getAllAlbums()`, consumed by
  `getAllArtists`, `getEarliestReleaseDate`, and `service/LibraryService.getLibraryStats`.
  Deserializes every document in a shared collection on every subscription.
- **Fix**: derive artist list / earliest date / stats from the user's
  `library_albums` join (already available via `getAllAlbumsInLibrary()`), or maintain
  aggregate documents. Never subscribe to the unfiltered collection from the UI.

### 3.5 — HTTP client lifecycle

- [ ] **Where**: `ProductionNetworkModule` — `provideHttpClient()` lazy-init is not
  thread-safe (`httpClient ?: HttpClient(...)`); the **Spotify client is never closed**
  (`close()` only closes the misc client).
- **Fix**: build both clients eagerly in the constructor (or guard with a lock), keep
  references to both, close both in `close()`.

### 3.6 — Non-cryptographic RNG for PKCE verifier and OAuth `state`

- [ ] **Where**: `network/oauth/spotify/BaseAuthHandler.kt` `generateCodeVerifier()`,
  `DesktopAuthHandler.kt` `generateState()` — both use `charPool.random()`
  (→ `kotlin.random.Random.Default`).
- **Fix**: use a CSPRNG. Desktop: `java.security.SecureRandom`. Provide a
  `expect fun secureRandomBytes(n: Int): ByteArray` if it needs to stay in `commonMain`.

### 3.7 — CI runs no tests

- [ ] **Where**: `.github/workflows/*.yml` — only builds installers for 3 OSes on every
  PR. `desktopTest` never runs. No lint/detekt/ktlint. Android APK never built.
- **Fix**: add a `test` job (fast, `ubuntu-latest`) that runs
  `./gradlew :composeApp:desktopTest` + a static-analysis step, and make it a required
  status check. Only run the slow 3-OS installer matrix on tags.

---

## Section 4 — Medium (correctness)

- [ ] **4.1 `saveAlbum` overwrites `addedAt` on every write**
  (`AlbumRepository.kt:55,74`) — re-sync resets every album's "date added", breaking
  `SortOrder.DATE_ADDED`. Only set `addedAt` when the doc doesn't already exist.
- [ ] **4.2 Double event dispatch** — `AlbumRepository.fetchMultipleAlbums` dispatches
  `AlbumEvent.AlbumAdded` inside `saveAlbum` **and** again explicitly (line ~285), and
  calls `AlbumMapper.toDomain(albumDto)` twice per album. Dispatch once; map once.
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

- [ ] **5.1 Test bug** — `LibraryServiceTest."test apply sync, remote only"` calls
  `SyncAction.UseLocal`, not a remote-only action. Fix the action or the name.
- [ ] **5.2 Thin test coverage** — ~1,100 lines across 4 real test files for ~21k LOC.
  Priority additions: `generateAlbumId` (collisions), `AlbumMapper` round-trips,
  `SpotifyAuthRepository` token refresh/expiry, `PlaybackSessionManager` state machine,
  `CollectionsService`.
- [ ] **5.3 Duplicated docs** — `PRODUCTION_ROADMAP.md` and `MIGRATION_SPOTIFY_ID.md`
  exist byte-identical at repo root **and** in `docs/`. Keep one copy (in `docs/`),
  leave a stub/README link at root. Consolidate the 5 overlapping migration docs
  (`docs/DATABASE_MIGRATION_README.md`, `FIREBASE_MIGRATION.md`, `MIGRATION_GUIDE.md`,
  `MIGRATION_SPOTIFY_ID.md`) into one `docs/MIGRATIONS.md` with a section per migration.
- [ ] **5.4 Untracked clutter in the working tree** — a `records/` Python venv,
  `migration-reporter/`, and ~8 root `*.py` / `*.sh` scripts. Move maintenance scripts
  into `scripts/`, delete the venv, ensure it's `.gitignore`d.
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
- [ ] **5.8 `OpenAiApi.prompt`** logs the full prompt and full raw response at debug
  (`network/openAi/OpenAiApi.kt:46,60`). Truncate, or gate behind a verbose flag.
- [ ] **5.9 Stale doc comments** — `OpenAiApi` KDoc says default model `gpt-3.5-turbo`
  (actual: `gpt-4.1`); `SpotifyApi` comment references a non-existent
  `HttpClientProvider`.

---

## Progress log

| Date | Section | Item | Commit | Notes |
|------|---------|------|--------|-------|
| 2026-09-07 | 1 | 1.1 runBlocking | _uncommitted_ | Tag repos/service/VM/handler → suspend; tests → coEvery |
| 2026-09-07 | 1 | 1.4 throw in Flow | _uncommitted_ | `getAlbumById` → `Flow<Album?>`; `.filterNotNull()` in use case |
| 2026-09-07 | 1 | 1.5 System.currentTimeMillis | _uncommitted_ | 6 sites → `Clock.System` |
| 2026-09-07 | 1 | 1.6 default package | _uncommitted_ | 4 types moved; 2 dead files deleted |
| 2026-09-07 | 1 | 1.7 package ≠ dir | _uncommitted_ | AlbumTagRepository, CollectionDetailViewModel |
| 2026-09-07 | 1 | 1.8 ad-hoc scopes | _uncommitted_ | SettingsRepository eager load; eventScope.cancel() |
| 2026-09-07 | 1 | 1.9 debug noise | _uncommitted_ | ProductionNetworkModule + SearchResultComponents |
| 2026-09-07 | 1 | 1.11 unused koin | _uncommitted_ | removed from build.gradle.kts |

**Verification**: `./gradlew :composeApp:compileKotlinDesktop :composeApp:compileTestKotlinDesktop`
passes. `desktopTest` = 41 tests / 16 failing — **identical set to the pre-change
baseline** (all 16 are pre-existing: `AlbumRepositoryTest` firestore-mock serialization,
`LibraryServiceTest` 5.1 + mock gaps, `AlbumViewModelTest` `addAlbumToCollection` mock
gaps, `CollectionImportServiceTest` one assertion). No regressions introduced.

`compileDebugKotlinAndroid` fails — but it **also fails on a clean `git stash` of all this
work**, for unrelated reasons (see 1.12). Android was already broken.

**Not done in Section 1**: 1.2 (ViewModel factory — needs navigator work), 1.3 (error
handling — large, incremental), 1.10 (rest of version-catalog consolidation), 1.12
(Android build — pre-existing, newly documented).
