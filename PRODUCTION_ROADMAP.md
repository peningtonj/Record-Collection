# Record Collection - Production Roadmap

**Last Updated**: 2026-01-27  
**Status**: Development → Production-Ready  
**Timeline**: 12-16 weeks

---

## Executive Summary

This roadmap outlines the path to transform Record Collection from a functional prototype to a production-ready application. The plan addresses stability, scalability, user experience, and deployment infrastructure needs.

### Current State Assessment
✅ **Working Features**: Core functionality operational  
⚠️ **Bugs & Stability**: Multiple error handling issues  
⚠️ **Android**: Authentication not implemented  
⚠️ **Infrastructure**: Local-only, no cloud services  
⚠️ **Testing**: Minimal coverage  
⚠️ **Monitoring**: No production observability  

### Target State
🎯 **Stable Application**: Robust error handling and recovery  
🎯 **Multi-Platform**: Android fully supported  
🎯 **Cloud-Enabled**: Optional cloud sync and backup  
🎯 **Observable**: Comprehensive logging and monitoring  
🎯 **Tested**: 80%+ code coverage  
🎯 **Scalable**: Ready for user growth  

---

## Table of Contents

1. [Phase 1: Critical Bugs & Stability (Weeks 1-3)](#phase-1-critical-bugs--stability)
2. [Phase 2: Architecture Refactoring (Weeks 4-6)](#phase-2-architecture-refactoring)
3. [Phase 3: Platform Completeness (Weeks 7-9)](#phase-3-platform-completeness)
4. [Phase 4: Cloud Infrastructure (Weeks 10-12)](#phase-4-cloud-infrastructure)
5. [Phase 5: Production Readiness (Weeks 13-16)](#phase-5-production-readiness)
6. [Cloud Infrastructure Plan](#cloud-infrastructure-plan)
7. [Architecture Recommendations](#architecture-recommendations)
8. [Success Metrics](#success-metrics)

---

## Phase 1: Critical Bugs & Stability
**Duration**: 3 weeks  
**Goal**: Fix immediate bugs, standardize error handling

### Week 1: Fix Critical Bugs

#### 1.1 Fix Import Errors ⚠️ CRITICAL
**Issue**: Invalid import in `LibraryViewModel.kt` line 18
```kotlin
import jdk.jfr.internal.OldObjectSample.emit  // ❌ Wrong import
```

**Fix**: Remove unused import
```kotlin
// Remove this line entirely
```

**Impact**: Prevents compilation on non-Oracle JVMs

#### 1.2 Fix Android Authentication 🔴 BLOCKER
**Location**: `androidMain/kotlin/.../AndroidAuthHandler.kt`

**Current State**:
```kotlin
override suspend fun authenticate(): Result<String> {
    // TODO: Implement Android-specific authentication
    TODO("Implement Android authentication flow")
}
```

**Implementation Plan**:
1. Use Custom Tabs for OAuth flow
2. Handle deep link callback via `AndroidManifest.xml`
3. Implement secure credential storage with EncryptedSharedPreferences
4. Test on multiple Android versions (API 24-35)

**Code Structure**:
```kotlin
class AndroidAuthHandler(
    private val context: Context,
    client: HttpClient
) : BaseAuthHandler(client) {
    
    override suspend fun authenticate(): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            val authUrl = buildAuthorizationUrl()
            
            // Launch Custom Tab
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(context, Uri.parse(authUrl))
            
            // Register deep link receiver
            AuthCallbackReceiver.register(context) { code ->
                continuation.resume(Result.success(code))
            }
        }
    }
    
    override fun getRedirectUri(): String = 
        "recordcollection://callback"
}
```

**Files to Create/Modify**:
- `AndroidAuthHandler.kt` - Complete implementation
- `AndroidManifest.xml` - Add deep link intent filter
- `build.gradle.kts` - Add security-crypto dependency
- Test on physical device and emulator

#### 1.3 Fix Error Throwing in Use Cases
**Issue**: `GetAlbumDetailUseCase.kt` line 94
```kotlin
throw (Error("Album not found"))  // ❌ Using Error class
```

**Fix**: Use domain exception
```kotlin
throw AlbumNotFoundException(albumId)
```

**Create domain exceptions**:
```kotlin
// util/DomainExceptions.kt
sealed class DomainException(message: String) : Exception(message) {
    class AlbumNotFoundException(val albumId: String) : 
        DomainException("Album not found: $albumId")
    class ArtistNotFoundException(val artistId: String) : 
        DomainException("Artist not found: $artistId")
    class PlaybackException(message: String) : 
        DomainException(message)
    class AuthenticationException(message: String) : 
        DomainException(message)
}
```

### Week 2: Standardize Error Handling

#### 2.1 Repository Layer Error Handling
**Current Issues**:
- Inconsistent use of `Result<T>` vs exceptions
- `AlbumRepository.fetchAlbum()` throws exceptions
- `ArtistRepository.fetchAlbumArtists()` throws exceptions

**Standard Pattern**:
```kotlin
// Use Result<T> for all operations that can fail
suspend fun fetchAlbum(id: String): Result<Album> = runCatching {
    val dto = spotifyApi.library.getAlbum(id)
        .getOrElse { error -> 
            return Result.failure(
                NetworkException("Failed to fetch album", error)
            )
        }
    AlbumMapper.toDomain(dto)
}

// Never throw in repository layer - always return Result
```

**Files to Refactor**:
- `repository/AlbumRepository.kt` (lines 165-210)
- `repository/ArtistRepository.kt` (lines 135-160)
- `repository/PlaylistRepository.kt`
- `repository/ProfileRepository.kt`

#### 2.2 ViewModel Error State Management
**Current Issues**:
- Inconsistent error state across ViewModels
- Some don't handle errors at all
- No user-friendly error messages

**Standard Pattern**:
```kotlin
// Standardized UI State
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(
        val message: String,
        val type: ErrorType,
        val canRetry: Boolean = true
    ) : UiState<Nothing>
}

enum class ErrorType {
    NETWORK,
    AUTHENTICATION,
    NOT_FOUND,
    PERMISSION_DENIED,
    UNKNOWN
}

// In ViewModel
private val _uiState = MutableStateFlow<UiState<List<Album>>>(UiState.Loading)
val uiState: StateFlow<UiState<List<Album>>> = _uiState.asStateFlow()

fun loadData() {
    viewModelScope.launch {
        _uiState.value = UiState.Loading
        libraryService.getAlbums()
            .onSuccess { albums ->
                _uiState.value = UiState.Success(albums)
            }
            .onFailure { error ->
                _uiState.value = UiState.Error(
                    message = error.toUserFriendlyMessage(),
                    type = error.toErrorType(),
                    canRetry = error.isRetryable()
                )
            }
    }
}
```

**Files to Update**:
- All ViewModels in `viewmodel/` directory
- Create `ui/state/UiState.kt`
- Create `util/ErrorMapper.kt` for user-friendly messages

#### 2.3 Network Error Recovery
**Current Issue**: Rate limiting and network failures not gracefully handled

**Enhancements**:
```kotlin
// di/module/impl/ProductionNetworkModule.kt
install(HttpRequestRetry) {
    maxRetries = 3
    exponentialDelay(base = 2.0, maxDelayMs = 60_000)
    
    // Categorize errors for better handling
    retryOnExceptionIf { request, cause ->
        when (cause) {
            is TimeoutCancellationException,
            is SocketTimeoutException,
            is UnknownHostException -> true
            else -> false
        }
    }
    
    retryIf(maxRetries) { request, response ->
        when (response.status.value) {
            429 -> {
                // Rate limited - respect Retry-After
                val retryAfter = response.headers["Retry-After"]
                    ?.toLongOrNull() ?: 60
                delay(retryAfter * 1000)
                true
            }
            in 500..599 -> true  // Server errors
            else -> false
        }
    }
}

// Add circuit breaker pattern
class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val resetTimeout: Duration = 60.seconds
) {
    private var failureCount = 0
    private var lastFailureTime: Instant? = null
    private var state = State.CLOSED
    
    enum class State { CLOSED, OPEN, HALF_OPEN }
    
    suspend fun <T> execute(block: suspend () -> T): Result<T> {
        if (state == State.OPEN) {
            val now = Clock.System.now()
            if (now - lastFailureTime!! < resetTimeout) {
                return Result.failure(
                    CircuitBreakerOpenException()
                )
            }
            state = State.HALF_OPEN
        }
        
        return runCatching { block() }
            .onSuccess { 
                failureCount = 0
                state = State.CLOSED 
            }
            .onFailure {
                failureCount++
                lastFailureTime = Clock.System.now()
                if (failureCount >= failureThreshold) {
                    state = State.OPEN
                }
            }
    }
}
```

### Week 3: Database Stability

#### 3.1 Add Database Migrations
**Current Issue**: Manual migration utility, no versioning

**Solution**: Proper SQLDelight migration system
```sql
-- migrations/3.sqm
-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_albums_artist ON albums(primary_artist);
CREATE INDEX IF NOT EXISTS idx_albums_release_date ON albums(release_date);
CREATE INDEX IF NOT EXISTS idx_albums_library ON albums(in_library);
CREATE INDEX IF NOT EXISTS idx_collection_albums_collection ON collection_albums(collection_name);
CREATE INDEX IF NOT EXISTS idx_album_tags_album ON album_tags(album_id);

-- Add created_at/updated_at to tables missing them
ALTER TABLE tracks ADD COLUMN created_at INTEGER DEFAULT 0;
ALTER TABLE artists ADD COLUMN updated_at INTEGER DEFAULT 0;
```

**Migration Testing**:
```kotlin
@Test
fun `test migration from version 2 to 3`() {
    // Use SQLDelight migration testing utilities
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    RecordCollectionDatabase.Schema.migrate(driver, 2, 3)
    
    // Verify migration success
    val database = RecordCollectionDatabase(driver)
    // Assert indexes exist
}
```

#### 3.2 Add Database Constraints
**Enhancements**:
```sql
-- Add foreign key constraints (already mostly done)
-- Add check constraints for data validity

ALTER TABLE album_ratings ADD CONSTRAINT rating_range 
    CHECK (rating >= 0 AND rating <= 5);

ALTER TABLE collection_albums ADD CONSTRAINT position_positive
    CHECK (position >= 0);
```

#### 3.3 Database Connection Pooling
**For Desktop app**:
```kotlin
// db/DatabaseDriver.desktop.kt
class DatabaseDriver {
    private val connectionPool = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:$databasePath"
            maximumPoolSize = 5
            connectionTimeout = 30000
        }
    )
    
    fun createDriver(): SqlDriver {
        return connectionPool.connection.asDriver()
    }
}
```

### Phase 1 Deliverables
- ✅ All critical bugs fixed
- ✅ Android authentication working
- ✅ Standardized error handling across app
- ✅ Database migrations and constraints
- ✅ Improved network resilience
- ✅ Unit tests for error scenarios

---

## Phase 2: Architecture Refactoring
**Duration**: 3 weeks  
**Goal**: Improve code organization, testability, scalability

### Week 4: Repository Pattern Enhancement

#### 4.1 Introduce Domain Layer
**Current**: Direct mapping from DTO → Domain in repositories  
**Problem**: Business logic mixed with data access  
**Solution**: Separate domain layer

```
New Structure:
repository/ (data access only)
    ├── AlbumRepositoryImpl.kt
    └── interfaces/
        └── AlbumRepository.kt (interface)

domain/ (new layer)
    ├── usecase/
    │   ├── album/
    │   │   ├── GetAlbumsUseCase.kt
    │   │   ├── AddAlbumUseCase.kt
    │   │   └── SyncAlbumsUseCase.kt
    │   └── collection/
    │       ├── CreateCollectionUseCase.kt
    │       └── PlayCollectionUseCase.kt
    ├── model/ (pure domain models)
    └── repository/ (repository interfaces)
```

**Benefits**:
- Clear separation of concerns
- Easier testing (mock interfaces)
- Single Responsibility Principle
- Reusable business logic

**Example**:
```kotlin
// domain/repository/AlbumRepository.kt
interface AlbumRepository {
    suspend fun getAlbum(id: String): Result<Album>
    suspend fun saveAlbum(album: Album): Result<Unit>
    fun observeAlbums(): Flow<List<Album>>
}

// data/repository/AlbumRepositoryImpl.kt
class AlbumRepositoryImpl(
    private val database: RecordCollectionDatabase,
    private val spotifyApi: SpotifyApi
) : AlbumRepository {
    override suspend fun getAlbum(id: String): Result<Album> = 
        // Implementation
}

// domain/usecase/album/AddAlbumUseCase.kt
class AddAlbumUseCase(
    private val albumRepository: AlbumRepository,
    private val eventDispatcher: AlbumEventDispatcher
) {
    suspend operator fun invoke(albumId: String): Result<Album> {
        // Business logic here
        return albumRepository.getAlbum(albumId)
            .onSuccess { album ->
                albumRepository.saveAlbum(album)
                eventDispatcher.dispatch(AlbumAdded(album))
            }
    }
}
```

#### 4.2 Improve Service Layer
**Current Issue**: Large service classes with mixed responsibilities

**Refactor**:
```kotlin
// Before: LibraryService.kt (338 lines)
class LibraryService(
    // 7 dependencies
) {
    fun getAllAlbumsEnriched() { }
    fun getFilteredAlbums() { }
    fun getLibraryStats() { }
    suspend fun syncWithSpotify() { }
    suspend fun handleSyncConflict() { }
    // ... many more methods
}

// After: Split into focused services
class LibraryQueryService(
    private val albumRepository: AlbumRepository
) {
    fun getAllAlbums(): Flow<List<Album>> { }
    fun getFilteredAlbums(filter: AlbumFilter): Flow<List<Album>> { }
}

class LibrarySyncService(
    private val albumRepository: AlbumRepository,
    private val spotifyApi: SpotifyApi
) {
    suspend fun syncWithSpotify(strategy: SyncStrategy): Result<SyncResult> { }
    suspend fun resolveConflict(conflict: SyncConflict): Result<Unit> { }
}

class LibraryStatisticsService(
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository
) {
    fun getStats(): Flow<LibraryStats> { }
    fun getListeningTrends(): Flow<TrendData> { }
}
```

### Week 5: State Management & Data Flow

#### 5.1 Implement Unidirectional Data Flow (UDF)
**Goal**: Make state changes predictable and testable

```kotlin
// Standardize on MVI-style architecture

// State
data class LibraryState(
    val albums: List<Album> = emptyList(),
    val filter: AlbumFilter = AlbumFilter.Default,
    val isLoading: Boolean = false,
    val error: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Idle
)

// Intent (User actions)
sealed interface LibraryIntent {
    data object LoadAlbums : LibraryIntent
    data class FilterChanged(val filter: AlbumFilter) : LibraryIntent
    data object SyncWithSpotify : LibraryIntent
    data class AddAlbum(val albumId: String) : LibraryIntent
}

// ViewModel as state machine
class LibraryViewModel(
    private val getAlbumsUseCase: GetAlbumsUseCase,
    private val syncLibraryUseCase: SyncLibraryUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()
    
    fun handleIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.LoadAlbums -> loadAlbums()
            is LibraryIntent.FilterChanged -> updateFilter(intent.filter)
            is LibraryIntent.SyncWithSpotify -> syncWithSpotify()
            is LibraryIntent.AddAlbum -> addAlbum(intent.albumId)
        }
    }
    
    private fun loadAlbums() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getAlbumsUseCase()
                .onSuccess { albums ->
                    _state.update { it.copy(
                        albums = albums,
                        isLoading = false,
                        error = null
                    )}
                }
                .onFailure { error ->
                    _state.update { it.copy(
                        isLoading = false,
                        error = error.message
                    )}
                }
        }
    }
}
```

#### 5.2 Add State Persistence
**Goal**: Restore app state after process death

```kotlin
// Create SavedStateHandle integration
class LibraryViewModel(
    private val savedStateHandle: SavedStateHandle,
    // ... other dependencies
) : ViewModel() {
    
    private val _state = MutableStateFlow(
        savedStateHandle.get<LibraryState>("library_state") 
            ?: LibraryState()
    )
    
    init {
        viewModelScope.launch {
            _state.collect { state ->
                savedStateHandle["library_state"] = state
            }
        }
    }
}

// Make domain models Parcelable/Serializable
@Serializable
@Parcelize
data class Album(
    val id: String,
    val name: String,
    // ...
) : Parcelable
```

### Week 6: Testing Infrastructure

#### 6.1 Unit Test Coverage
**Goal**: 80%+ coverage for business logic

**Test Structure**:
```
commonTest/
├── domain/
│   └── usecase/
│       ├── GetAlbumsUseCaseTest.kt
│       ├── AddAlbumUseCaseTest.kt
│       └── SyncLibraryUseCaseTest.kt
├── repository/
│   ├── AlbumRepositoryTest.kt
│   └── fake/
│       └── FakeAlbumRepository.kt
├── viewmodel/
│   ├── LibraryViewModelTest.kt
│   └── AlbumDetailViewModelTest.kt
└── service/
    ├── LibraryQueryServiceTest.kt
    └── LibrarySyncServiceTest.kt
```

**Example Test**:
```kotlin
class GetAlbumsUseCaseTest {
    private lateinit var useCase: GetAlbumsUseCase
    private lateinit var fakeRepository: FakeAlbumRepository
    
    @Before
    fun setup() {
        fakeRepository = FakeAlbumRepository()
        useCase = GetAlbumsUseCase(fakeRepository)
    }
    
    @Test
    fun `returns albums from repository`() = runTest {
        // Given
        val expectedAlbums = listOf(
            TestAlbumFactory.create(id = "1"),
            TestAlbumFactory.create(id = "2")
        )
        fakeRepository.setAlbums(expectedAlbums)
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedAlbums, result.getOrNull())
    }
    
    @Test
    fun `returns error when repository fails`() = runTest {
        // Given
        fakeRepository.setError(NetworkException("No connection"))
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }
}

// Fake Repository for testing
class FakeAlbumRepository : AlbumRepository {
    private var albums = listOf<Album>()
    private var error: Exception? = null
    
    fun setAlbums(albums: List<Album>) {
        this.albums = albums
        this.error = null
    }
    
    fun setError(error: Exception) {
        this.error = error
    }
    
    override suspend fun getAlbums(): Result<List<Album>> {
        return error?.let { Result.failure(it) }
            ?: Result.success(albums)
    }
}
```

#### 6.2 Integration Tests
**Goal**: Test complete flows

```kotlin
@Test
fun `adding album triggers artist enrichment`() = runTest {
    // Given
    val testDb = createInMemoryDatabase()
    val testApi = FakeSpotifyApi()
    val repository = AlbumRepositoryImpl(testDb, testApi)
    val eventDispatcher = TestEventDispatcher()
    
    // When
    repository.addAlbum("album-id")
    
    // Then
    advanceUntilIdle()  // Let coroutines complete
    
    val savedAlbum = testDb.albumsQueries.getAlbumById("album-id").executeAsOne()
    assertNotNull(savedAlbum)
    
    val events = eventDispatcher.capturedEvents
    assertEquals(1, events.size)
    assertTrue(events[0] is AlbumEvent.AlbumAdded)
}
```

#### 6.3 UI Tests (Compose)
**Goal**: Test user interactions

```kotlin
@Test
fun `clicking album navigates to detail screen`() {
    composeTestRule.setContent {
        LibraryScreen(
            viewModel = testViewModel,
            navigator = testNavigator
        )
    }
    
    // When
    composeTestRule
        .onNodeWithText("Test Album")
        .performClick()
    
    // Then
    verify(testNavigator).navigateTo(Screen.Album("album-id"))
}

@Test
fun `shows error message when loading fails`() {
    // Given
    val errorViewModel = LibraryViewModel(
        getAlbumsUseCase = AlwaysFailingGetAlbumsUseCase()
    )
    
    composeTestRule.setContent {
        LibraryScreen(viewModel = errorViewModel)
    }
    
    // Then
    composeTestRule
        .onNodeWithText("Failed to load albums")
        .assertIsDisplayed()
    
    composeTestRule
        .onNodeWithText("Retry")
        .assertIsDisplayed()
}
```

### Phase 2 Deliverables
- ✅ Clean architecture with domain layer
- ✅ Refactored service classes
- ✅ Unidirectional data flow
- ✅ State persistence
- ✅ 80%+ test coverage
- ✅ CI/CD pipeline with tests

---

## Phase 3: Platform Completeness
**Duration**: 3 weeks  
**Goal**: Feature parity and platform optimization

### Week 7: Android Platform

#### 7.1 Android-Specific Features
**Background sync**:
```kotlin
class AlbumSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val syncService = // Get from DI
            syncService.syncWithSpotify()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

// Schedule periodic sync
WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork(
        "album-sync",
        ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<AlbumSyncWorker>(
            repeatInterval = 6,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).build()
    )
```

**Media notification**:
```kotlin
class PlaybackNotificationManager(
    private val context: Context
) {
    fun showNowPlaying(track: Track) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(track.name)
            .setContentText(track.artists.joinToString { it.name })
            .setSmallIcon(R.drawable.ic_music_note)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken))
            .addAction(R.drawable.ic_skip_previous, "Previous", 
                previousPendingIntent)
            .addAction(R.drawable.ic_pause, "Pause", 
                pausePendingIntent)
            .addAction(R.drawable.ic_skip_next, "Next", 
                nextPendingIntent)
            .build()
        
        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID, notification)
    }
}
```

**Widget support**:
```kotlin
class LibraryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            LibraryWidgetContent()
        }
    }
}

@Composable
fun LibraryWidgetContent() {
    Column {
        Text("Recently Added")
        // Show recent albums
    }
}
```

#### 7.2 Android Optimization
**Image loading**:
```kotlin
// Use Coil with caching
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(album.imageUrl)
        .memoryCacheKey(album.id)
        .diskCacheKey(album.id)
        .crossfade(true)
        .build(),
    contentDescription = album.name
)
```

**Database optimization**:
```kotlin
// Use Room for Android (better than SQLDelight for Android-only features)
// Or optimize SQLDelight queries
@Query("SELECT * FROM albums WHERE in_library = 1 ORDER BY added_at DESC LIMIT :limit")
fun getRecentAlbums(limit: Int): Flow<List<Album>>

// Add indexes in migration
CREATE INDEX idx_albums_added_library ON albums(added_at DESC, in_library)
WHERE in_library = 1;
```

### Week 8: Desktop Platform

#### 8.1 Desktop-Specific Features
**System tray integration**:
```kotlin
fun setupSystemTray(
    window: ComposeWindow,
    playbackViewModel: PlaybackViewModel
) {
    if (!SystemTray.isSupported()) return
    
    val tray = SystemTray.getSystemTray()
    val image = Toolkit.getDefaultToolkit()
        .getImage("icon.png")
    
    val popup = PopupMenu()
    popup.add(MenuItem("Play/Pause").apply {
        addActionListener { 
            playbackViewModel.togglePlayback()
        }
    })
    popup.add(MenuItem("Next Track").apply {
        addActionListener { 
            playbackViewModel.nextTrack()
        }
    })
    popup.addSeparator()
    popup.add(MenuItem("Show Window").apply {
        addActionListener { 
            window.isVisible = true
            window.toFront()
        }
    })
    popup.add(MenuItem("Exit").apply {
        addActionListener { 
            exitProcess(0)
        }
    })
    
    val trayIcon = TrayIcon(image, "Record Collection", popup)
    tray.add(trayIcon)
}
```

**Keyboard shortcuts**:
```kotlin
@Composable
fun RecordCollectionApp() {
    val playbackViewModel = rememberPlaybackViewModel()
    
    LaunchedEffect(Unit) {
        Window.current.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when {
                    e.isControlDown && e.keyCode == KeyEvent.VK_P ->
                        playbackViewModel.togglePlayback()
                    e.isControlDown && e.keyCode == KeyEvent.VK_RIGHT ->
                        playbackViewModel.nextTrack()
                    e.isControlDown && e.keyCode == KeyEvent.VK_LEFT ->
                        playbackViewModel.previousTrack()
                    e.keyCode == KeyEvent.VK_SPACE ->
                        playbackViewModel.togglePlayback()
                }
            }
        })
    }
    
    // ... app content
}
```

**Menu bar**:
```kotlin
@Composable
fun RecordCollectionMenuBar(
    onNewCollection: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit
) {
    MenuBar {
        Menu("File") {
            Item("New Collection", onClick = onNewCollection)
            Separator()
            Item("Import", onClick = onImport)
            Item("Export", onClick = onExport)
            Separator()
            Item("Settings", onClick = onSettings)
        }
        Menu("Edit") {
            Item("Preferences", onClick = onSettings)
        }
        Menu("View") {
            CheckboxItem("Show Sidebar", checked = true, onCheckedChange = {})
        }
    }
}
```

#### 8.2 Desktop Optimization
**Memory management**:
```kotlin
// Implement pagination for large lists
@Composable
fun AlbumGrid(albums: List<Album>) {
    val lazyGridState = rememberLazyGridState()
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        state = lazyGridState
    ) {
        items(
            count = albums.size,
            key = { albums[it].id }
        ) { index ->
            AlbumTile(albums[index])
        }
    }
}
```

### Week 9: Cross-Platform Polish

#### 9.1 Platform Abstraction
**Expect/Actual pattern for platform features**:
```kotlin
// commonMain
expect class FileManager {
    suspend fun saveFile(name: String, content: ByteArray): Result<String>
    suspend fun loadFile(path: String): Result<ByteArray>
    fun getDefaultDirectory(): String
}

// androidMain
actual class FileManager(private val context: Context) {
    actual suspend fun saveFile(name: String, content: ByteArray): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.getExternalFilesDir(null), name)
                file.writeBytes(content)
                Result.success(file.absolutePath)
            } catch (e: IOException) {
                Result.failure(e)
            }
        }
    }
}

// desktopMain
actual class FileManager {
    actual suspend fun saveFile(name: String, content: ByteArray): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(getDefaultDirectory(), name)
                file.writeBytes(content)
                Result.success(file.absolutePath)
            } catch (e: IOException) {
                Result.failure(e)
            }
        }
    }
}
```

#### 9.2 Adaptive UI
**Responsive layouts**:
```kotlin
@Composable
fun RecordCollectionApp() {
    val windowSizeClass = calculateWindowSizeClass()
    
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> CompactLayout()
        WindowWidthSizeClass.Medium -> MediumLayout()
        WindowWidthSizeClass.Expanded -> ExpandedLayout()
    }
}

@Composable
fun ExpandedLayout() {
    Row {
        NavigationRail(Modifier.width(80.dp)) {
            // Navigation items
        }
        LibraryContent(Modifier.weight(1f))
        DetailPanel(Modifier.width(400.dp))
    }
}
```

### Phase 3 Deliverables
- ✅ Android fully functional
- ✅ Platform-specific features
- ✅ Optimized performance per platform
- ✅ Adaptive UI
- ✅ System integration (notifications, tray, etc.)

---

## Phase 4: Cloud Infrastructure
**Duration**: 3 weeks  
**Goal**: Enable cloud sync, backup, and analytics

### Week 10: Backend Infrastructure Setup

#### 10.1 Cloud Provider Selection
**Recommendation**: **Firebase + Cloud Run hybrid**

**Why Firebase**:
- ✅ Easy authentication integration
- ✅ Real-time database for sync
- ✅ File storage for backups
- ✅ Built-in analytics
- ✅ Free tier generous enough
- ✅ No server management

**Why Cloud Run**:
- ✅ For custom API endpoints
- ✅ Kotlin/JVM backend possible
- ✅ Serverless (pay per use)
- ✅ Auto-scaling

**Architecture**:
```
┌─────────────┐
│  Client App │
└──────┬──────┘
       │
       ├────────────┐
       │            │
       ▼            ▼
┌──────────┐  ┌─────────────┐
│ Firebase │  │  Cloud Run  │
│          │  │   (Custom   │
│ - Auth   │  │    API)     │
│ - Firestore│ │           │
│ - Storage│  │ - Advanced  │
│ - Analytics│ │   queries   │
└──────────┘  │ - Background│
              │   jobs      │
              └─────────────┘
                     │
                     ▼
              ┌─────────────┐
              │  Cloud SQL  │
              │  (Optional) │
              └─────────────┘
```

#### 10.2 Firebase Setup
**`google-services.json` / `GoogleService-Info.plist`**:
```kotlin
// build.gradle.kts
plugins {
    id("com.google.gms.google-services") version "4.4.0"
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
}
```

**Authentication integration**:
```kotlin
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val spotifyAuthRepository: SpotifyAuthRepository
) {
    suspend fun signInWithSpotify(spotifyToken: String): Result<User> {
        return suspendCancellableCoroutine { continuation ->
            // Create custom token from backend
            val customToken = exchangeSpotifyTokenForFirebaseToken(spotifyToken)
            
            firebaseAuth.signInWithCustomToken(customToken)
                .addOnSuccessListener { result ->
                    val user = result.user?.let { firebaseUser ->
                        User(
                            id = firebaseUser.uid,
                            email = firebaseUser.email ?: ""
                        )
                    }
                    continuation.resume(Result.success(user!!))
                }
                .addOnFailureListener { error ->
                    continuation.resume(Result.failure(error))
                }
        }
    }
}
```

#### 10.3 Firestore Schema Design
```
users/
  {userId}/
    profile/
      spotifyId: string
      displayName: string
      email: string
      premium: boolean
      createdAt: timestamp
      
    library/
      albums/
        {albumId}/
          spotifyId: string
          addedAt: timestamp
          rating: number
          playCount: number
          lastPlayed: timestamp
          
    collections/
      {collectionId}/
        name: string
        createdAt: timestamp
        updatedAt: timestamp
        folder: string?
        albums: array<albumId>
        
    settings/
      theme: string
      defaultSort: string
      syncEnabled: boolean
      
sync_metadata/
  lastSyncAt: timestamp
  version: number
  deviceId: string
```

**Security Rules**:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null 
                        && request.auth.uid == userId;
    }
    
    match /sync_metadata/{userId} {
      allow read: if request.auth != null 
                  && request.auth.uid == userId;
      allow write: if request.auth != null 
                   && request.auth.uid == userId
                   && request.resource.data.version > resource.data.version;
    }
  }
}
```

### Week 11: Sync Implementation

#### 11.1 Sync Strategy
**Two-way sync with conflict resolution**:
```kotlin
class CloudSyncService(
    private val firestore: FirebaseFirestore,
    private val localDatabase: RecordCollectionDatabase,
    private val syncMetadataRepository: SyncMetadataRepository
) {
    suspend fun sync(): Result<SyncResult> {
        return try {
            val userId = getCurrentUserId()
            val lastSync = syncMetadataRepository.getLastSyncTime()
            
            // 1. Get remote changes since last sync
            val remoteChanges = getRemoteChanges(userId, lastSync)
            
            // 2. Get local changes since last sync
            val localChanges = getLocalChanges(lastSync)
            
            // 3. Resolve conflicts
            val resolved = resolveConflicts(remoteChanges, localChanges)
            
            // 4. Apply remote changes locally
            applyRemoteChanges(resolved.remoteWins)
            
            // 5. Push local changes to remote
            pushLocalChanges(resolved.localWins)
            
            // 6. Update sync metadata
            syncMetadataRepository.updateLastSync(Clock.System.now())
            
            Result.success(SyncResult(
                itemsSynced = resolved.total,
                conflicts = resolved.conflicts.size
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun resolveConflicts(
        remote: List<Album>,
        local: List<Album>
    ): ResolvedChanges {
        val conflicts = mutableListOf<SyncConflict>()
        val remoteWins = mutableListOf<Album>()
        val localWins = mutableListOf<Album>()
        
        remote.forEach { remoteAlbum ->
            val localAlbum = local.find { it.id == remoteAlbum.id }
            
            if (localAlbum != null) {
                // Conflict - both modified
                when (resolveConflict(remoteAlbum, localAlbum)) {
                    ConflictResolution.REMOTE -> remoteWins.add(remoteAlbum)
                    ConflictResolution.LOCAL -> localWins.add(localAlbum)
                    ConflictResolution.MERGE -> {
                        val merged = mergeAlbums(remoteAlbum, localAlbum)
                        remoteWins.add(merged)
                    }
                    ConflictResolution.ASK_USER -> {
                        conflicts.add(SyncConflict(remoteAlbum, localAlbum))
                    }
                }
            } else {
                // Only in remote - apply
                remoteWins.add(remoteAlbum)
            }
        }
        
        return ResolvedChanges(remoteWins, localWins, conflicts)
    }
    
    private fun resolveConflict(
        remote: Album,
        local: Album
    ): ConflictResolution {
        // Strategy: Last-write-wins
        return if (remote.updatedAt > local.updatedAt) {
            ConflictResolution.REMOTE
        } else {
            ConflictResolution.LOCAL
        }
    }
}

enum class ConflictResolution {
    REMOTE, LOCAL, MERGE, ASK_USER
}
```

#### 11.2 Offline-First Architecture
```kotlin
class OfflineFirstAlbumRepository(
    private val localDatabase: RecordCollectionDatabase,
    private val cloudFirestore: FirebaseFirestore,
    private val syncQueue: SyncQueue
) : AlbumRepository {
    
    override suspend fun addAlbum(album: Album): Result<Unit> {
        // 1. Save locally immediately
        localDatabase.albumsQueries.insert(album.toEntity())
        
        // 2. Queue for cloud sync
        syncQueue.enqueue(SyncOperation.AddAlbum(album))
        
        // 3. Try to sync in background
        syncInBackground()
        
        return Result.success(Unit)
    }
    
    override fun observeAlbums(): Flow<List<Album>> {
        // Always read from local database
        return localDatabase.albumsQueries
            .selectAllAlbumsInLibrary()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map(::toDomain) }
    }
    
    private fun syncInBackground() {
        scope.launch {
            if (networkMonitor.isOnline()) {
                processSyncQueue()
            }
        }
    }
    
    private suspend fun processSyncQueue() {
        syncQueue.getPendingOperations().forEach { operation ->
            try {
                when (operation) {
                    is SyncOperation.AddAlbum -> 
                        pushAlbumToCloud(operation.album)
                    is SyncOperation.UpdateAlbum -> 
                        updateAlbumInCloud(operation.album)
                    is SyncOperation.DeleteAlbum -> 
                        deleteAlbumFromCloud(operation.albumId)
                }
                syncQueue.markComplete(operation)
            } catch (e: Exception) {
                syncQueue.markFailed(operation, e)
            }
        }
    }
}
```

### Week 12: Cloud Features

#### 12.1 Cloud Backup
```kotlin
class CloudBackupService(
    private val storage: FirebaseStorage,
    private val database: RecordCollectionDatabase
) {
    suspend fun backupDatabase(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Export database to file
                val dbFile = database.export()
                
                // 2. Compress
                val compressed = compress(dbFile)
                
                // 3. Upload to Cloud Storage
                val timestamp = Clock.System.now()
                val path = "backups/$userId/${timestamp.toEpochMilliseconds()}.db.gz"
                
                val uploadTask = storage.reference.child(path)
                    .putBytes(compressed)
                    .await()
                
                // 4. Save metadata
                val metadata = BackupMetadata(
                    path = path,
                    timestamp = timestamp,
                    size = compressed.size,
                    deviceId = getDeviceId()
                )
                saveBackupMetadata(metadata)
                
                Result.success(path)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun restoreFromBackup(backupPath: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Download from Cloud Storage
                val compressed = storage.reference.child(backupPath)
                    .getBytes(MAX_DOWNLOAD_SIZE)
                    .await()
                
                // 2. Decompress
                val dbFile = decompress(compressed)
                
                // 3. Restore database
                database.restore(dbFile)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Auto-backup on changes
    fun enableAutoBackup() {
        scope.launch {
            database.observeChanges()
                .debounce(5.minutes)
                .collect {
                    backupDatabase()
                }
        }
    }
}
```

#### 12.2 Analytics & Monitoring
```kotlin
class AnalyticsService(
    private val analytics: FirebaseAnalytics
) {
    fun trackAlbumPlayed(album: Album) {
        analytics.logEvent("album_played") {
            param("album_id", album.id)
            param("album_name", album.name)
            param("artist", album.primaryArtist)
            param("genre", album.genres.firstOrNull() ?: "unknown")
        }
    }
    
    fun trackCollectionCreated(collection: AlbumCollection) {
        analytics.logEvent("collection_created") {
            param("collection_name", collection.name)
            param("album_count", collection.albumCount.toLong())
        }
    }
    
    fun trackSync(result: SyncResult) {
        analytics.logEvent("sync_completed") {
            param("items_synced", result.itemsSynced.toLong())
            param("conflicts", result.conflicts.toLong())
            param("duration_ms", result.durationMs)
        }
    }
}

// Privacy-focused: All analytics opt-in
class PrivacySettings(
    private val analytics: FirebaseAnalytics
) {
    fun setAnalyticsEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }
}
```

### Phase 4 Deliverables
- ✅ Firebase infrastructure set up
- ✅ Two-way sync implemented
- ✅ Offline-first architecture
- ✅ Cloud backup/restore
- ✅ Analytics (opt-in)
- ✅ Security rules in place

---

## Phase 5: Production Readiness
**Duration**: 4 weeks  
**Goal**: Polish, monitoring, deployment

### Week 13: Observability

#### 13.1 Logging Strategy
```kotlin
// Structured logging with log levels
object AppLogger {
    private val logger = Logger.withTag("RecordCollection")
    
    fun d(message: String, metadata: Map<String, Any> = emptyMap()) {
        logger.d { formatLog(message, metadata) }
    }
    
    fun w(message: String, error: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        logger.w(error) { formatLog(message, metadata) }
    }
    
    fun e(message: String, error: Throwable, metadata: Map<String, Any> = emptyMap()) {
        logger.e(error) { formatLog(message, metadata) }
        
        // Send to crash reporting
        if (BuildConfig.RELEASE) {
            CrashReporter.recordException(error, metadata)
        }
    }
    
    private fun formatLog(message: String, metadata: Map<String, Any>): String {
        return if (metadata.isEmpty()) {
            message
        } else {
            "$message | ${metadata.entries.joinToString { "${it.key}=${it.value}" }}"
        }
    }
}

// Usage
AppLogger.e(
    message = "Failed to fetch album",
    error = exception,
    metadata = mapOf(
        "album_id" to albumId,
        "user_id" to userId,
        "retry_count" to retryCount
    )
)
```

#### 13.2 Crash Reporting
```kotlin
// Firebase Crashlytics integration
dependencies {
    implementation("com.google.firebase:firebase-crashlytics-ktx")
}

class CrashReporter {
    private val crashlytics = FirebaseCrashlytics.getInstance()
    
    fun initialize() {
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }
    
    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }
    
    fun recordException(
        exception: Throwable,
        metadata: Map<String, Any> = emptyMap()
    ) {
        metadata.forEach { (key, value) ->
            crashlytics.setCustomKey(key, value.toString())
        }
        crashlytics.recordException(exception)
    }
    
    fun logBreadcrumb(message: String) {
        crashlytics.log(message)
    }
}

// Automatic crash boundary for Compose
@Composable
fun CrashBoundary(
    fallback: @Composable (Throwable) -> Unit = { ErrorScreen(it) },
    content: @Composable () -> Unit
) {
    var error by remember { mutableStateOf<Throwable?>(null) }
    
    if (error != null) {
        LaunchedEffect(error) {
            CrashReporter.recordException(error!!)
        }
        fallback(error!!)
    } else {
        CompositionLocalProvider(
            LocalOnErrorCallback provides { throwable ->
                error = throwable
            }
        ) {
            content()
        }
    }
}
```

#### 13.3 Performance Monitoring
```kotlin
// Firebase Performance Monitoring
class PerformanceMonitor {
    private val performance = FirebasePerformance.getInstance()
    
    fun traceSync(block: suspend () -> Unit) {
        val trace = performance.newTrace("sync_operation")
        trace.start()
        
        try {
            runBlocking { block() }
            trace.incrementMetric("success", 1)
        } catch (e: Exception) {
            trace.incrementMetric("failure", 1)
            throw e
        } finally {
            trace.stop()
        }
    }
    
    fun traceAlbumLoad(albumId: String, block: suspend () -> Unit) {
        val trace = performance.newTrace("album_load")
        trace.putAttribute("album_id", albumId)
        trace.start()
        
        try {
            runBlocking { block() }
        } finally {
            trace.stop()
        }
    }
}

// Custom metrics
class AppMetrics {
    fun recordSyncDuration(duration: Duration) {
        performance.getHttpMetric(
            url = "sync",
            httpMethod = "POST"
        ).apply {
            setRequestPayloadSize(0)
            setResponsePayloadSize(0)
            setHttpResponseCode(200)
            start()
            // Simulate duration
            Thread.sleep(duration.inWholeMilliseconds)
            stop()
        }
    }
}
```

### Week 14: Security Hardening

#### 14.1 Secure Credential Storage
```kotlin
// Android: EncryptedSharedPreferences
class SecureCredentialStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveToken(token: AccessToken) {
        encryptedPrefs.edit {
            putString("access_token", token.accessToken)
            putString("refresh_token", token.refreshToken)
            putLong("expires_at", token.expiresAt)
        }
    }
    
    fun getToken(): AccessToken? {
        val accessToken = encryptedPrefs.getString("access_token", null)
        val refreshToken = encryptedPrefs.getString("refresh_token", null)
        val expiresAt = encryptedPrefs.getLong("expires_at", 0)
        
        return if (accessToken != null && refreshToken != null) {
            AccessToken(accessToken, refreshToken, expiresAt)
        } else {
            null
        }
    }
}

// Desktop: KeyStore integration
actual class SecureCredentialStore {
    private val keyStore = KeyStore.getInstance("JCEKS")
    
    init {
        val keyStorePath = Paths.get(
            System.getProperty("user.home"),
            ".recordcollection",
            "keystore.jks"
        )
        
        if (keyStorePath.exists()) {
            keyStore.load(keyStorePath.inputStream(), PASSWORD)
        } else {
            keyStore.load(null, PASSWORD)
        }
    }
    
    actual fun saveToken(token: AccessToken) {
        // Store encrypted
    }
}
```

#### 14.2 API Key Protection
```kotlin
// Don't hardcode API keys - use BuildConfig
// build.gradle.kts
android {
    defaultConfig {
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"${System.getenv("SPOTIFY_CLIENT_ID")}\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"${System.getenv("SPOTIFY_CLIENT_SECRET")}\"")
    }
}

// ProGuard rules to obfuscate
-keep class io.github.peningtonj.recordcollection.BuildConfig { *; }
-keepclassmembers class io.github.peningtonj.recordcollection.BuildConfig {
    public static <fields>;
}

// For open source: Use backend proxy for secrets
class SecureSpotifyApi(
    private val backendUrl: String,
    private val client: HttpClient
) {
    suspend fun exchangeCodeForToken(code: String): Result<AccessToken> {
        // Backend handles client secret
        return client.post("$backendUrl/auth/spotify/token") {
            setBody(mapOf("code" to code))
        }
    }
}
```

#### 14.3 Data Validation
```kotlin
// Input validation
class AlbumValidator {
    fun validate(album: Album): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        if (album.name.isBlank()) {
            errors.add(ValidationError.EmptyName)
        }
        if (album.name.length > 500) {
            errors.add(ValidationError.NameTooLong)
        }
        if (album.artists.isEmpty()) {
            errors.add(ValidationError.NoArtists)
        }
        if (album.releaseDate != null && 
            !album.releaseDate.matches(DATE_REGEX)) {
            errors.add(ValidationError.InvalidDate)
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}

sealed interface ValidationResult {
    object Valid : ValidationResult
    data class Invalid(val errors: List<ValidationError>) : ValidationResult
}
```

### Week 15: User Experience Polish

#### 15.1 Loading States
```kotlin
// Skeleton screens
@Composable
fun AlbumGridSkeleton() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp)
    ) {
        items(12) {
            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .shimmer()
            ) {
                // Placeholder content
            }
        }
    }
}

// Shimmer effect
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.LightGray.copy(alpha = 0.3f),
                Color.LightGray.copy(alpha = 0.5f),
                Color.LightGray.copy(alpha = 0.3f)
            ),
            start = Offset(translateAnim - 1000f, translateAnim - 1000f),
            end = Offset(translateAnim, translateAnim)
        )
    )
}
```

#### 15.2 Empty States
```kotlin
@Composable
fun EmptyLibraryState(
    onImport: () -> Unit,
    onSync: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.LibraryMusic,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            "Your library is empty",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            "Start by importing albums from Spotify or adding them manually",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
        )
        
        Spacer(Modifier.height(32.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onSync) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sync with Spotify")
            }
            
            OutlinedButton(onClick = onImport) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Albums")
            }
        }
    }
}
```

#### 15.3 Animations & Transitions
```kotlin
// Shared element transitions
@Composable
fun AlbumGrid(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit
) {
    LazyVerticalGrid(columns = GridCells.Adaptive(150.dp)) {
        items(albums, key = { it.id }) { album ->
            AlbumTile(
                album = album,
                onClick = { onAlbumClick(album) },
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}

// Page transitions
NavHost(
    startDestination = Screen.Library,
    enterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(300)
        )
    },
    exitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(300)
        )
    }
) {
    // Navigation graph
}
```

### Week 16: Deployment & Launch

#### 16.1 CI/CD Pipeline
```yaml
# .github/workflows/android.yml
name: Android Build

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Run tests
      run: ./gradlew testDebug
    
    - name: Build APK
      run: ./gradlew assembleRelease
      env:
        SPOTIFY_CLIENT_ID: ${{ secrets.SPOTIFY_CLIENT_ID }}
        SPOTIFY_CLIENT_SECRET: ${{ secrets.SPOTIFY_CLIENT_SECRET }}
    
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-release
        path: composeApp/build/outputs/apk/release/*.apk
    
    - name: Run instrumented tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 29
        script: ./gradlew connectedCheck

# .github/workflows/desktop.yml
name: Desktop Build

on:
  push:
    branches: [ main ]

jobs:
  build-macos:
    runs-on: macos-latest
    steps:
    - uses: actions/checkout@v3
    - name: Build macOS app
      run: ./gradlew packageDmg
    - name: Upload DMG
      uses: actions/upload-artifact@v3
      with:
        name: macos-app
        path: build/compose/binaries/main/dmg/*.dmg
  
  build-windows:
    runs-on: windows-latest
    steps:
    - uses: actions/checkout@v3
    - name: Build Windows app
      run: ./gradlew packageMsi
    - name: Upload MSI
      uses: actions/upload-artifact@v3
      with:
        name: windows-app
        path: build/compose/binaries/main/msi/*.msi
```

#### 16.2 Release Process
```kotlin
// Version management
// build.gradle.kts
android {
    defaultConfig {
        versionCode = 1
        versionName = "1.0.0"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

// Automated release notes
task generateReleaseNotes {
    doLast {
        val lastTag = "git describe --tags --abbrev=0".execute()
        val commits = "git log $lastTag..HEAD --pretty=format:'- %s'".execute()
        
        File("release-notes.md").writeText("""
            # Release ${version}
            
            ## Changes
            $commits
        """.trimIndent())
    }
}
```

#### 16.3 App Store Submission
**Android (Play Store)**:
```
1. Create Play Console account
2. Prepare store listing:
   - App name: Record Collection
   - Short description: Album-focused music player for Spotify
   - Screenshots (phone, tablet, desktop)
   - Feature graphic
   - Privacy policy URL
   
3. Set up app signing
4. Configure release track (Internal → Beta → Production)
5. Submit for review
```

**Desktop**:
```
1. macOS:
   - Sign with Developer ID
   - Notarize with Apple
   - Distribute via DMG or Mac App Store
   
2. Windows:
   - Sign with EV Code Signing cert
   - Distribute via MSI installer or Microsoft Store
   
3. Linux:
   - Publish to Snap Store
   - Publish to Flathub
```

### Phase 5 Deliverables
- ✅ Comprehensive logging and monitoring
- ✅ Crash reporting configured
- ✅ Security hardened
- ✅ Polished UX
- ✅ CI/CD pipeline
- ✅ App deployed to stores

---

## Cloud Infrastructure Plan

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                      Client Apps                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Android    │  │   Desktop    │  │     Web      │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└────────────┬────────────────────────────────┬───────────┘
             │                                │
             └───────────────┬────────────────┘
                             ▼
            ┌────────────────────────────────┐
            │        Load Balancer           │
            │      (Firebase Hosting)        │
            └───────────────┬────────────────┘
                            │
              ┌─────────────┴─────────────┐
              │                           │
              ▼                           ▼
    ┌─────────────────┐         ┌─────────────────┐
    │    Firebase     │         │   Cloud Run     │
    │                 │         │   (Custom API)  │
    │ - Auth          │◄────────┤                 │
    │ - Firestore     │         │ - Token exchange│
    │ - Storage       │         │ - Batch ops     │
    │ - Analytics     │         │ - Webhooks      │
    │ - Crashlytics   │         └─────────────────┘
    │ - Performance   │                   │
    └─────────────────┘                   │
                                          ▼
                                ┌─────────────────┐
                                │   Cloud SQL     │
                                │   (PostgreSQL)  │
                                │                 │
                                │ - User data     │
                                │ - Analytics     │
                                └─────────────────┘
```

### Cost Estimation (Monthly)

#### Starter Tier (< 100 users)
- **Firebase**: $0-25/month (Free tier sufficient)
  - Firestore: 50K reads, 20K writes/day free
  - Storage: 5GB free
  - Auth: Unlimited free
  - Hosting: 10GB bandwidth free

- **Cloud Run**: $0-10/month
  - Pay per request
  - First 2 million requests free

- **Cloud SQL**: $0 (Not needed yet)

- **Total**: ~$0-35/month

#### Growth Tier (100-10K users)
- **Firebase**: $50-200/month
  - Firestore: ~$1 per 100K reads
  - Storage: ~$0.10 per GB beyond free tier
  - Hosting: ~$0.15 per GB bandwidth

- **Cloud Run**: $50-100/month
  - ~1M requests/day
  - 2GB memory per instance

- **Cloud SQL**: $100/month
  - db-f1-micro instance
  - 10GB storage

- **Total**: ~$200-400/month

#### Enterprise Tier (10K+ users)
- **Firebase**: $500-1000/month
- **Cloud Run**: $300-500/month
- **Cloud SQL**: $300-500/month (db-n1-standard-1)
- **CDN**: $100/month
- **Monitoring**: $50/month

- **Total**: ~$1250-2150/month

### Infrastructure as Code

```terraform
# terraform/main.tf
provider "google" {
  project = var.project_id
  region  = var.region
}

# Firebase project (manual setup required first)
resource "google_firebase_project" "record_collection" {
  provider = google-beta
  project  = var.project_id
}

# Cloud Run service
resource "google_cloud_run_service" "api" {
  name     = "record-collection-api"
  location = var.region

  template {
    spec {
      containers {
        image = "gcr.io/${var.project_id}/record-collection-api:latest"
        
        resources {
          limits = {
            memory = "2Gi"
            cpu    = "2"
          }
        }
        
        env {
          name  = "DATABASE_URL"
          value = google_sql_database_instance.main.connection_name
        }
      }
    }
  }

  traffic {
    percent         = 100
    latest_revision = true
  }
}

# Cloud SQL instance
resource "google_sql_database_instance" "main" {
  name             = "record-collection-db"
  database_version = "POSTGRES_15"
  region           = var.region

  settings {
    tier = "db-f1-micro"
    
    backup_configuration {
      enabled    = true
      start_time = "03:00"
    }
    
    ip_configuration {
      ipv4_enabled = true
      authorized_networks {
        name  = "cloud-run"
        value = "0.0.0.0/0"  # Better: Use Cloud SQL Proxy
      }
    }
  }
}

# Firestore (created via Firebase console)
# Security rules deployed via Firebase CLI

# Storage bucket
resource "google_storage_bucket" "backups" {
  name          = "${var.project_id}-backups"
  location      = var.region
  storage_class = "STANDARD"
  
  lifecycle_rule {
    action {
      type = "Delete"
    }
    condition {
      age = 90  # Keep backups for 90 days
    }
  }
  
  versioning {
    enabled = true
  }
}
```

---

## Architecture Recommendations

### 1. Keep Current Architecture ✅

**Recommendation**: The current clean architecture is solid. Don't refactor for the sake of refactoring.

**What to Keep**:
- ✅ Repository pattern
- ✅ ViewModel layer
- ✅ Service layer for complex operations
- ✅ Event-driven architecture for side effects
- ✅ SQLDelight for local database

**Minor Refinements**:
- Split large service classes
- Add domain layer with use cases
- Standardize error handling
- Improve dependency injection testability

### 2. Add Domain Layer

**Why**: Separates business logic from data access

**Structure**:
```
domain/
├── model/          # Pure domain models
├── repository/     # Repository interfaces
└── usecase/        # Business operations
    ├── album/
    ├── collection/
    └── sync/
```

### 3. Adopt Offline-First

**Why**: Better UX, resilient to network issues

**Pattern**:
1. Write to local database immediately
2. Return success to UI
3. Queue for cloud sync
4. Sync in background
5. Handle conflicts on next sync

### 4. Add State Machine for Complex Flows

**Example**: Sync process
```kotlin
sealed class SyncState {
    object Idle : SyncState()
    object FetchingRemote : SyncState()
    data class ResolvingConflicts(val conflicts: List<Conflict>) : SyncState()
    object ApplyingChanges : SyncState()
    object Complete : SyncState()
    data class Error(val error: Throwable) : SyncState()
}

class SyncStateMachine {
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    
    suspend fun start() {
        _state.value = SyncState.FetchingRemote
        // ... transition through states
    }
}
```

---

## Success Metrics

### Technical Metrics
- ✅ **Crash-free rate**: > 99.5%
- ✅ **App start time**: < 2 seconds (cold start)
- ✅ **Time to interactive**: < 1 second
- ✅ **API success rate**: > 99%
- ✅ **Sync success rate**: > 95%
- ✅ **Test coverage**: > 80%
- ✅ **Build time**: < 5 minutes

### User Experience Metrics
- ✅ **First-time user success**: > 90% complete onboarding
- ✅ **Active users**: Track DAU/MAU ratio
- ✅ **Retention**: Day 1, Day 7, Day 30
- ✅ **Session length**: Average time in app
- ✅ **Feature adoption**: % users using collections, ratings, etc.

### Business Metrics (if applicable)
- ✅ **User growth**: Month-over-month
- ✅ **Cloud costs**: Per active user
- ✅ **Support tickets**: Track volume and resolution time

---

## Timeline Summary

| Phase | Duration | Key Deliverables |
|-------|----------|------------------|
| 1. Critical Bugs | 3 weeks | Bug fixes, error handling, Android auth |
| 2. Architecture | 3 weeks | Domain layer, testing, refactoring |
| 3. Platforms | 3 weeks | Platform features, optimization |
| 4. Cloud | 3 weeks | Firebase, sync, backup |
| 5. Production | 4 weeks | Monitoring, security, deployment |
| **Total** | **16 weeks** | **Production-ready app** |

---

## Next Steps

### Immediate Actions (This Week)
1. ✅ Fix critical import error in LibraryViewModel
2. ✅ Set up Firebase project
3. ✅ Create GitHub repository (if not already)
4. ✅ Set up CI/CD pipeline
5. ✅ Implement Android authentication

### Short-term (Next 2 Weeks)
1. Standardize error handling across repositories
2. Add comprehensive unit tests
3. Implement domain layer with use cases
4. Set up crash reporting

### Medium-term (Next Month)
1. Complete platform-specific features
2. Implement cloud sync
3. Add performance monitoring
4. Conduct security audit

### Long-term (Next 3 Months)
1. Beta testing program
2. App store submission
3. Marketing and launch
4. User feedback loop

---

**Document Owner**: Architecture & Product Team  
**Review Cycle**: Bi-weekly  
**Last Updated**: 2026-01-27
