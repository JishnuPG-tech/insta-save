# Agent Rules & Technical Guidelines — Insta-Save

Binding development standards for any AI agent or engineer working in this repository. If a change conflicts with a rule here, the rule wins or the rule gets amended in the same PR — never silently violated.

---

## 1. Purpose

This document defines coding conventions, architectural boundaries, security requirements, and the pre-merge checklist. It is the operational companion to `PRD.md` (what to build) and `architecture.md` (how it fits together).

---

## 2. Core Rules

### Rule 1 — Zero third-party relay servers

All network I/O occurs **directly** between the user's device and Meta-owned hosts:

```
instagram.com  www.instagram.com  i.instagram.com
*.cdninstagram.com  *.fbcdn.net
```

Forbidden without an explicit, discussed exception:
- Proxy or CORS-bypass services
- Remote scraping/extraction backends
- Analytics, telemetry, crash reporting, A/B, or feature-flag SDKs
- Ad networks, attribution SDKs, push services

The only permitted non-Meta network call is an **opt-in, user-initiated** GitHub Releases check for app updates. It must be off by default.

**Enforcement.** A `NetworkAllowlistInterceptor` in the OkHttp chain throws on any host outside the allowlist. A unit test asserts the interceptor rejects `example.com`. Do not remove either.

### Rule 2 — Clean Architecture & UDF

**Domain layer** (`domain/`): pure Kotlin. Zero imports from `android.*`, `androidx.*`, Compose, Room, OkHttp, or Hilt-Android. Coroutines and `kotlinx.serialization` annotations are permitted. Verified by a Konsist/ArchUnit-style test — see §5.

**Data layer** (`data/`): implements `domain` repository interfaces. Never imports from `presentation/`.

**Presentation layer** (`presentation/`): 100% Jetpack Compose. No `R.layout.*`, no XML layout files, no `Fragment`. The single permitted legacy-View escape hatch is the authentication `WebView`, wrapped in `AndroidView` inside `LoginCookieScreen`, and it must stay confined to that file.

**Data flow.** State flows down as immutable `StateFlow<T>` exposed by ViewModels; events flow up through explicit `sealed interface` event types. Composables receive state + a single `onEvent: (UiEvent) -> Unit`. No `MutableState` hoisted across screen boundaries, no ViewModel instance passed into a child composable below the screen root.

**Threading.** All I/O on `Dispatchers.IO`. Never block the main thread. Dispatchers are injected (`@IoDispatcher`), never hardcoded in classes that need testing.

### Rule 3 — Mobile UX & touch standards

- Minimum touch target **48dp × 48dp**, always. Use `Modifier.minimumInteractiveComponentSize()` or explicit sizing on icon buttons.
- Every async operation renders all four states explicitly: **Loading** (skeleton or determinate progress — prefer skeletons over spinners for content), **Error** (inline message + retry affordance, never a bare toast), **Empty** (illustration + one-line explanation + primary action), **Success**.
- Edge-to-edge via `enableEdgeToEdge()`, with correct `WindowInsets` consumption. No content under the gesture bar or status bar unless intentionally scrolled behind it.
- Respect `Settings.Global.ANIMATOR_DURATION_SCALE` / reduced-motion.
- All icon-only controls carry a `contentDescription`.

### Rule 4 — Security & credential handling

- `sessionid`, `csrftoken`, `ds_user_id` are stored **only** in `EncryptedSharedPreferences` with an `AES256_GCM` value scheme and a Keystore-backed master key. Nowhere else — not DataStore, not Room, not a file, not an in-memory singleton that outlives the process need.
- **Never log cookie values.** Not at `VERBOSE`, not in debug builds, not truncated, not hashed-and-logged. Any log statement touching a cookie-bearing object must redact it.
- `OkHttp`'s `HttpLoggingInterceptor` is debug-only and must be configured with `redactHeader("Cookie")` and `redactHeader("Set-Cookie")`.
- Release builds strip logging via R8 (`-assumenosideeffects` on `android.util.Log`).
- `android:allowBackup="false"` and no `android:debuggable` in release. The encrypted prefs file is excluded from any backup rules regardless.
- The auth WebView: `setThirdPartyCookiesEnabled` scoped, no `addJavascriptInterface`, no JS injection into the login page, `setSavePassword(false)`, and cleared on sign-out via `CookieManager.removeAllCookies` + `WebStorage.deleteAllData`.

### Rule 5 — Honest failure

Extraction *will* break. When it does:
- Surface the actual failure class to the user (login required / rate limited / post removed / parser out of date), not a generic "Something went wrong".
- Never silently fall back to a lower-quality asset without saying so in the UI.
- Never retry an auth-failing request in a loop; that is how a user's account gets challenge-locked.

---

## 3. Technology Stack (permitted libraries)

| Domain | Library | Notes |
|---|---|---|
| Language | Kotlin 2.1+ | K2 compiler, `explicitApi()` off for app module |
| UI | Jetpack Compose (BOM) + Material 3 | Compose Compiler plugin, not the legacy extension |
| Navigation | Navigation Compose (type-safe routes) | `@Serializable` route objects |
| DI | Hilt (Dagger-Hilt) | KSP, not KAPT |
| Async | Coroutines + `StateFlow` / `SharedFlow` | No RxJava, no `LiveData` |
| Networking | OkHttp 5.x, Jsoup | Ktor Client permitted only if a concrete need appears; do not add both HTTP clients speculatively |
| Serialization | `kotlinx.serialization` | No Gson, no Moshi |
| Media playback | AndroidX Media3 (ExoPlayer) | |
| Image loading | Coil 3.x (Compose) | |
| Database | Room | KSP, schema exported to `app/schemas/` |
| Preferences | DataStore (Proto or Preferences) | Non-secret settings only |
| Secrets | `androidx.security:security-crypto` | Session cookies only |
| Background | WorkManager + Foreground Service | |
| Fallback extractor | `youtubedl-android` (yt-dlp) + bundled FFmpeg | |
| Testing | JUnit 5, Turbine, MockK, Robolectric, Compose UI Test | |

**Adding a dependency requires justification in the PR description.** Every added library is APK size, an attack surface, and a Rule 1 audit obligation.

---

## 4. Code Style & Naming

| Artifact | Convention | Example |
|---|---|---|
| Class files | PascalCase | `InstaNativeParser.kt` |
| Extension/util files | camelCase, plural noun | `stringExtensions.kt` |
| ViewModels | `[Feature]ViewModel` | `DownloadViewModel.kt` |
| Use cases | `[Verb][Entity]UseCase` | `ParseInstagramUrlUseCase.kt` |
| Repository interface | `[Entity]Repository` (in `domain`) | `CookieRepository.kt` |
| Repository impl | `[Entity]RepositoryImpl` (in `data`) | `CookieRepositoryImpl.kt` |
| UI state | immutable `data class` ending `State` | `HomeScreenUiState` |
| UI events | `sealed interface` ending `Event` | `HomeEvent` |
| Composable screens | `[Feature]Screen` | `DownloadsScreen.kt` |
| Hilt modules | `[Scope][Area]Module` | `SingletonNetworkModule.kt` |
| Test files | `[Subject]Test` | `ParseInstagramUrlUseCaseTest.kt` |

Formatting: ktlint (official Kotlin style), 4-space indent, 120-col soft limit, trailing commas on. Run `./gradlew ktlintFormat` before committing.

Use cases expose a single `operator fun invoke(...)`. Repositories return `Result<T>` or a domain-specific sealed result — never throw across a layer boundary, never leak `IOException` into `domain`.

---

## 5. Pre-Merge Verification Checklist

Run all of these. A green build is not the same as a correct one.

```bash
./gradlew ktlintCheck
./gradlew assembleDebug      # must be warning-free
./gradlew test               # unit tests, all modules
./gradlew lint               # Android Lint, no new errors
./gradlew assembleRelease    # verifies R8 rules don't strip something needed
```

Manual gates:

1. **URL regex suite** passes for every pattern in `docs/implementation-blueprint.md` §7, including the negative cases.
2. **Architecture test** passes: no `android.*` import anywhere under `domain/`.
3. **Leak check**: navigate Home → Preview → back, 10×, with LeakCanary attached. `ExoPlayer.release()` and Coil request disposal confirmed.
4. **Theme check**: pure-black surfaces render `#000000` (screenshot-sample the pixel), white type, no Monet tint bleeding through on an Android 12+ device with a coloured wallpaper.
5. **Cookie hygiene**: `adb logcat` during a full sign-in → download → sign-out cycle contains zero occurrences of the session value.
6. **Offline behaviour**: airplane mode mid-download → task moves to a retryable failed state, resumes on reconnect, no crash.

---

## 6. What an agent must not do unprompted

- Add any dependency that reaches a non-Meta network host.
- Introduce a server, an API key, or a cloud backend.
- Weaken Rule 4 to "make debugging easier".
- Replace the native parser with the yt-dlp path "because it's simpler" — the latency budget in `PRD.md` §5 depends on the native path being primary.
- Claim a feature works without having run the verification for it.
