# System Architecture — Insta-Save Android

---

## 1. Philosophy

Clean Architecture + MVVM + Unidirectional Data Flow. Three strictly decoupled layers; dependencies point inward only.

```text
                  ┌─────────────────────────────────────┐
                  │          Presentation Layer         │
                  │   Compose UI · ViewModels · Nav     │
                  │   StateFlow<UiState> ▼   UiEvent ▲  │
                  └──────────────────┬──────────────────┘
                                     │ depends on
                                     ▼
                  ┌─────────────────────────────────────┐
                  │             Domain Layer            │
                  │   Entities · UseCases · Repo ifaces │
                  │       PURE KOTLIN — no android.*    │
                  └──────────────────▲──────────────────┘
                                     │ implements
                                     │
                  ┌──────────────────┴──────────────────┐
                  │              Data Layer             │
                  │  RepositoryImpls · Extractor        │
                  │  Pipeline · Room · OkHttp · Store   │
                  │  WorkManager · EncryptedPrefs       │
                  └─────────────────────────────────────┘
```

The domain layer knows nothing about Android, Instagram HTML, Room, or Compose. It declares `interface MediaRepository`; the data layer supplies it; Hilt binds them.

---

## 2. Component Map

```text
┌───────────────────────────── PRESENTATION ─────────────────────────────┐
│ MainActivity (single Activity, ACTION_SEND / ACTION_VIEW entry)        │
│   └ NavHost                                                            │
│      ├ HomeScreen ────────── HomeViewModel                             │
│      ├ FormatBottomSheet ─── (hosted by HomeViewModel state)           │
│      ├ DownloadsScreen ───── DownloadsViewModel                        │
│      ├ PreviewScreen ─────── PreviewViewModel                          │
│      ├ LoginCookieScreen ─── LoginViewModel                            │
│      └ SettingsScreen ────── SettingsViewModel                         │
└───────────────────────────────┬────────────────────────────────────────┘
                                │
┌───────────────────────────── DOMAIN ───────────────────────────────────┐
│ Entities:  MediaInfo · MediaItem · DownloadTask · FormatOption          │
│            InstagramUrl · SessionState · ExtractionError                │
│ UseCases:  ParseInstagramUrl · ExtractMedia · EnqueueDownload           │
│            PauseResumeDownload · SaveCookieSession · ClearSession       │
│            ObserveDownloadQueue · ObserveDownloadHistory · DeleteRecord │
│ Ports:     MediaRepository · DownloadRepository · CookieRepository      │
│            SettingsRepository · HistoryRepository                       │
└───────────────────────────────┬────────────────────────────────────────┘
                                │
┌───────────────────────────── DATA ─────────────────────────────────────┐
│ extractor/   InstagramExtractorManager                                 │
│                ├ InstaNativeParser   (OkHttp · Jsoup · kotlinx.serial) │
│                └ YtDlpAndroidEngine  (youtubedl-android · FFmpeg)      │
│ network/     OkHttpProvider · UserAgentRotator · CookieInterceptor      │
│              NetworkAllowlistInterceptor · RateLimitBackoff             │
│ download/    DownloadForegroundService · ChunkedDownloader              │
│              DownloadWorker (WorkManager) · NotificationController      │
│ storage/     MediaStoreWriter · SafDirectoryWriter · SidecarWriter      │
│ security/    EncryptedCookieStore (Keystore + EncryptedSharedPrefs)     │
│ local/       InstaSaveDatabase (Room) · DownloadDao · HistoryDao        │
│              SettingsDataStore                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Layer Detail

### 3.1 Presentation

Single `MainActivity` hosting a Compose `NavHost` with type-safe `@Serializable` routes. Screens are stateless functions taking `(state, onEvent)`; ViewModels own `MutableStateFlow` and expose `asStateFlow()`, collected with `collectAsStateWithLifecycle()`.

One-shot effects (navigation, snackbars, share intents) go through a `Channel<UiEffect>` exposed as `receiveAsFlow()` — not through state, so they don't replay on configuration change.

Screen inventory and every state variant: see `docs/ui-ux-spec.md`.

### 3.2 Domain

Entities are immutable `data class`es with no framework annotations beyond `@Serializable` where they cross a process boundary.

```
MediaInfo   { postId, shortcode, url, caption, author: Author,
              takenAt: Instant, kind: PostKind, items: List<MediaItem>,
              isPrivate: Boolean, extractedBy: EngineTag }

MediaItem   { id, index, type: IMAGE|VIDEO|AUDIO, formats: List<FormatOption>,
              thumbnailUrl, durationMs?, width, height }

FormatOption{ id, container, codec, width?, height?, bitrateKbps?,
              approxSizeBytes?, url, requiresMux: Boolean }

DownloadTask{ id, mediaInfo, item, format, destination,
              progress: Float, downloadedBytes, totalBytes,
              speedBytesPerSec, etaSeconds,
              status: QUEUED|EXTRACTING|DOWNLOADING|MUXING|PAUSED|
                      COMPLETED|FAILED(reason)|CANCELLED }
```

Use cases are single-method (`operator fun invoke`), return `Result<T>` or a sealed outcome, and never throw across the boundary.

### 3.3 Data

#### A. Hybrid extraction engine

**Primary — `InstaNativeParser`.** Direct OkHttp requests, session cookies attached when present.

Resolution order for a given shortcode:
1. `GET /p/<shortcode>/?__a=1&__d=dis` — JSON if it works, login wall if not.
2. `GET /p/<shortcode>/embed/captioned/` — parse the inlined `window.__additionalDataLoaded` / `contextJSON` blob out of the HTML with Jsoup.
3. `GET /p/<shortcode>/` — extract `application/ld+json` JSON-LD from `<head>`.
4. `GET /api/v1/media/<mediaId>/info/` with `X-IG-App-ID` — requires a session; highest fidelity when available.
5. GraphQL `graphql/query/` with a `query_hash`/`doc_id` — kept behind a remotely-unchanging local constant table; treat as most brittle, try last.

Stories/Highlights bypass the shortcode path entirely and use `/api/v1/feed/reels_media/` with a session.

Target < 500 ms on the happy path. Any step returning a login interstitial short-circuits to `ExtractionError.LoginRequired` rather than falling through all five.

**Fallback — `YtDlpAndroidEngine`.** `youtubedl-android` wrapping the yt-dlp Python payload plus FFmpeg native libs. Invoked when:
- the native parser exhausts its ladder, or
- the requested output needs transcoding (MP3/FLAC audio extraction), or
- the selected video/audio formats are separate DASH streams needing a mux.

Cookies are exported to a Netscape-format cookie file in `cacheDir`, passed via `--cookies`, and **deleted in a `finally` block**. Expect 3–8 s, so the UI must show it as a distinct "trying fallback engine" state rather than pretending the fast path is still running.

**`InstagramExtractorManager`** orchestrates: in-memory LRU + Room cache lookup → dedupe concurrent parses of the same shortcode via a `Mutex`-guarded map of in-flight `Deferred`s → native → fallback → map both engines' output onto the same `MediaInfo`.

#### B. Storage

Scoped storage via `MediaStore` on API 29+:

| Kind | Collection | Relative path |
|---|---|---|
| Image | `MediaStore.Images` | `Pictures/InstaSave` |
| Video | `MediaStore.Video` | `Movies/InstaSave` |
| Audio | `MediaStore.Audio` | `Music/InstaSave` |

Written with `IS_PENDING=1`, flipped to `0` on completion so a partial file never appears in the gallery. On API 26–28, `WRITE_EXTERNAL_STORAGE` + direct `File` writes with a `MediaScannerConnection` ping.

Custom directories via SAF `ACTION_OPEN_DOCUMENT_TREE` with `takePersistableUriPermission`, written through `DocumentFile`.

Caption/metadata sidecars (`.txt`, `.json`) are written alongside the media by `SidecarWriter`, using the same base filename.

#### C. Cookies & security

```text
WebView (instagram.com/accounts/login/)
   │  user authenticates directly with Meta; app injects nothing
   ▼
CookieManager.getCookie("https://www.instagram.com")
   │  extract sessionid · ds_user_id · csrftoken · mid · rur
   ▼
EncryptedCookieStore
   │  EncryptedSharedPreferences
   │  MasterKey(AES256_GCM, Keystore-backed, StrongBox when available)
   ▼
   ├─► CookieInterceptor  → attaches Cookie header to OkHttp requests
   └─► YtDlpCookieExporter → transient Netscape file, deleted after use
```

Sign-out clears the encrypted store, `CookieManager.removeAllCookies`, and `WebStorage.deleteAllData`.

#### D. Networking

OkHttp singleton with, in order: `NetworkAllowlistInterceptor` (Rule 1 enforcement) → `UserAgentRotator` → `CookieInterceptor` → `RateLimitBackoff` (respects `Retry-After`, exponential with jitter, hard ceiling, and a circuit-break after repeated 401/429 so we never hammer an account into a challenge lock) → debug-only redacted logging.

Header shaping mimics an Android Chrome session: current Chrome-on-Android UA strings, `X-IG-App-ID`, `X-ASBD-ID`, `Sec-Fetch-*`, `Accept-Language`.

**On TLS fingerprinting — scope honestly.** `ConnectionSpec` lets us restrict to a browser-like TLS 1.3 cipher list, which moves the JA3 hash closer to a real client. It does **not** produce a byte-identical Chrome Client Hello: extension ordering, GREASE, and ALPS are not controllable through OkHttp/Conscrypt. Byte-level parity needs a uTLS-style native layer and is out of scope. Implement the cipher-list tweak, document it as best-effort, and do not describe it as spoofing in user-facing text.

#### E. Persistence (Room)

```
download_tasks   (id PK, shortcode, itemIndex, formatId, destUri,
                  status, downloadedBytes, totalBytes, createdAt,
                  updatedAt, failureReason)
history_records  (id PK, shortcode, author, kind, filePath, mimeType,
                  sizeBytes, caption, metadataJson, completedAt)
media_cache      (shortcode PK, mediaInfoJson, cachedAt)   -- TTL 6h
```

Schemas exported to `app/schemas/`; every version bump ships a migration and a Room migration test.

---

## 4. Data Flows

### 4.1 Extraction

```text
User pastes / shares URL
   │
   ▼ ParseInstagramUrlUseCase  ── normalise, strip igsh/utm params,
   │                              classify kind, reject non-Instagram
   ▼ ExtractMediaUseCase
   ▼ InstagramExtractorManager
      ├─► 1. media_cache hit (< 6h)? ───────────────► MediaInfo
      ├─► 2. in-flight Deferred for shortcode? ─────► await it
      ├─► 3. InstaNativeParser (OkHttp + cookies)
      │        ├─ Success ─────────────────────────► MediaInfo
      │        ├─ LoginRequired ───────────────────► prompt sign-in (stop)
      │        └─ Failure ─┐
      │                    ▼
      ├─────────────► 4. YtDlpAndroidEngine
      │                    ├─ Success ─────────────► MediaInfo
      │                    └─ Failure ─────────────► ExtractionError
      └─► write media_cache, emit to UI
                                │
                                ▼ FormatBottomSheet
```

### 4.2 Download

```text
User confirms selection
   │
   ▼ EnqueueDownloadUseCase → persist DownloadTask rows (QUEUED)
   ▼ start DownloadForegroundService (startForeground within 5s)
   │
   ├─► Notification: ongoing, determinate progress, pause/cancel actions
   ├─► ChunkedDownloader: OkHttp streaming, Range-request resume,
   │     writes to MediaStore pending URI, emits progress at ≤4 Hz
   ├─► Room: task row updated (throttled, not per-chunk)
   ├─► If mux/transcode needed → FFmpeg step, status = MUXING
   ├─► SidecarWriter: caption .txt / metadata .json
   └─► Completion: IS_PENDING=0, history row, notification →
       "Download finished" with open/share actions
   │
   └─► Process death or network loss → WorkManager
       (NetworkType.CONNECTED, exponential backoff) re-enters the queue
       from the persisted byte offset
```

Concurrency is bounded by a `Semaphore` (default 3). The service stops itself when the queue drains.

---

## 5. Security Posture

- **Direct-to-Meta only**, enforced by interceptor and unit test (`agent.md` Rule 1).
- **No telemetry, no crash reporting, no ads.** Zero third-party SDKs with network access.
- **Cookies** encrypted at rest, never logged, cleared completely on sign-out.
- **Release hardening**: R8 full mode, log stripping, `allowBackup=false`, no cleartext traffic (`usesCleartextTraffic=false`), certificate transparency left to platform defaults (pinning is not viable against Meta's rotating CDN certs).
- **Rate-limit safety** is a *user-protection* feature, not a performance one: the circuit breaker exists so an over-eager retry loop can't get someone's account locked.

---

## 6. Known Architectural Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Instagram gates anonymous access behind a login wall | Public downloads fail without sign-in | Ladder of endpoints; clear `LoginRequired` state; never pretend it's a generic error |
| `query_hash` / `doc_id` rotation | GraphQL path dies | Kept last in the ladder; app remains functional without it |
| yt-dlp Instagram extractor breaks upstream | Fallback dies too | Dependency update path documented; UI names the failing engine |
| yt-dlp + FFmpeg payload size | 25–40 MB per ABI | Split ABI; consider a build flavour with the fallback engine excluded |
| Foreground-service restrictions tightening (API 34+) | Downloads killed | Declare `dataSync` FGS type, request `startForeground` immediately, WorkManager as the durable backstop |
