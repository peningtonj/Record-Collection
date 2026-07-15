# Record Collection

A Kotlin Multiplatform (Android + Desktop/JVM) app providing an album-centric alternative front-end to Spotify. Built with Compose Multiplatform and backed by Firebase Firestore.

![Record Collection App Screenshot](screenshots/screenshot.png)

## Features

- **Album-centric playback** — browse and play full albums, not just playlists
- **Library management** — sync with your Spotify saved albums or build your own collection
- **Collections** — curated playlists of albums, playable in order or shuffled, with optional sound effects between albums
- **Ratings** — rate albums on a 0–5 star scale
- **Filtering & sorting** — filter by artist, genre, release date, rating, or free-text search
- **Genre tagging** — automatic genre enrichment via Every Noise At Once data
- **Artist detail pages** — browse all albums by an artist
- **Release group management** — swap between standard releases and special/alternate editions (powered by MusicBrainz)
- **Album deduplication** — identify and merge duplicate album entries
- **Search** — search the Spotify catalogue directly from the app
- **AI-powered collection import** — paste an article URL and let GPT-4.1 extract an album list (requires an OpenAI API key)
- **Spotify playlist import** — import all albums referenced in any Spotify playlist as a collection
- **Profile view** — see your connected Spotify account details

## Platforms

| Platform | Entry point |
|---|---|
| Desktop (JVM) | `desktopMain/.../main.kt` |
| Android | `androidMain/.../MainActivity.kt` |

## Prerequisites

- A **Spotify Premium** account (required for playback control via the Spotify API)
- A registered **Spotify Developer application** (Client ID & Client Secret) from the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
- A **Firebase project** with Firestore enabled and `google-services.json` placed at `composeApp/google-services.json`
- *(Optional)* An **OpenAI API key** for AI-powered collection imports — configured in Settings inside the app

## Building & Running

```bash
# Run the desktop app
./gradlew :composeApp:run

# Build a distributable desktop package
./gradlew :composeApp:createDistributable

# Build an Android debug APK
./gradlew :composeApp:assembleDebug

# Run all shared tests (JVM target)
./gradlew :composeApp:desktopTest
```

## Importing Your Library

1. Log in with your Spotify account.
2. Use the **Sync with Spotify Saved Albums** button to import your saved albums.
3. The sync can be run repeatedly — it merges both libraries without duplicating albums.

## Collections

Collections are ordered lists of albums.

### Playing a Collection
Select a collection and choose **Play** (in order) or **Shuffle**. A short sound effect plays between albums by default — this can be disabled in **Settings**.

### Importing a Collection

| Source | How |
|---|---|
| Spotify playlist | Enter a playlist URL — every album with a track in the playlist is added |
| Web article | Enter an article URL — GPT-4.1 parses the page and extracts albums (requires OpenAI API key) |

## Architecture

Clean architecture with layers: **UI → ViewModel → Service → Repository → Network/Database**

- Shared code in `composeApp/src/commonMain/`
- Platform-specific code via `expect`/`actual` in `androidMain/` and `desktopMain/`
- Persistence via **Firebase Firestore** (`dev.gitlive:firebase-firestore`)
- HTTP via **Ktor** (OkHttp engine) with bearer-token auth, exponential-backoff retry, and rate-limit handling

See [ARCHITECTURE.md](ARCHITECTURE.md) for full details.

## Technology Stack

| Concern | Library |
|---|---|
| UI | Compose Multiplatform 1.8.1 |
| Language | Kotlin 2.1.21 |
| Async | Kotlin Coroutines 1.10.2 |
| Database | Firebase Firestore (`dev.gitlive`) |
| Networking | Ktor 3.0.3 + OkHttp |
| Serialization | Kotlinx Serialization |
| Logging | Napier 2.7.1 |
| Testing | MockK · Turbine · coroutines-test |

## External APIs

- **Spotify Web API** — library sync, search, playback control, OAuth 2.0 auth
- **MusicBrainz API** — release group metadata and alternate edition lookup
- **Every Noise At Once** — genre enrichment for artists
- **OpenAI API (GPT-4.1)** — article parsing for collection import

## Inspiration

Heavily inspired by the [kotify](https://github.com/dzirbel/kotify) project by Dominic Zirbel.
