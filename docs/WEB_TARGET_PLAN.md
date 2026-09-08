# Web Target Plan — Compose Multiplatform on Kotlin/JS

**Goal**: run the existing Compose UI as a browser app alongside desktop and Android,
with maximum code reuse.

**Approach**: add a **Kotlin/JS browser target** to `composeApp`. Compose Multiplatform
1.8.1 publishes `js` variants of `foundation` / `material3` / `ui`, so the existing
`@Composable` screens render to an HTML `<canvas>` via Skiko-compiled-to-JS. No new UI
code — `commonMain` compiles as-is once a handful of platform seams get a `jsMain`
`actual`.

This is deliberately a **bridge**: JetBrains is steering the canvas renderer toward
`wasmJs` (Beta in 1.8), and the `js` backend is slower and in maintenance mode. We pick
`js` now purely because of **GitLive Firebase**:

| Artifact | `js` (stable) | `wasmJs` |
|---|---|---|
| `firebase-firestore:2.3.0` | ✅ | ❌ (only in `3.0.0-alpha02`) |
| `firebase-auth:2.3.0` | ✅ | ❌ |

Every other dependency we use ships **both** `js` and `wasmJs` (verified against Maven
Central: Ktor, Coil3 `coil-compose` / `coil-network-ktor3`, `androidx.lifecycle`
viewmodel + viewmodel-compose, `multiplatform-settings`, napier, kotlinx-*). So the day
GitLive's wasmJs is stable, `js` → `wasmJs` is a target swap plus the Firebase `actual`
— nothing else.

> **Out of scope for now**: the OpenAI "import collection from an article" feature
> (`OpenAiApi` + `readability4j`) — no CORS + can't ship the key client-side + the lib is
> JVM-only. On web that feature is hidden; "import from a Spotify playlist" (pure Spotify
> API, CORS-OK) stays.

---

## Gradle / module changes

```kotlin
kotlin {
    androidTarget { … }
    jvm("desktop") { … }

    js(IR) {
        browser {
            commonWebpackConfig { outputFileName = "recordcollection.js" }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            // MOVE OUT of commonMain (into androidMain + desktopMain):
            //   - net.dankito.readability4j:readability4j   (JVM-only)
            //   - io.ktor:ktor-client-okhttp                (engine; see HTTP seam below)
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(libs.firebase.firestore)    // resolves the -js variant
                implementation(libs.firebase.auth)
                implementation(libs.coil.network.ktor3)     // replaces coil-network-okhttp on web
            }
        }
    }
}
```

New files:
- `jsMain/kotlin/main.kt` — `fun main() { … ComposeViewport(document.body!!) { App(deps, navigator) } }`
- `jsMain/resources/index.html` — canvas host + the Firebase JS config object
- the `actual`s listed below

---

## Platform seams

Ordered roughly by effort. "commonMain change" means the fix also improves android/desktop.

### 1. HTTP client engine  *(~1 d, also a commonMain cleanup)*

`ProductionNetworkModule` and the DI factories hardcode `HttpClient(OkHttp)` in
`commonMain` — it only compiles today because both current targets are JVM.

- Add `expect fun httpClientEngineFactory(): HttpClientEngineFactory<*>`
  → `OkHttp` in `androidMain`/`desktopMain`, `Js` in `jsMain`.
- `ProductionNetworkModule` takes the engine (or an engine factory) as a constructor arg
  instead of importing `OkHttp` directly.
- Also fixes the two `cause is java.io.IOException` checks in the retry predicate — use
  Ktor's multiplatform `io.ktor.utils.io.errors.IOException`, or match on
  `HttpRequestTimeoutException` / connection exceptions.

### 2. `FirebaseDriver` (`expect class`)  *(~0.5 d)*

- Desktop: `android.app.Application()` stub + `FirebasePlatform` shim.
- Web `actual`: `Firebase.initialize(options = FirebaseOptions(apiKey = …, projectId = …,
  applicationId = …, …))`, values read from a `window.firebaseConfig` object baked into
  `index.html`. Clean — real Firebase JS SDK, no shim, no `google-services.json`.

### 3. `sha256Hex` (`expect fun`)  *(~0.5 d)*

- JVM `actual` uses `java.security.MessageDigest` — **synchronous**. The browser has no
  synchronous SHA-256 (`crypto.subtle.digest` is async).
- Replace `util/Hashing.kt` for **all** targets with a pure-Kotlin MPP hash
  (`org.kotlincrypto.hash:sha2`). Keeps `generateAlbumId` synchronous everywhere. Only
  call site is `AlbumMapper.generateAlbumId`; `GenerateAlbumIdTest` locks the digest.

### 4. `secureRandomHex` (`expect fun`)  *(~0.25 d)*

- Web `actual`: `crypto.getRandomValues(Uint8Array(byteCount))` → hex. Synchronous, ~10
  lines.

### 5. OAuth handler  *(~1–1.5 d)*

- Current: `DesktopAuthHandler` (`ServerSocket(8888)` + `java.awt.Desktop.browse`),
  `AndroidAuthHandler` (Custom Tabs).
- Web `WebAuthHandler`: PKCE stays identical; `window.location.href = authorizeUrl`; a
  `callback.html` (or a `#callback` hash route) reads `?code=` / `state`, hands it back
  to the app (via `localStorage` event / `BroadcastChannel`, or just resume on the main
  page after redirect). Spotify's Authorization Code + PKCE flow is designed for browser
  SPAs — this is the simplest of the three handlers.
- Redirect URI: register `https://<host>/callback` (and `http://localhost:8080/callback`
  for dev) in the Spotify dashboard.

### 6. Coil network fetcher  *(~0.5 d)*

- Current `coil-network-okhttp` auto-registers on JVM.
- Web: add a `SingletonImageLoader.Factory` that installs `coil-network-ktor3` with the
  `jsMain` `HttpClient`. (Doing this via a factory is cleaner on all platforms than
  relying on auto-registration.)

### 7. `RecordCollectionApp` / `NavigationHost` (`expect`)  *(~0.5 d)*

- Web `actual` for `RecordCollectionApp` starts as a copy of the desktop side-nav layout.
- `NavigationHost` web `actual` is the same shape as the others (provide the per-screen
  `LocalViewModelStoreOwner`, render `content(screen)`).

### 8. URL routing  *(~1–2 d, strongly recommended)*

The custom `Navigator` has no URL binding, so a browser refresh drops the user back to
Login/Library and there are no deep links.

- Add `Screen.toRoute()` / `Screen.fromRoute()` (partly exists) and, in the web
  `Navigator` wiring, push/replace `window.history` on navigation + listen for
  `popstate`. `NavigationState.backStack` already models the stack.

### 9. Persistence (`multiplatform-settings`)  *(free)*

- Web `actual` container factory uses `StorageSettings()` → `localStorage`.
- **Security note**: Spotify tokens in `localStorage` are reachable by any XSS. This is
  strictly worse than desktop/Android and interacts with TECH_DEBT 2.5. See also the
  Firestore security note below.

### 10. Logging  *(free)*

- `FirebaseFileAntilog` / `SpotifyFileAntilog` are `desktopMain` file I/O. Web `main.kt`
  installs only a console antilog.

---

## Build & run

- `./gradlew :composeApp:jsBrowserDevelopmentRun` — webpack dev server.
- `./gradlew :composeApp:jsBrowserDistribution` — static bundle in
  `build/dist/js/productionExecutable/` (`.js` + Skiko `.wasm` blob + `index.html`).
- Deploy to any static host (Firebase Hosting fits — same project).

---

## What the running app is like

- **Bundle**: ~2–5 MB gzipped (Skiko-JS blob + app JS). Heavy first load, cached after.
- **Cold start**: slower than `wasmJs` (no wasm JIT for Skia) — ~1–2 s to first paint
  after download on a modern laptop.
- **Canvas caveats** (same for js and wasmJs): the whole UI is one `<canvas>`, so no
  native text selection/copy outside `TextField`s, limited screen-reader accessibility,
  no SEO, suppressed browser context menu (`onRightClick` still works), rough IME/CJK
  input, `Ctrl/Cmd+F` doesn't find text. Acceptable for an app; not for a marketing page.
- **Mobile web**: works, but needs the Android bottom-nav layout as the small-screen
  `RecordCollectionApp` `actual`.

---

## Phased plan

| Phase | Work | Rough size |
|---|---|---|
| **1. Spike** | Add the `js` target; stub every `actual` as `TODO()`; get `jsBrowserDevelopmentRun` to compile and blank-render. Surfaces any hidden `commonMain` JVM assumption fast. | 2–3 d |
| **2. Core online** | HTTP engine seam (1), `FirebaseDriver` js (2), hashing lib swap (3), `secureRandomHex` (4), Coil ktor fetcher (6), `settings` (9). Target: Library screen shows real Firestore + Spotify data. | ~1 wk |
| **3. Auth** | `WebAuthHandler` + PKCE redirect + `callback.html` (5). | 2–3 d |
| **4. Polish** | URL routing (8), responsive layout `actual` (7), hide the OpenAI feature on web, favicon / title / meta, canvas-init error boundary. | ~1 wk |

**≈ 3–4 weeks** to a usable web build — most of it in auth, URL routing, and shaking
out `commonMain` assumptions. The UI itself is free.

---

## Migration to `wasmJs` later

When GitLive Firebase ships stable `wasmJs` (currently `3.0.0-alpha02` only):

1. Change `js(IR) { browser() }` → `wasmJs { browser() }` (or run both).
2. `jsMain` → `wasmJsMain` (or a shared `webMain` if running both).
3. The Firebase `actual` may need small binding tweaks.
4. Everything else — Ktor `Js` engine, Coil, lifecycle, settings, the OAuth handler, URL
   routing, the hashing/random `actual`s — already supports `wasmJs` unchanged.

Keeping the seams above as `expect/actual` (rather than `#if`-style target checks) makes
this a mechanical move.

---

## Codebase-specific risks

- **`ProductionNetworkModule` in `commonMain`** imports the OkHttp engine directly — the
  single biggest coupling and a prerequisite. Untangling it (engine injection) is also
  good hygiene for android/desktop.
- **`FirebaseDriver` is an `expect class` with a constructor** — still compiler-Beta in
  this project; keep the third `actual` minimal.
- **Firestore security** — the web build ships the Firebase config publicly, so the
  current "any signed-in client can read/write any `users/{uid}/…` path" gap (anonymous
  auth, no rule enforcement of the path key — TECH_DEBT 2.1) goes from "anyone who has
  the app" to "anyone on the internet". **Real per-user auth (custom token) should land
  before or with the web release**, not after.
- **GitLive `js` is stable but lightly used** — most GitLive users are on
  jvm/android/ios. Expect the occasional missing binding; a thin `@JsModule` shim over
  the Firebase JS SDK is the fallback.
- **Playback** — the app controls playback on *external* Spotify devices via the Web API
  (CORS-OK), so no blocker. If the browser tab itself should become a playback device
  that's the Spotify **Web Playback SDK** (a separate web-only feature, not a port
  blocker).
