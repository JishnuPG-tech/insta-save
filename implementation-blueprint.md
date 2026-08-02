# Implementation Blueprint — Insta-Save

File-by-file plan for `com.instasave.app`. Every class listed with its responsibility and key signatures. Build in the order of §8.

---

## 1. Package Tree

```text
com.instasave.app
├── InstaSaveApplication.kt                 @HiltAndroidApp, Configuration.Provider
│
├── domain/
│   ├── model/
│   │   ├── MediaModels.kt                  MediaInfo, MediaItem, FormatOption, Author, PostKind
│   │   ├── DownloadModels.kt               DownloadTask, DownloadStatus, Destination
│   │   ├── InstagramUrl.kt                 parsed + normalised URL value type
│   │   ├── SessionState.kt                 SignedOut | SignedIn(handle)
│   │   └── ExtractionError.kt              sealed error taxonomy
│   ├── repository/
│   │   ├── MediaRepository.kt
│   │   ├── DownloadRepository.kt
│   │   ├── HistoryRepository.kt
│   │   ├── CookieRepository.kt
│   │   └── SettingsRepository.kt
│   └── usecase/
│       ├── ParseInstagramUrlUseCase.kt
│       ├── ExtractMediaUseCase.kt
│       ├── EnqueueDownloadUseCase.kt
│       ├── PauseResumeDownloadUseCase.kt
│       ├── CancelDownloadUseCase.kt
│       ├── RetryDownloadUseCase.kt
│       ├── ObserveDownloadQueueUseCase.kt
│       ├── ObserveHistoryUseCase.kt
│       ├── DeleteHistoryRecordUseCase.kt
│       ├── SaveCookieSessionUseCase.kt
│       ├── ClearCookieSessionUseCase.kt
│       └── ObserveSessionUseCase.kt
│
├── data/
│   ├── extractor/
│   │   ├── InstagramExtractorManager.kt
│   │   ├── InstaNativeParser.kt
│   │   ├── YtDlpAndroidEngine.kt
│   │   ├── dto/  (IgMediaDto, IgCandidateDto, IgUserDto, JsonLdDto)
│   │   └── mapper/ (IgDtoMapper.kt, YtDlpJsonMapper.kt)
│   ├── network/
│   │   ├── OkHttpProvider.kt
│   │   ├── NetworkAllowlistInterceptor.kt
│   │   ├── UserAgentRotator.kt
│   │   ├── CookieInterceptor.kt
│   │   └── RateLimitBackoffInterceptor.kt
│   ├── download/
│   │   ├── DownloadForegroundService.kt
│   │   ├── ChunkedDownloader.kt
│   │   ├── DownloadWorker.kt
│   │   ├── DownloadQueueManager.kt
│   │   └── NotificationController.kt
│   ├── storage/
│   │   ├── MediaStoreWriter.kt
│   │   ├── SafDirectoryWriter.kt
│   │   ├── SidecarWriter.kt
│   │   └── FilenameTemplater.kt
│   ├── security/
│   │   ├── EncryptedCookieStore.kt
│   │   └── YtDlpCookieExporter.kt
│   ├── local/
│   │   ├── InstaSaveDatabase.kt
│   │   ├── dao/ (DownloadDao, HistoryDao, MediaCacheDao)
│   │   ├── entity/ (DownloadEntity, HistoryEntity, MediaCacheEntity)
│   │   └── SettingsDataStore.kt
│   └── repository/  (five *RepositoryImpl.kt)
│
├── di/
│   ├── NetworkModule.kt · DatabaseModule.kt · RepositoryModule.kt
│   ├── ExtractorModule.kt · DispatcherModule.kt
│
└── presentation/
    ├── MainActivity.kt
    ├── navigation/ (InstaSaveNavHost.kt, Routes.kt, BottomBar.kt)
    ├── theme/ (Color.kt, Type.kt, Shape.kt, Spacing.kt, Theme.kt)
    ├── components/ (MediaPreviewCard, ClipboardChip, ErrorCard, EmptyState,
    │                SkeletonBox, DownloadRow, SectionHeader, FormatRadioRow,
    │                SelectableThumb, ProgressBarTnum)
    ├── home/ (HomeScreen, HomeViewModel, HomeUiState, HomeEvent, FormatBottomSheet)
    ├── downloads/ (DownloadsScreen, DownloadsViewModel, DownloadsUiState, DownloadsEvent)
    ├── preview/ (PreviewScreen, PreviewViewModel, VideoPlayer, ZoomableImagePager)
    ├── login/ (LoginCookieScreen, LoginViewModel, ConsentPanel, SandboxedWebView)
    └── settings/ (SettingsScreen, SettingsViewModel, AboutScreen, LegalNoticeScreen)
```

---

## 2. Domain

### `MediaModels.kt`

```kotlin
enum class PostKind { REEL, POST, CAROUSEL, STORY, HIGHLIGHT, IGTV, PROFILE_PICTURE }
enum class MediaType { IMAGE, VIDEO, AUDIO }
enum class EngineTag { NATIVE, YTDLP }

data class Author(val username: String, val displayName: String?, val avatarUrl: String?)

data class FormatOption(
    val id: String,
    val container: String,          // mp4 | m4a | mp3 | flac | jpg | webp
    val codec: String?,
    val width: Int?, val height: Int?,
    val bitrateKbps: Int?,
    val approxSizeBytes: Long?,     // estimate; always shown with "~"
    val url: String,
    val requiresMux: Boolean = false,
    val requiresTranscode: Boolean = false,
)

data class MediaItem(
    val id: String, val index: Int, val type: MediaType,
    val thumbnailUrl: String?, val durationMs: Long?,
    val width: Int, val height: Int,
    val formats: List<FormatOption>,
) { val bestFormat: FormatOption? get() = formats.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) } }

data class MediaInfo(
    val postId: String, val shortcode: String, val sourceUrl: String,
    val kind: PostKind, val author: Author,
    val caption: String?, val hashtags: List<String>,
    val takenAtEpochSec: Long?, val isPrivate: Boolean,
    val items: List<MediaItem>, val extractedBy: EngineTag,
)
```

### `ExtractionError.kt`

```kotlin
sealed interface ExtractionError {
    data object LoginRequired : ExtractionError
    data class RateLimited(val retryAfterSec: Long?) : ExtractionError
    data object NotFound : ExtractionError
    data object PrivateAccount : ExtractionError
    data class ParserOutdated(val engine: EngineTag, val detail: String) : ExtractionError
    data object Network : ExtractionError
    data class Unknown(val detail: String) : ExtractionError
}
```

Maps 1:1 onto the error-copy table in `docs/ui-ux-spec.md` §7.

### `ParseInstagramUrlUseCase.kt`

```kotlin
class ParseInstagramUrlUseCase @Inject constructor() {
    operator fun invoke(raw: String): Result<InstagramUrl>
}
```

Responsibilities: trim, add `https://` if scheme-less, reject non-Instagram hosts, strip tracking params (`igsh`, `igshid`, `utm_*`, `hl`), resolve `share/` redirect URLs by flagging `needsRedirectResolution = true`, classify `PostKind`, extract the shortcode or username. Pure — no network. Regex table in §7.

### Repository ports

```kotlin
interface MediaRepository {
    suspend fun extract(url: InstagramUrl): Result<MediaInfo>
    suspend fun invalidate(shortcode: String)
}

interface DownloadRepository {
    suspend fun enqueue(tasks: List<DownloadTask>)
    fun observeQueue(): Flow<List<DownloadTask>>
    suspend fun pause(id: String); suspend fun resume(id: String)
    suspend fun cancel(id: String); suspend fun retry(id: String)
}

interface CookieRepository {
    fun observeSession(): Flow<SessionState>
    suspend fun save(cookies: Map<String, String>, handle: String?)
    suspend fun cookieHeader(): String?      // null when signed out
    suspend fun clear()
}
```

Note `cookieHeader()` returns the assembled header, so no caller ever handles an individual cookie value — that is what keeps Rule 4 enforceable.

---

## 3. Data — Extraction

### `InstaNativeParser.kt`

```kotlin
class InstaNativeParser @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val cookies: CookieRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun parse(url: InstagramUrl): Result<MediaInfo>
}
```

Endpoint ladder (stop at first success; short-circuit on a login wall):

| # | Endpoint | Notes |
|---|---|---|
| 1 | `GET /p/{shortcode}/?__a=1&__d=dis` | Fastest when it works; frequently gated |
| 2 | `GET /p/{shortcode}/embed/captioned/` | Jsoup out the inlined JSON blob |
| 3 | `GET /p/{shortcode}/` | `application/ld+json` from `<head>` |
| 4 | `GET /api/v1/media/{mediaId}/info/` + `X-IG-App-ID` | Session required; best fidelity |
| 5 | `graphql/query/` with `doc_id` | Most brittle; last resort |

Stories/Highlights skip the ladder: `GET /api/v1/feed/reels_media/?reel_ids={userId}` with a session. Profile pictures: `GET /api/v1/users/web_profile_info/?username={handle}` → `profile_pic_url_hd`.

Detection helpers: a response containing `"login_required"`, a redirect to `/accounts/login/`, or HTML with a `loginForm` node all map to `ExtractionError.LoginRequired` — do **not** let them fall through to the next rung, it just burns rate limit.

### `YtDlpAndroidEngine.kt`

```kotlin
class YtDlpAndroidEngine @Inject constructor(
    private val context: Application,
    private val cookieExporter: YtDlpCookieExporter,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun ensureInitialised()                     // YoutubeDL.init(), once
    suspend fun probe(url: String): Result<MediaInfo>   // --dump-single-json
    suspend fun download(
        url: String, format: String, destDir: File,
        onProgress: (Float, Long, String) -> Unit,
    ): Result<File>
}
```

Cookie file lifecycle: write to `cacheDir`, pass `--cookies`, **delete in `finally`** — no exceptions.

### `InstagramExtractorManager.kt`

```kotlin
class InstagramExtractorManager @Inject constructor(
    private val native: InstaNativeParser,
    private val fallback: YtDlpAndroidEngine,
    private val cache: MediaCacheDao,
    private val settings: SettingsRepository,
) {
    suspend fun extract(url: InstagramUrl): Result<MediaInfo>
}
```

Order: cache (TTL 6h) → in-flight `Deferred` dedupe under a `Mutex` → native → on non-`LoginRequired` failure, fallback → write cache. Honours the Settings "Extraction engine" override (Auto / Native only / Backup only). Emits which engine produced the result so the UI can label it.

---

## 4. Data — Download

### `ChunkedDownloader.kt`

```kotlin
class ChunkedDownloader @Inject constructor(private val client: OkHttpClient) {
    fun download(
        url: String, sink: OutputStream, startByte: Long = 0,
    ): Flow<DownloadProgress>
}
data class DownloadProgress(val bytes: Long, val total: Long, val bytesPerSec: Long)
```

`Range: bytes=<start>-` for resume. Emits at ≤4 Hz via `sample(250.ms)` — emitting per-buffer will melt recomposition. Speed is an EWMA over a 3s window, not an instantaneous delta, or the readout flickers unusably.

### `DownloadForegroundService.kt`

`startForeground()` **within 5 seconds** of start or API 31+ throws. `foregroundServiceType="dataSync"`. Bounded concurrency via `Semaphore(maxConcurrent)`. Stops itself when the queue drains. Actions: pause / resume / cancel / cancel-all via `PendingIntent`.

### `DownloadWorker.kt`

The durability backstop. `Constraints(NetworkType.CONNECTED or UNMETERED)` per the Wi-Fi-only setting, exponential backoff, unique work per task ID, resumes from the persisted byte offset after process death.

### `MediaStoreWriter.kt`

```kotlin
suspend fun createPendingUri(displayName: String, mime: String, kind: MediaType): Uri
suspend fun openOutput(uri: Uri): OutputStream
suspend fun finalise(uri: Uri)     // IS_PENDING = 0
suspend fun abandon(uri: Uri)      // delete partial
```

API 26–28 branch: direct `File` write + `MediaScannerConnection.scanFile`.

---

## 5. Data — Security

### `EncryptedCookieStore.kt`

```kotlin
class EncryptedCookieStore @Inject constructor(context: Application) {
    private val prefs = EncryptedSharedPreferences.create(
        context, "instasave_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        PrefKeyEncryptionScheme.AES256_SIV, PrefValueEncryptionScheme.AES256_GCM,
    )
    fun put(cookies: Map<String, String>, handle: String?)
    fun cookieHeader(): String?
    fun handle(): String?
    fun clear()
    override fun toString() = "EncryptedCookieStore(present=${cookieHeader() != null})"
}
```

That `toString()` override is deliberate: it makes an accidental `Log.d("...", store)` harmless.

---

## 6. Presentation

### `MainActivity.kt`

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { InstaSaveTheme { InstaSaveNavHost(sharedUrl = intent.extractSharedUrl()) } }
    }
    override fun onNewIntent(intent: Intent) { /* singleTask: re-dispatch shared URL */ }
}

fun Intent.extractSharedUrl(): String? = when (action) {
    Intent.ACTION_SEND -> getStringExtra(Intent.EXTRA_TEXT)
    Intent.ACTION_VIEW -> dataString
    else -> null
}
```

### ViewModel contract (applies to all)

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(...) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: HomeEvent) { /* when(event) ... */ }
}
```

State is a single immutable data class. Effects (navigate, snackbar, open file) go through the channel so they don't replay on rotation. Screens are stateless `(state, onEvent)` functions; the stateful wrapper collects with `collectAsStateWithLifecycle()`.

### `HomeUiState`

```kotlin
data class HomeUiState(
    val urlInput: String = "",
    val urlValid: Boolean = false,
    val clipboardSuggestion: String? = null,
    val phase: Phase = Phase.Idle,
    val mediaInfo: MediaInfo? = null,
    val error: ExtractionError? = null,
    val recent: List<HistoryRecord> = emptyList(),
    val formatSheet: FormatSheetState? = null,
) {
    enum class Phase { Idle, ExtractingNative, ExtractingFallback, Ready }
}
```

`ExtractingNative` and `ExtractingFallback` are separate on purpose — the UI must tell the user when it has switched engines, per `docs/ui-ux-spec.md` §1.3.

### `PreviewScreen` leak discipline

```kotlin
val player = remember { ExoPlayer.Builder(context).build() }
DisposableEffect(Unit) { onDispose { player.release() } }
```

Verified by the LeakCanary gate in `agent.md` §5.

---

## 7. URL Regex Suite

Canonical patterns. Every one of these is a required unit-test case.

```kotlin
object IgPatterns {
    val POST      = Regex("""instagram\.com/(?:[\w.]+/)?p/([A-Za-z0-9_-]{5,20})""")
    val REEL      = Regex("""instagram\.com/(?:[\w.]+/)?reels?/([A-Za-z0-9_-]{5,20})""")
    val IGTV      = Regex("""instagram\.com/(?:[\w.]+/)?tv/([A-Za-z0-9_-]{5,20})""")
    val STORY     = Regex("""instagram\.com/stories/([\w.]+)/(\d+)?""")
    val HIGHLIGHT = Regex("""instagram\.com/stories/highlights/(\d+)""")
    val PROFILE   = Regex("""instagram\.com/([\w.]{1,30})/?$""")
    val SHARE     = Regex("""instagram\.com/share/([A-Za-z0-9_-]+)""")  // needs redirect resolve
}
```

Must-pass positives:

```
https://www.instagram.com/p/Cx4f2AbCdEf/
https://instagram.com/p/Cx4f2AbCdEf/?igsh=MXY=          → params stripped
https://www.instagram.com/reel/Cx4f2AbCdEf/
https://www.instagram.com/reels/Cx4f2AbCdEf/
https://www.instagram.com/username/p/Cx4f2AbCdEf/
https://www.instagram.com/tv/Cx4f2AbCdEf/
https://www.instagram.com/stories/username/3212345678901234567/
https://www.instagram.com/stories/highlights/17900000000000000/
https://www.instagram.com/username
instagram.com/p/Cx4f2AbCdEf                              → scheme added
https://www.instagram.com/share/BAbCdEfGh/               → flagged for resolution
```

Must-fail negatives (each one has bitten a naive regex):

```
https://instagram.com.evil.com/p/Cx4f2AbCdEf/     host suffix attack
https://notinstagram.com/p/Cx4f2AbCdEf/
https://www.instagram.com/accounts/login/         reserved path, not a profile
https://www.instagram.com/explore/tags/cats/      reserved path
https://www.instagram.com/direct/inbox/           reserved path
https://youtube.com/watch?v=abc
just some text
""  (empty)
```

Reserved-word list that `PROFILE` must exclude: `p`, `reel`, `reels`, `tv`, `stories`, `explore`, `accounts`, `direct`, `about`, `developer`, `legal`, `api`, `share`.

---

## 8. Build Order

| Phase | Deliverable | Verify |
|---|---|---|
| 1 | Gradle setup, Hilt app class, theme, nav skeleton | App launches to a black Home stub |
| 2 | `domain/` models + `ParseInstagramUrlUseCase` | Full regex suite green |
| 3 | OkHttp stack + allowlist interceptor | Interceptor rejects `example.com` (unit test) |
| 4 | `InstaNativeParser` + DTO mappers | MockWebServer fixtures for each ladder rung |
| 5 | HomeScreen + FormatBottomSheet with fake repo | All states render, screenshot-checked |
| 6 | Room, history, downloads repo | Migration test |
| 7 | ChunkedDownloader + MediaStoreWriter | Real file lands in Movies/InstaSave |
| 8 | Foreground service + notifications + WorkManager | Kill process mid-download → resumes |
| 9 | DownloadsScreen wired to live queue | Progress ≤4 Hz, no dropped frames |
| 10 | Cookie store + LoginCookieScreen | `adb logcat` clean of session value |
| 11 | `YtDlpAndroidEngine` + manager fallback | Force native failure → fallback succeeds |
| 12 | PreviewScreen (ExoPlayer + zoom pager) | LeakCanary clean over 10 nav cycles |
| 13 | SettingsScreen, SAF picker, sidecars | Custom dir + caption `.txt` written |
| 14 | R8 release, split ABI, legal notice | `assembleRelease` clean, log-strip verified |

---

## 9. Test Targets

**Unit**
- `ParseInstagramUrlUseCaseTest` — the full §7 table as a `@ParameterizedTest`.
- `InstaNativeParserTest` — MockWebServer, one fixture per rung, plus a login-wall fixture asserting `LoginRequired` and asserting the parser **stopped** rather than trying rung 2.
- `IgDtoMapperTest` — carousel with mixed image/video, video with separate audio track, missing-caption case.
- `FilenameTemplaterTest` — token substitution, illegal-character sanitising, collision suffixing.
- `ChunkedDownloaderTest` — resume from offset, speed EWMA, cancellation.
- `NetworkAllowlistInterceptorTest` — allows `instagram.com` and `*.cdninstagram.com`, throws on anything else.
- `ArchitectureTest` — no `android.*` import under `domain/`.

**Instrumented**
- Room migration tests for every schema bump.
- `MediaStoreWriter` write/finalise/abandon against a real provider.
- Compose UI tests: Home renders all 10 states from §1.3 of the UI spec; format sheet CTA disabled with zero selection.

**Fixtures.** Keep sanitised JSON/HTML captures in `src/test/resources/fixtures/`, dated. They rot fast — re-capture whenever the parser is touched, and note the capture date in the filename.
