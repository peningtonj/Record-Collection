# AGENTS.md — Record Collection

Kotlin Multiplatform (Android + Desktop/JVM) app using Compose Multiplatform. Alternative Spotify front-end for album-centric listening.

## Architecture

Clean architecture with these layers (top → bottom): **UI → ViewModel → Service → Repository → Network/Database**

- All shared code lives in `composeApp/src/commonMain/kotlin/io/github/peningtonj/recordcollection/`
- Platform-specific code uses `expect`/`actual` in `androidMain/` and `desktopMain/`
- Desktop entry point: `desktopMain/kotlin/.../main.kt`; Android entry: `androidMain/.../MainActivity.kt`

Key package responsibilities:
- `service/` — multi-repository orchestration (`LibraryService`, `CollectionsService`, `TagService`, `PlaybackQueueService`)
- `repository/` — single data-source wrappers (DB + API ops, `Flow`-returning)
- `di/container/` + `di/module/` — manual DI; no Hilt/Koin used for wiring (despite koin being on classpath, `DependencyContainer` is injected via `CompositionLocal`)
- `events/` — `AlbumEventDispatcher` decouples album persistence from side effects (artist enrichment, genre tagging)
- `db/mapper/` — all DTO→domain mapping (`AlbumMapper`, `ArtistMapper`, etc.)

## Dependency Injection Pattern

`DependencyContainer` is provided via `LocalDependencyContainer` (a `CompositionLocal`). ViewModels are **not** obtained from a framework; they're constructed manually inside `remember { }` blocks in `ViewModelFactoryExtensions.kt`:

```kotlin
// Add a new rememberXxxViewModel() here when creating a new screen
val vm = rememberLibraryViewModel()   // pulls from LocalDependencyContainer.current
```

Adding a new dependency: add to `DependencyContainer` interface → implement in `ModularDependencyContainer` → wire in `DependencyContainerFactory.create()` (desktop) and the Android equivalent.

## Build & Run

```bash
# Run desktop app
./gradlew :composeApp:run

# Build Android APK
./gradlew :composeApp:assembleDebug

# Run all shared tests
./gradlew :composeApp:desktopTest

# Build desktop distributable
./gradlew :composeApp:createDistributable
```

Logging is split into three Napier antilog sinks at startup (`main.kt`): console (filtered), `logs/firebase_queries.log`, `logs/spotify_queries.log`.

## Testing Conventions

- Tests live in `commonTest/` and run on the JVM target (`desktopTest` task)
- Use `mockk<T>()` for mocking; `coEvery`/`coVerify` for suspending functions
- Use `runTest` (coroutines-test) for all coroutine-based tests
- Use `Turbine` (`.test { }`) for `Flow` assertions
- Test data built with factories in `testDataFactory/` (e.g. `TestAlbumDataFactory.album("id", "name")`)

```kotlin
// Standard test structure
class LibraryServiceTest {
    private val albumRepository = mockk<AlbumRepository>()
    @BeforeTest fun setup() { /* wire service */ }
    @Test fun `description`() = runTest { ... }
}
```

## Database (Firestore)

Persistence is entirely via **Firebase Firestore** (`dev.gitlive:firebase-firestore`). There is no SQLDelight; there are no `.sq` files.

- `FirebaseDriver` (`db/FirebaseDriver.kt` — `expect`/`actual`) initialises Firebase before the DI container is built
- `FirebaseModule` / `ProductionFirebaseModule` expose `Firebase.firestore`; every repository that needs persistence receives a `FirebaseFirestore` constructor parameter
- Collection names match domain entities: `"albums"`, `"artists"`, `"tracks"`, `"ratings"`, etc.
- Document ID for albums is a **hash of name + primary artist** (not the Spotify ID); `spotifyId` is a separate field
- Document models are `@Serializable` data classes (e.g. `AlbumDocument`, `ArtistDocument`) stored in `db/domain/`; they are separate from the domain objects (`Album`, `Artist`)
- Complex fields that would cause Firestore Int/Long type issues (`artists`, `images`, `externalIds`) are stored as **JSON-encoded strings** inside the document — use `AlbumMapper` / `ArtistMapper` to convert, never write raw strings
- Reads return `Flow` by subscribing to `.snapshots`; writes use `.set(document)` or `.set(map, merge = true)`
- `AlbumMapper.toDomain(AlbumDocument)` and `AlbumMapper.toDocument(Album)` are the canonical conversion points

## Network / API

- All HTTP via Ktor (`OkHttp` engine); configured with bearer-token auth, retry (max 3, exponential backoff), and rate-limit handling (429/503)
- Spotify base URL: `https://api.spotify.com/v1`; desktop OAuth redirects to `localhost:8888`
- Polling requests must set header `X-No-Retry: true` to bypass the retry plugin
- `PlaybackException` sealed class models Spotify playback errors (no active device, premium required, etc.)
- OpenAI model in use: **GPT-4.1** (`network/openAi/`)

## Conventions & Gotchas

- **Error handling is inconsistent** — some methods throw, some return `Result<T>`; prefer `Result<T>` for new code
- **Navigation** uses a custom `Screen` sealed class (`navigation/NavigationScreen.kt`) — add new screens there and in `NavigationHostComposable`
- `SharingStarted.WhileSubscribed(5000)` is the standard for all `StateFlow` in ViewModels
- The `AlbumEvent` system (`AlbumAdded`, `AlbumUpdated`, `AlbumDeleted`) must be dispatched after any album DB write — don't bypass it
- `FirebaseDriver.initializeFirebase()` must be called **before** `DependencyContainerFactory.create()` — it is called at the top of `DependencyContainerFactory.create()` on desktop; ensure the same on Android
- `gradle/libs.versions.toml` is the single source of truth for dependency versions

