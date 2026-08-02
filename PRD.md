# Product Requirements Document — Insta-Save (Android)

**Status:** Draft v1.0
**Platform:** Android 8.0 (API 26) → Android 15 (API 35+)
**Distribution:** F-Droid / GitHub Releases (sideload). **Not Google Play** — see §8.
**License intent:** GPL-3.0 (matches the Seal/yt-dlp ecosystem it derives from)

---

## 1. Executive Summary

Insta-Save is an open-source, ad-free Android application that downloads media from Instagram URLs — Reels, Posts, Carousels, Stories, Highlights, IGTV, standalone audio tracks, profile pictures, and captions/metadata.

It is modelled on **Seal**, the open-source yt-dlp front-end for Android: a single-activity Jetpack Compose app with a paste-a-link home screen, a format-selection bottom sheet, a foreground-service download manager, and zero telemetry. Insta-Save keeps that interaction model and swaps the extraction layer for an Instagram-specific hybrid pipeline.

The visual identity departs from Seal in one deliberate way: **Insta-Save is pitch black (`#000000`) with pure white type**, with Material You dynamic colour explicitly disabled. See `docs/design-system.md`.

---

## 2. Target Audience & Vision

**Audience.** Android users who want to save Instagram media they have legitimate access to — their own posts, content they are licensed to reuse, archival of accounts they follow — without watermarks, ads, upload limits, or handing a URL to an unknown web service that logs it.

**Vision.** Become the reference open-source Instagram media downloader on Android: the UX polish of Seal, an honest privacy posture, and an extraction layer resilient enough to survive Instagram's frontend churn.

**Explicit non-goals.**
- No mass-scraping, no account harvesting, no follower/analytics tooling.
- No re-upload, no repost-with-watermark, no engagement automation.
- No server component of any kind. No account on our side. Nothing to log in to except Instagram itself.

---

## 3. Feature Specifications

### 3.1 Extraction & Download Matrix

| Media type | Capability | Auth required |
|---|---|---|
| **Reels** | Highest available progressive/DASH video (1080p+ where the CDN serves it), original audio track extraction to MP3/M4A, caption export | Public: no. Private: yes |
| **Posts / Carousels** | Multi-item batch selection — download all, or tick specific images/videos from a gallery post | Public: no. Private: yes |
| **Stories** | Single-slide download or the full 24h sequence for one account | **Always yes** |
| **Highlights** | Whole highlight reel or per-slide selection | **Always yes** |
| **IGTV / long-form video** | Full-length video, resolution picker | Public: no. Private: yes |
| **Audio / soundtracks** | Extract original audio from a Reel/Post to standalone MP3, M4A, or FLAC, with ID3/MP4 metadata tags (title, artist = author username, source URL, date) | Follows parent post |
| **Profile pictures** | Full-resolution avatar (`profile_pic_url_hd`) | Public: no. Private: yes |
| **Captions & metadata** | Post text, hashtags, mention list, like/view counts where exposed, upload timestamp, author handle + display name → `.txt` and/or `.json` sidecar | Follows parent post |

**Caption sidecars are ON by default** (writes `<basename>.txt` next to the media), toggleable in Settings. This was an open question in the original brief; defaulting on is the right call because captions are the cheapest thing to capture and the most annoying to retrieve later, and the sidecar costs ~1 KB.

### 3.2 UX Features (Seal-inspired)

| Feature | Behaviour |
|---|---|
| **Clipboard auto-detection** | On resume, inspect the clipboard. If it matches an Instagram URL pattern and differs from the last-consumed value, surface a dismissible "Paste `instagram.com/reel/…`" chip above the URL field. Never auto-submit. Android 12+ shows a system clipboard-read toast — the chip must appear *after* an explicit user tap on Android 13+ to avoid the toast on every launch. |
| **Share-sheet integration** | `Intent.ACTION_SEND` with `text/plain` from the Instagram app lands directly on a task-confirmation sheet with the media already resolving. Also registers `ACTION_VIEW` for `instagram.com` deep links. |
| **Format & resolution selector** | Bottom sheet: media-type segment (Video / Audio / Image), resolution rows, container rows (MP4 / MKV; MP3 / M4A / FLAC; JPEG original / WebP), and a per-item checklist for carousels and story sequences. |
| **Batch processing** | Queue of N items from one URL, or N URLs pasted newline-separated. Global concurrency limit configurable (default 3). |
| **In-app preview** | Media3 ExoPlayer for video, Coil 3 + zoomable pager for images — preview *before* committing to a download. |
| **Download manager** | Foreground service, per-item notification progress, aggregate notification when >1 active, live speed + ETA, pause/resume, retry-failed, and WorkManager re-queue after process death or network loss. |
| **History** | Room-backed, filterable by media type and account, re-download, open-in-gallery, delete-file-and-record. |

### 3.3 Privacy & Authentication

**100% local processing.** Every HTTP request goes device → `instagram.com` / `*.cdninstagram.com` / `*.fbcdn.net`. No relay, no proxy, no analytics SDK, no crash reporter that leaves the device. This is a hard architectural rule, enforced in `agent.md` Rule 1.

**Session sync.** Stories, Highlights, and anything from a private account require a logged-in session. Insta-Save offers an opt-in, sandboxed `WebView` pointed at `https://www.instagram.com/accounts/login/`. The user authenticates against Instagram directly; the app never sees the password fields, never injects JS into the login form, and never transmits credentials anywhere. On successful login the app reads `sessionid`, `ds_user_id`, and `csrftoken` out of the `CookieManager` and writes them to `EncryptedSharedPreferences` backed by an Android Keystore key.

Cookie handling constraints:
- Never written to logs, never included in crash traces, never rendered in the UI beyond a masked "Signed in as @handle".
- A single "Sign out & erase session" action clears the encrypted store *and* the WebView cookie jar.
- The app must function fully for public content with no session present. Auth is strictly additive.

### 3.4 Customisation & UI

- **Pitch-black theme.** `#000000` surfaces, `#FFFFFF` on-surface. Dynamic/Monet colour is *disabled by design* — see `docs/design-system.md`. A "Follow system light theme" option is out of scope for v1; the app is dark-only.
- **Storage.** Default scoped-storage targets: `Pictures/InstaSave`, `Movies/InstaSave`, `Music/InstaSave`. Custom directory via Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`), persisted URI permission.
- **Filename template.** Configurable token string, default `{author}_{shortcode}_{index}`.

---

## 4. Technical Overview

Full detail in `architecture.md`. Summary:

- **Kotlin 2.1+**, 100% Compose, no XML layouts (sole exception: the auth `WebView`, wrapped in `AndroidView`).
- **Clean Architecture** — `domain` (pure Kotlin, zero `android.*` imports) / `data` / `presentation`. MVVM with unidirectional data flow: immutable `StateFlow<UiState>` down, sealed `UiEvent` up.
- **Hilt** for DI, **Room** for history, **DataStore** for prefs, **EncryptedSharedPreferences** for the session.
- **Hybrid extraction:** `InstaNativeParser` (OkHttp + JSON-LD/GraphQL/embed parsing) as primary, `YtDlpAndroidEngine` (`youtubedl-android`) as fallback and as the audio-transcode/remux path via bundled FFmpeg.
- **Media3 ExoPlayer** for playback, **Coil 3** for images.
- **WorkManager** + a bound **Foreground Service** for the download queue.

---

## 5. Non-Functional Requirements

| Area | Target |
|---|---|
| Cold start | < 500 ms to first frame on a mid-range 2022 device |
| Parse latency | < 1.2 s p50 for a public Reel via the native parser; fallback path is expected to take 3–8 s |
| Memory | No leaked `ExoPlayer` or `ImageLoader` across navigation; verified with LeakCanary in debug |
| APK size | Split-ABI release (`arm64-v8a`, `armeabi-v7a`, `x86_64`); R8 full-mode + resource shrinking. Note: the yt-dlp + FFmpeg payload dominates — budget ~25–40 MB per ABI split, which is unavoidable if the fallback engine ships |
| Battery | Downloads batched under a foreground service; no periodic background polling of any kind |
| Compatibility | API 26 minimum (scoped storage branches for API < 29), API 35 target |
| Accessibility | 48dp minimum touch targets, TalkBack labels on all icon-only controls, contrast ≥ 7:1 (trivially met by white-on-black) |

---

## 6. Success Metrics

- Download success rate > 98% on **public** Reels and Posts, measured against a fixture set refreshed monthly.
- Zero crashes in background download tasks across a 100-item queue with network interruption injected.
- No dropped frames > 16 ms during list scroll and sheet transitions on a 120 Hz panel.
- Session-sync flow completes in under 60 s for a first-time user.

---

## 7. Release Criteria

1. `./gradlew assembleDebug` and `./gradlew test` pass clean.
2. All six screens implemented with every state (loading / empty / error / success) rendered.
3. Share-sheet entry from the official Instagram app resolves and downloads end to end.
4. Sign-out demonstrably clears both the encrypted store and the WebView cookie jar.
5. R8 release build strips all logging; a manual `strings`/decompile spot-check finds no cookie-adjacent log tags.

---

## 8. Legal, Policy & Feasibility Risks

**Read this before writing code.** These are not hypothetical.

**Instagram Terms of Use.** Meta's terms prohibit accessing the service by automated means and collecting data using automated methods without prior permission. An app whose entire purpose is programmatic media retrieval is in violation regardless of how it is engineered. Accounts used for session sync can be rate-limited, challenge-locked, or disabled. That risk lands on the end user, and the app must say so plainly at the point of sign-in, not bury it in an about screen.

**Copyright.** Downloading does not confer any licence. The app should surface a one-time notice that redistribution of downloaded media may infringe the uploader's rights.

**Google Play.** Play's Device and Network Abuse policy bars apps that facilitate unauthorised downloading from third-party services; this class of app is routinely removed. Plan for F-Droid and GitHub Releases. F-Droid additionally requires reproducible, FOSS-only builds — the bundled yt-dlp/FFmpeg binaries need a documented build recipe or the submission will be rejected.

**Extraction fragility — the honest version.** Instagram aggressively rate-limits and gates unauthenticated access; anonymous fetches of `/p/<shortcode>/` and the embed endpoints frequently return a login wall rather than JSON-LD, and the pattern changes without notice. `query_hash` GraphQL parameters rotate. The `>98%` public-content target in §6 is an aspiration contingent on the parser being actively maintained; the yt-dlp fallback is a mitigation, not a guarantee — its Instagram extractor breaks too, and when it does you are waiting on an upstream release.

**On TLS fingerprint spoofing.** The original brief called for "TLS Client Hello spoofing" via OkHttp. Be precise about what is achievable: OkHttp on Android uses Conscrypt/BoringSSL and lets you constrain the cipher suite list and `ConnectionSpec`, which shifts the JA3 fingerprint somewhat. It does **not** give you a byte-accurate Chrome Client Hello — extension ordering, GREASE values, and ALPS are not exposed. Achieving that requires a uTLS-equivalent native layer, which is a large, separate project. Ship realistic User-Agent and `X-IG-App-ID`/`Sec-Fetch-*` header shaping, document the cipher-list tweak as best-effort, and do not promise fingerprint parity in the README.

**Recommended posture.** Position Insta-Save as a personal archival tool for content the user has rights to, gate the destructive-risk features (session sync) behind an informed-consent screen, and keep the legal notice in the repo README, not just here.
