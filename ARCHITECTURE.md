# Record Collection - Architecture Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture Layers](#architecture-layers)
3. [Technology Stack](#technology-stack)
4. [Project Structure](#project-structure)
5. [Core Components](#core-components)
6. [Data Flow](#data-flow)
7. [Database Schema](#database-schema)
8. [Network Layer](#network-layer)
9. [Dependency Injection](#dependency-injection)
10. [Testing Strategy](#testing-strategy)
11. [Areas Requiring Review/Completion](#areas-requiring-reviewcompletion)

---

## Overview

**Record Collection** is a Kotlin Multiplatform application that provides an alternative front-end to Spotify, specifically designed for listening to full albums. The application supports both Android and Desktop (JVM) platforms using Compose Multiplatform for the UI layer.

### Key Features
- Album-centric music library management
- Spotify API integration for playback and library sync
- Collections (playlists of albums)
- Album ratings and filtering
- Genre tagging via Every Noise At Once
- Release group management for alternate editions
- AI-powered collection import from articles (OpenAI)
- MusicBrainz integration for metadata

---

## Architecture Layers

The application follows a clean architecture pattern with clear separation of concerns:

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                          │
│  (Compose Multiplatform - Screens & Components)     │
└─────────────────────────────────────────────────────┘
                         ↕
┌─────────────────────────────────────────────────────┐
│                 ViewModel Layer                      │
│  (Business Logic, State Management, UI State)       │
└─────────────────────────────────────────────────────┘
                         ↕
┌─────────────────────────────────────────────────────┐
│                  Service Layer                       │
│  (Complex Business Logic, Orchestration)            │
└─────────────────────────────────────────────────────┘
                         ↕
┌─────────────────────────────────────────────────────┐
│              Use Case Layer (Optional)               │
│  (Single-responsibility business operations)         │
└─────────────────────────────────────────────────────┘
                         ↕
┌─────────────────────────────────────────────────────┐
│                Repository Layer                      │
│  (Data Access Abstraction, API/DB Operations)       │
└─────────────────────────────────────────────────────┘
                         ↕
┌──────────────────────┬──────────────────────────────┐
│   Network Layer      │      Database Layer          │
│ (API Clients)        │    (SQLDelight)              │
└──────────────────────┴──────────────────────────────┘
```

### Layer Responsibilities

#### 1. **UI Layer** (`ui/`)
- Compose components and screens
- User interaction handling
- State observation and rendering
- Platform-agnostic UI code

#### 2. **ViewModel Layer** (`viewmodel/`)
- UI state management
- User action handling
- Coroutine scope management
- Reactive data streams via StateFlow

#### 3. **Service Layer** (`service/`)
- Complex business logic orchestration
- Multi-repository coordination
- Domain-specific operations
- Examples: `LibraryService`, `CollectionsService`, `TagService`

#### 4. **Use Case Layer** (`usecase/`)
- Single-purpose business operations
- Examples: `GetAlbumDetailUseCase`, `ReleaseGroupUseCase`

#### 5. **Repository Layer** (`repository/`)
- Data source abstraction
- API and database operations
- Data mapping (DTO → Domain)
- Reactive data streams

#### 6. **Network Layer** (`network/`)
- HTTP client configuration
- API endpoint definitions
- Authentication handling
- Request/response serialization

#### 7. **Database Layer** (`db/`)
- SQLDelight schema definitions
- Database queries
- Type adapters
- Database driver abstraction

---

## Technology Stack

### Core Technologies
- **Kotlin Multiplatform**: Code sharing across platforms
- **Compose Multiplatform 1.8.1**: UI framework
- **Kotlin 2.1.21**: Programming language
- **Coroutines 1.10.2**: Asynchronous programming

### Data & Persistence
- **SQLDelight 2.0.1**: Type-safe SQL database
- **Kotlinx Serialization**: JSON serialization
- **Kotlinx Datetime 0.6.0**: Date/time handling

### Networking
- **Ktor 3.0.3**: HTTP client
  - Client: OkHttp (Android/Desktop)
  - Content Negotiation
  - Auth Plugin (Bearer tokens)
  - Retry logic with exponential backoff

### Dependency Management
- **Custom DI Container**: Modular dependency injection
- No third-party DI framework (manual constructor injection)

### Testing
- **MockK 1.13.8**: Mocking framework
- **Turbine 1.0.0**: Flow testing
- **JUnit 4**: Test runner

### External APIs
- **Spotify Web API**: Music data and playback
- **OpenAI API**: AI-powered article parsing
- **MusicBrainz API**: Music metadata
- **Every Noise At Once**: Genre data

### Platform-Specific
- **Android**: Minimum SDK 24, Target SDK 35
- **Desktop**: JVM/Swing integration
- **Napier 2.7.1**: Multiplatform logging

---

## Project Structure

```
composeApp/src/
├── androidMain/           # Android-specific implementations
│   └── kotlin/
│       ├── Platform.android.kt
│       ├── MainActivity.kt
│       ├── db/DatabaseDriver.android.kt
│       └── network/oauth/AndroidAuthHandler.kt
│
├── desktopMain/          # Desktop-specific implementations
│   └── kotlin/
│       ├── main.kt
│       ├── Platform.jvm.kt
│       ├── db/DatabaseDriver.desktop.kt
│       ├── navigation/DesktopNavigator.kt
│       └── network/oauth/DesktopAuthHandler.kt
│
├── commonMain/           # Shared code
│   ├── kotlin/
│   │   └── io/github/peningtonj/recordcollection/
│   │       ├── App.kt                    # App entry point
│   │       ├── db/                       # Database layer
│   │       │   ├── DatabaseDriver.kt
│   │       │   ├── DatabaseHelper.kt
│   │       │   ├── domain/               # Domain models
│   │       │   └── mapper/               # DTO → Domain mappers
│   │       ├── di/                       # Dependency injection
│   │       │   ├── container/
│   │       │   └── module/
│   │       ├── events/                   # Event-driven architecture
│   │       │   ├── AlbumEvent.kt
│   │       │   ├── AlbumEventDispatcher.kt
│   │       │   └── handlers/
│   │       ├── migration/                # Database migration utilities
│   │       ├── navigation/               # Navigation system
│   │       ├── network/                  # API clients
│   │       │   ├── spotify/
│   │       │   ├── openAi/
│   │       │   ├── miscApi/
│   │       │   └── oauth/
│   │       ├── repository/               # Data repositories
│   │       ├── service/                  # Business logic services
│   │       ├── ui/                       # UI components
│   │       │   ├── components/
│   │       │   └── screens/
│   │       ├── usecase/                  # Use cases
│   │       ├── util/                     # Utilities
│   │       └── viewmodel/                # ViewModels
│   │
│   └── sqldelight/                      # SQL schema
│       └── io/github/peningtonj/recordcollection/db/
│           ├── Albums.sq
│           ├── Artists.sq
│           ├── Tracks.sq
│           ├── Ratings.sq
│           ├── AlbumCollections.sq
│           ├── CollectionAlbums.sq
│           ├── CollectionFolders.sq
│           ├── Tags.sq
│           ├── AlbumTags.sq
│           ├── Auths.sq
│           ├── Profiles.sq
│           ├── CollectionFilter.sq
│           └── migrations/
│
└── commonTest/           # Shared tests
    └── kotlin/
        ├── testDataFactory/
        └── [test files]
```

---

## Core Components

### 1. Dependency Injection (`di/`)

**Pattern**: Manual constructor injection with modular containers

```
DependencyContainer (interface)
    ├── ModularDependencyContainer (implementation)
    │
    └── Modules:
        ├── DatabaseModule
        ├── NetworkModule
        ├── RepositoryModule
        ├── UseCaseModule
        ├── EventModule
        └── SettingsModule
```

**Key Features**:
- Lazy initialization for expensive resources
- Modular design allows easy testing
- Platform-specific implementations injected at app startup

**Example**:
```kotlin
class ModularDependencyContainer(
    private val networkModule: NetworkModule,
    private val databaseModule: DatabaseModule,
    private val repositoryModule: RepositoryModule,
    override val authHandler: AuthHandler,
    private val useCaseModule: UseCaseModule,
    private val eventModule: EventModule,
    private val settingsModule: SettingsModule
) : DependencyContainer
```

### 2. Event System (`events/`)

**Pattern**: Event dispatcher with multiple handlers

The application uses an event-driven architecture for album-related operations:

```
AlbumEvent (sealed class)
    ├── AlbumAdded
    └── AlbumUpdated

AlbumEventDispatcher
    └── handlers: List<AlbumEventHandler>
        └── AlbumProcessingHandler
            ├── Enriches artist data
            ├── Fetches genres
            └── Updates metadata
```

**Purpose**: Decouple album persistence from side effects like artist enrichment and genre tagging.

### 3. Navigation (`navigation/`)

**Pattern**: Custom navigation system with platform-specific implementations

```
Navigator (interface)
    ├── DesktopNavigator (desktop)
    └── [AndroidNavigator - not shown in code]

NavigationScreen (sealed class)
    ├── Screen.Library
    ├── Screen.Album(albumId)
    ├── Screen.Artist(artistId)
    ├── Screen.Collection(name)
    ├── Screen.Search
    ├── Screen.Settings
    └── Screen.Profile
```

**Features**:
- Back navigation support
- Type-safe screen parameters
- Platform-specific back stack management

### 4. Settings Management (`repository/SettingsRepository.kt`)

**Pattern**: Settings stored in multiplatform-settings library

```kotlin
data class AppSettings(
    val defaultSortOrder: SortOrder = SortOrder.DATE_ADDED,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val playCollectionSound: Boolean = true,
    val openAiApiKey: String = ""
)
```

---

## Data Flow

### Example: Loading Library Screen

```
1. LibraryScreen (UI)
   ↓ observes
2. LibraryViewModel.filteredAlbums (StateFlow)
   ↓ combines
3. LibraryService.getFilteredAlbums()
   ↓ uses
4. AlbumRepository.getAllAlbumsInLibrary() (Flow)
   ↓ queries
5. Database (SQLDelight)
   ↓ maps
6. AlbumMapper.toDomain()
   ↓ enriches with
7. ArtistRepository.getAllArtists() (genres)
   RatingRepository.getAllRatings()
   ↓ returns
8. Flow<List<AlbumDisplayData>>
```

### Example: Adding Album from Spotify

```
1. User clicks "Add Album"
   ↓
2. AlbumDetailViewModel.addToLibrary()
   ↓
3. AlbumRepository.addToLibrary(albumId)
   ↓
4. spotifyApi.library.getAlbum(albumId)
   ↓
5. AlbumMapper.toDomain(dto)
   ↓
6. Database insert
   ↓
7. AlbumEventDispatcher.dispatch(AlbumAdded)
   ↓
8. AlbumProcessingHandler.handle()
   ├── Fetch artist data
   ├── Fetch genres (Every Noise)
   └── Save to database
```

---

## Database Schema

### Tables (12 total)

#### 1. **albums**
Primary table for album metadata.

```sql
CREATE TABLE albums (
    id TEXT NOT NULL PRIMARY KEY,           -- Spotify album ID
    name TEXT NOT NULL,
    primary_artist TEXT NOT NULL,
    artists TEXT NOT NULL,                   -- JSON array of artist IDs
    release_date TEXT,
    total_tracks INTEGER NOT NULL,
    spotify_uri TEXT NOT NULL,
    added_at TEXT NOT NULL,
    album_type TEXT NOT NULL,                -- album, single, compilation
    images TEXT NOT NULL,                    -- JSON array
    updated_at INTEGER NOT NULL,
    external_ids TEXT,                       -- JSON (ISRC, UPC, etc.)
    release_group_id TEXT,                   -- MusicBrainz release group
    in_library INTEGER NOT NULL              -- Boolean flag
);
```

**Key Queries**:
- `selectAllAlbumsInLibrary`: Get user's library
- `getAlbumsByArtist`: Artist filtering
- `selectAlbumsByReleaseId`: Alternate editions
- `updateInLibraryStatus`: Add/remove from library

#### 2. **artists**
Artist metadata with genre information.

```sql
CREATE TABLE artists (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    spotify_uri TEXT NOT NULL,
    external_urls TEXT NOT NULL,
    images TEXT NOT NULL,
    popularity INTEGER NOT NULL,
    followers INTEGER NOT NULL,
    genres TEXT NOT NULL,                    -- JSON array (custom adapter)
    every_noise_genre TEXT
);
```

**Custom Adapter**: Handles JSON serialization for `genres` list.

#### 3. **tracks**
Track metadata for album contents.

```sql
CREATE TABLE tracks (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    album_id TEXT NOT NULL,
    artists TEXT NOT NULL,                   -- JSON array
    duration_ms INTEGER NOT NULL,
    track_number INTEGER NOT NULL,
    disc_number INTEGER NOT NULL,
    spotify_uri TEXT NOT NULL,
    FOREIGN KEY (album_id) REFERENCES albums(id)
);
```

#### 4. **album_ratings**
User ratings for albums (0-5 stars).

```sql
CREATE TABLE album_ratings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    album_id TEXT NOT NULL,
    rating INTEGER NOT NULL,
    rated_at INTEGER NOT NULL,
    FOREIGN KEY (album_id) REFERENCES albums(id),
    UNIQUE(album_id)                         -- One rating per album
);
```

#### 5. **album_collections**
Collection (playlist) definitions.

```sql
CREATE TABLE album_collections (
    name TEXT NOT NULL PRIMARY KEY,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    folder TEXT
);
```

#### 6. **collection_albums**
Many-to-many relationship between collections and albums.

```sql
CREATE TABLE collection_albums (
    collection_name TEXT NOT NULL,
    album_id TEXT NOT NULL,
    position INTEGER NOT NULL,
    added_at TEXT NOT NULL,
    PRIMARY KEY (collection_name, album_id),
    FOREIGN KEY (collection_name) REFERENCES album_collections(name) ON DELETE CASCADE,
    FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE CASCADE
);
```

**Features**:
- Ordered albums via `position`
- Cascade delete when collection removed

#### 7. **collection_folders**
Hierarchical organization of collections.

```sql
CREATE TABLE collection_folders (
    folder_name TEXT NOT NULL PRIMARY KEY,
    created_at TEXT NOT NULL,
    parent_folder TEXT,
    FOREIGN KEY (parent_folder) REFERENCES collection_folders(folder_name)
);
```

#### 8. **tags**
User-defined tags for categorization.

```sql
CREATE TABLE tags (
    name TEXT NOT NULL PRIMARY KEY,
    category TEXT,                           -- genre, mood, style, etc.
    created_at TEXT NOT NULL
);
```

#### 9. **album_tags**
Many-to-many relationship for album tagging.

```sql
CREATE TABLE album_tags (
    album_id TEXT NOT NULL,
    tag_name TEXT NOT NULL,
    PRIMARY KEY (album_id, tag_name),
    FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_name) REFERENCES tags(name) ON DELETE CASCADE
);
```

#### 10. **auths**
OAuth token storage.

```sql
CREATE TABLE auths (
    id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
    access_token TEXT NOT NULL,
    token_type TEXT NOT NULL,
    scope TEXT NOT NULL,
    expires_in INTEGER NOT NULL,
    refresh_token TEXT NOT NULL,
    expires_at INTEGER NOT NULL
);
```

**Note**: Single-row approach (id=1) for current user session.

#### 11. **profiles**
User profile information from Spotify.

```sql
CREATE TABLE profiles (
    id TEXT NOT NULL PRIMARY KEY,
    display_name TEXT NOT NULL,
    email TEXT NOT NULL,
    product TEXT,                            -- premium, free
    images TEXT,                             -- JSON array
    country TEXT,
    followers INTEGER
);
```

#### 12. **collection_filter**
Saved filter state for collections.

```sql
CREATE TABLE collection_filter (
    collection_name TEXT NOT NULL PRIMARY KEY,
    artist_filter TEXT,
    genre_filter TEXT,
    rating_filter INTEGER,
    start_date TEXT,
    end_date TEXT,
    text_filter TEXT,
    FOREIGN KEY (collection_name) REFERENCES album_collections(name) ON DELETE CASCADE
);
```

### Database Migrations

Location: `composeApp/src/commonMain/sqldelight/migrations/`

- `1.sqm`: Initial schema
- `2.sqm`: Schema updates

**Migration Utility**: `DatabaseMigrationUtil.kt` provides manual migration support for backup/restore operations.

---

## Network Layer

### Architecture

```
HttpClient (Ktor)
    ├── Plugins:
    │   ├── ContentNegotiation (JSON)
    │   ├── HttpRequestRetry
    │   ├── Auth (Bearer tokens)
    │   └── HttpResponseValidator
    │
    └── API Clients:
        ├── SpotifyApi
        │   ├── LibraryApi
        │   ├── UserApi
        │   ├── PlaybackApi
        │   ├── SearchApi
        │   └── PlaylistAlbumExtractor
        ├── OpenAiApi
        └── MiscApi (MusicBrainz, Every Noise)
```

### Spotify API (`network/spotify/`)

**Base URL**: `https://api.spotify.com/v1`

#### LibraryApi
- `getUserSavedAlbums()`: Fetch user's saved albums
- `getAlbum(albumId)`: Get album details
- `getAlbumTracks(albumId)`: Get album tracks
- `saveAlbumsToLibrary(albumIds)`: Add albums to library
- `removeAlbumsFromLibrary(albumIds)`: Remove from library

#### PlaybackApi
- `getPlaybackState()`: Current playback status
- `play(request)`: Start/resume playback
- `pause()`: Pause playback
- `toggleShuffle(state)`: Shuffle control
- `addToQueue(uri)`: Add track to queue

**Error Handling**:
```kotlin
sealed class PlaybackException : Exception() {
    class NoActiveDevice : PlaybackException()
    class NoActivePlayback : PlaybackException()
    class PremiumRequired : PlaybackException()
    data class UnexpectedError(val code: Int, val body: String) : PlaybackException()
}
```

#### SearchApi
- `search(query, types, limit)`: Universal search

#### UserApi
- `getCurrentUserProfile()`: Get user profile

### OpenAI API (`network/openAi/`)

**Purpose**: Parse articles to extract album lists

**Key Methods**:
- `getUrlContent(url)`: Fetch and parse article using Readability4J
- `prompt(prompt, apiKey)`: Send prompt to GPT model
- `isApiKeyValid(apiKey)`: Validate API key

**Model**: GPT-4.1

### MusicBrainz API (`network/miscApi/`)

**Purpose**: Metadata enrichment and release group management

**Key Methods**:
- `getReleaseGroup(releaseGroupId)`: Get release group details
- `searchAlbum(query)`: Search for albums
- Artist genre data from Every Noise At Once

### Authentication (`network/oauth/spotify/`)

**OAuth 2.0 Flow**:

```
1. User initiates login
   ↓
2. AuthHandler.authenticate()
   ├── Desktop: Opens browser, starts local server
   └── Android: Opens Custom Tab
   ↓
3. User authorizes in Spotify
   ↓
4. Redirect to app with authorization code
   ↓
5. AuthHandler.exchangeCodeForToken(code)
   ↓
6. SpotifyAuthRepository.saveToken()
   ↓
7. Token stored in database
   ↓
8. HttpClient Auth plugin handles token refresh
```

**Platform-Specific**:
- **Desktop**: `DesktopAuthHandler` - Local server on port 8888
- **Android**: `AndroidAuthHandler` - Custom Tab with app link

**Token Management**:
```kotlin
interface AuthHandler {
    suspend fun authenticate(): Result<String>
    suspend fun exchangeCodeForToken(code: String): Result<AccessToken>
    suspend fun refreshToken(refreshToken: String): Result<AccessToken>
}
```

### Retry Logic & Rate Limiting

**Configuration**:
```kotlin
install(HttpRequestRetry) {
    maxRetries = 3
    retryOnServerErrors(maxRetries)
    exponentialDelay(base = 2.0, maxDelayMs = 60_000)
    
    retryIf { request, response ->
        response.status == HttpStatusCode.TooManyRequests ||
        response.status == HttpStatusCode.ServiceUnavailable
    }
}
```

**Features**:
- Rate limit detection (429, 503)
- Exponential backoff
- Respects `Retry-After` header
- Polling requests bypass retry (marked with `X-No-Retry: true`)

---

## Dependency Injection

### Container Structure

```kotlin
interface DependencyContainer {
    val authHandler: AuthHandler
    val authRepository: SpotifyAuthRepository
    val albumRepository: AlbumRepository
    val artistRepository: ArtistRepository
    val playbackRepository: PlaybackRepository
    val profileRepository: ProfileRepository
    val playlistRepository: PlaylistRepository
    val openAiApi: OpenAiApi
    // ... other dependencies
}
```

### Module Pattern

Each module provides specific dependencies:

**DatabaseModule**:
```kotlin
interface DatabaseModule {
    fun provideDatabase(): RecordCollectionDatabase
}
```

**NetworkModule**:
```kotlin
interface NetworkModule {
    fun provideHttpClient(): HttpClient
    fun provideSpotifyApi(authRepository: SpotifyAuthRepository): SpotifyApi
    fun provideMiscApi(): MiscApi
    fun provideOpenAiApi(): OpenAiApi
}
```

**RepositoryModule**:
```kotlin
interface RepositoryModule {
    fun provideAlbumRepository(
        database: RecordCollectionDatabase,
        miscApi: MiscApi,
        spotifyApi: SpotifyApi,
        eventDispatcher: AlbumEventDispatcher
    ): AlbumRepository
    // ... other repositories
}
```

### Production Implementation

Located in: `di/module/impl/`

- `ProductionDatabaseModule`
- `ProductionNetworkModule`
- `ProductionRepositoryModule`
- `ProductionUseCaseModule`
- `ProductionEventModule`

### Factory

```kotlin
object DependencyContainerFactory {
    fun create(): DependencyContainer {
        val databaseModule = ProductionDatabaseModule()
        val authHandler = createPlatformAuthHandler()
        val networkModule = ProductionNetworkModule()
        // ... initialize other modules
        
        return ModularDependencyContainer(
            networkModule = networkModule,
            databaseModule = databaseModule,
            repositoryModule = repositoryModule,
            authHandler = authHandler,
            useCaseModule = useCaseModule,
            eventModule = eventModule,
            settingsModule = settingsModule
        )
    }
}
```

**Platform-specific injection** handled via `expect/actual` pattern:
```kotlin
expect fun createPlatformAuthHandler(): AuthHandler
```

---

## Testing Strategy

### Test Structure

```
commonTest/
├── kotlin/
│   ├── testDataFactory/          # Test data builders
│   │   ├── TestAlbumDataFactory
│   │   ├── TestTrackDataFactory
│   │   └── TestImageDataFactory
│   ├── service/                  # Service tests
│   │   ├── LibraryServiceTest
│   │   └── CollectionImportServiceTest
│   ├── repository/               # Repository tests
│   │   └── AlbumRepositoryTest
│   └── viewmodel/                # ViewModel tests
│       └── AlbumViewModelTest
```

### Testing Tools

**MockK**: Mocking framework
```kotlin
@Test
fun `fetchAlbum returns mapped album on success`() = runTest {
    val mockLibraryApi = mockk<LibraryApi>()
    coEvery { mockLibraryApi.getAlbum("test-album-id") } returns Result.success(testAlbumDto)
    // ... assertions
}
```

**Turbine**: Flow testing
```kotlin
viewModel.uiState.test {
    assertEquals(LoadingState, awaitItem())
    assertEquals(SuccessState(data), awaitItem())
}
```

**Coroutines Test**: `runTest` for coroutine testing

### Test Data Factories

Pattern: Builder pattern for test data

```kotlin
object TestAlbumDataFactory {
    fun createAlbumDto(
        id: String = "test-id",
        name: String = "Test Album",
        // ... with defaults
    ): AlbumDto = AlbumDto(...)
}
```

---

## Areas Requiring Review/Completion

### 🔴 High Priority

#### 1. **Error Handling Standardization**
**Location**: Throughout repository and network layers

**Issue**: Inconsistent error handling patterns
- Some methods throw exceptions
- Some return `Result<T>`
- UI error state management varies

**Recommendation**:
```kotlin
// Standardize on Result<T> for operations that can fail
suspend fun fetchAlbum(id: String): Result<Album> = runCatching {
    // operation
}

// Use sealed class for domain errors
sealed class DomainError {
    data class NetworkError(val message: String) : DomainError()
    data class AuthError(val message: String) : DomainError()
    data class NotFound(val id: String) : DomainError()
}
```

**Files to Review**:
- `repository/AlbumRepository.kt` (line 168: `fetchAlbum` throws)
- `repository/ArtistRepository.kt` (line 155: `fetchAlbumArtists` throws)
- `network/spotify/PlaybackApi.kt` (custom exception types)

#### 2. **Database Migration Strategy**
**Location**: `migration/DatabaseMigrationUtil.kt`

**Issue**: Manual migration utility exists but SQLDelight migrations not fully utilized

**Current State**:
```kotlin
class DatabaseMigration {
    fun migrateDatabases(backupDbPath: String, targetDbPath: String, specificTables: List<String> = emptyList()) {
        // Manual table-by-table migration
    }
}
```

**Recommendation**:
- Leverage SQLDelight's migration system more fully
- Add migration tests
- Document migration strategy for production releases
- Consider adding schema version tracking

**Files to Review**:
- `composeApp/src/commonMain/sqldelight/migrations/*.sqm`
- `migration/DatabaseMigrationUtil.kt`

#### 3. **Authentication Token Expiry Handling**
**Location**: `repository/SpotifyAuthRepository.kt`, `network` module

**Current State**: Token refresh is implemented but edge cases need review

**Potential Issues**:
- Multiple concurrent requests triggering refresh
- Token refresh failure recovery
- Logout behavior when refresh fails

**Recommendation**:
```kotlin
// Add mutex for token refresh
private val refreshMutex = Mutex()

suspend fun ensureValidToken(): Result<AccessToken> {
    refreshMutex.withLock {
        // Check again after acquiring lock
        val currentToken = getStoredToken()
        if (currentToken != null && !currentToken.isExpired()) {
            return Result.success(currentToken.toAccessToken())
        }
        // Proceed with refresh
    }
}
```

**Files to Review**:
- `repository/SpotifyAuthRepository.kt` (lines 37-60)
- `di/module/impl/ProductionNetworkModule.kt` (lines 226-248)

#### 4. **Import Statement Issues**
**Location**: `viewmodel/LibraryViewModel.kt` (line 18)

**Issue**: Incorrect import
```kotlin
import jdk.jfr.internal.OldObjectSample.emit  // ❌ Wrong import
```

**Fix**: Remove unused import

### 🟡 Medium Priority

#### 5. **Incomplete Implementation: DatabaseMigrationUtil**
**Location**: `migration/DatabaseMigrationUtil.kt`

**Issue**: File appears truncated (lines 1-57 shown, but looks incomplete)

**Recommendation**: Review and complete the migration utility implementation

#### 6. **Platform-Specific Navigator Implementations**
**Location**: `navigation/`

**Current State**:
- `DesktopNavigator` implemented
- Android navigator not visible in codebase

**Recommendation**: Verify Android navigation implementation exists and matches desktop functionality

#### 7. **Test Coverage**
**Location**: `commonTest/`

**Current State**: Some tests exist but coverage appears limited

**Missing Test Areas**:
- ViewModels (only `AlbumViewModelTest` found)
- Services (limited coverage)
- Use cases (not tested)
- UI components (no UI tests found)
- Integration tests

**Recommendation**:
- Add ViewModel tests for all ViewModels
- Add service layer tests
- Consider UI testing with Compose UI testing library
- Add integration tests for critical flows

#### 8. **Settings Persistence**
**Location**: `repository/SettingsRepository.kt`

**Recommendation**: Review settings synchronization and ensure proper persistence across app restarts

#### 9. **Collection Import Service Robustness**
**Location**: `service/CollectionImportService.kt`

**Areas to Review**:
- Error handling for malformed URLs
- OpenAI API key validation before usage
- Parsing failures for article content
- Rate limiting for batch operations

#### 10. **Playback Queue Management**
**Location**: `service/PlaybackQueueService.kt`

**Areas to Review**:
- Queue state persistence
- Shuffle implementation for collections
- Sound effect insertion between albums
- Queue synchronization with Spotify state

### 🟢 Low Priority / Nice to Have

#### 11. **Logging Strategy**
**Current State**: Using Napier for logging

**Recommendation**:
- Standardize log levels (d, i, w, e)
- Consider log redaction for sensitive data (tokens, user info)
- Add structured logging for analytics

#### 12. **Performance Optimization**
**Areas to Review**:
- Database query optimization (consider indexes)
- Image loading and caching strategy
- Large list rendering optimization
- Coroutine dispatcher strategy

#### 13. **Code Organization**
**Observations**:
- Some large files (e.g., `LibraryService.kt` - 338 lines)
- Consider splitting into smaller, focused classes

**Example**: `LibraryService` could be split into:
- `LibraryQueryService` (reading data)
- `LibrarySyncService` (syncing with Spotify)
- `LibraryStatisticsService` (stats calculations)

#### 14. **Documentation**
**Missing**:
- KDoc comments for public APIs
- README sections for:
  - Development setup
  - Building the project
  - Testing guidelines
  - Contributing guidelines

#### 15. **Release Group Management Edge Cases**
**Location**: `usecase/ReleaseGroupUseCase.kt`

**Review**:
- Handling of albums without MusicBrainz data
- Multiple release groups matching
- Fallback behavior

### 🔵 Future Enhancements

#### 16. **Offline Support**
**Recommendation**: Consider implementing offline mode for:
- Cached album data
- Offline playback queue
- Sync when connection restored

#### 17. **Analytics & Crash Reporting**
**Recommendation**: Add analytics and crash reporting
- User behavior tracking (opt-in)
- Crash reporting (Firebase, Sentry, etc.)
- Performance monitoring

#### 18. **Accessibility**
**Recommendation**: Review and enhance accessibility features
- Screen reader support
- Keyboard navigation (desktop)
- Content descriptions

#### 19. **Localization**
**Recommendation**: Add multi-language support
- String resources
- Date/time formatting
- Number formatting

---

## Summary

### Architecture Strengths
✅ Clear separation of concerns  
✅ Reactive data flow with Kotlin Flows  
✅ Type-safe database with SQLDelight  
✅ Multiplatform code sharing  
✅ Modular dependency injection  
✅ Event-driven architecture for side effects  

### Key Areas for Improvement
⚠️ Error handling standardization  
⚠️ Database migration strategy  
⚠️ Token refresh synchronization  
⚠️ Test coverage expansion  
⚠️ Documentation completeness  

### Architectural Patterns Used
- **Clean Architecture**: Layered separation
- **Repository Pattern**: Data access abstraction
- **MVVM**: ViewModel + UI state
- **Event-Driven**: Album processing events
- **Dependency Injection**: Modular containers
- **Mapper Pattern**: DTO → Domain conversion
- **Flow-based Reactive**: StateFlow for UI state

---

**Document Version**: 1.0  
**Last Updated**: 2026-01-27  
**Maintainer**: Architecture documentation generated from codebase analysis
