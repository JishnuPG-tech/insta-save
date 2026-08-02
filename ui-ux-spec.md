# UI/UX Specification — Insta-Save

Complete screen-by-screen build spec. Every layout below assumes the pitch-black theme in `docs/design-system.md`: `#000000` canvas, `#FFFFFF` primary type, `#A3A3A3` secondary, `#0A0A0A` cards with 1dp `#262626` borders.

Wireframes are 360dp-wide phone portrait. Ⓘ marks an annotation.

---

## 0. Navigation Graph

```text
MainActivity (single Activity, edge-to-edge, black window background)
│
└── NavHost  startDestination = Home
     ├── Home                    ← ACTION_SEND / ACTION_VIEW land here
     │     └── FormatSheet       (modal bottom sheet, not a destination)
     │     └── ConfirmSheet      (share-sheet fast path)
     ├── Downloads
     ├── Preview/{shortcode}/{index}
     ├── Login
     └── Settings
           └── Settings/About
```

Type-safe routes:

```kotlin
@Serializable object Home
@Serializable object Downloads
@Serializable data class Preview(val shortcode: String, val index: Int)
@Serializable object Login
@Serializable object Settings
@Serializable object About
```

Bottom navigation bar (`#0A0A0A`, top hairline `#1A1A1A`, 80dp + nav inset) with three items: **Home** (`download`), **Downloads** (`history`, with a badge showing active count), **Settings** (`settings`). Selected item: white icon + white label, unselected `#A3A3A3`. Indicator pill `#1C1C1C`.

Preview and Login are full-screen destinations that **hide** the bottom bar.

Transitions: forward `slideInHorizontally(+30%) + fadeIn`, back the mirror, 300ms emphasized. Preview enters with a shared-element thumbnail expansion where available, otherwise `fadeIn + scaleIn(0.95f)`.

---

## 1. HomeScreen

### 1.1 Layout — idle / empty

```text
┌──────────────────────────────────────┐
│                    (status bar inset)│
│  Insta-Save                    ⚙︎    │ Ⓘ headlineMedium, large top bar
│                                      │   collapses to titleMedium on scroll
│  ┌────────────────────────────────┐  │
│  │ 🔗  Paste Instagram link       │  │ Ⓘ TextField, #1C1C1C, 12dp radius
│  │                          ⧉ 📋 │  │   trailing: clear (⧉) + paste (📋)
│  └────────────────────────────────┘  │   min 56dp tall
│                                      │
│  ┌────────────────────────────────┐  │
│  │  ⬇  Get media                  │  │ Ⓘ Filled white button, 48dp,
│  └────────────────────────────────┘  │   disabled (#1C1C1C / #5C5C5C)
│                                      │   until a valid URL is present
│                                      │
│              ┌───────┐               │
│              │   ⬇   │               │ Ⓘ 96dp outline glyph, #262626
│              └───────┘               │
│        Paste a link to start         │ Ⓘ titleMedium, #FFFFFF
│   Reels · Posts · Stories · Audio    │ Ⓘ bodyMedium, #A3A3A3
│                                      │
│  Recent                        See all│ Ⓘ only if history non-empty
│  ┌────┐┌────┐┌────┐┌────┐            │ Ⓘ 72dp thumbs, horizontal scroll
│  │    ││    ││    ││    │            │
│  └────┘└────┘└────┘└────┘            │
│                                      │
├──────────────────────────────────────┤
│   ⬇ Home    ⟳ Downloads   ⚙ Settings │
└──────────────────────────────────────┘
```

### 1.2 Clipboard chip

Appears between the text field and the CTA when a valid Instagram URL is on the clipboard and differs from the last consumed value:

```text
┌────────────────────────────────────┐
│ 📋  instagram.com/reel/Cx4… · Tap  │  Ⓘ #1C1C1C pill, full radius,
│                                 ✕  │    bodyMedium white, 48dp tall
└────────────────────────────────────┘
```

Behaviour rules:
- **Never auto-submits.** Tap fills the field and triggers extraction; ✕ dismisses and marks that URL consumed.
- On **Android 13+**, reading the clipboard raises a system toast. So do not read on every resume: render a passive "Paste from clipboard" affordance and read only on explicit tap. On Android 12 and below, the proactive chip is fine. Branch on `Build.VERSION.SDK_INT >= 33`.
- Toggleable in Settings ("Check clipboard on open", default on below API 33, off at/above).

### 1.3 States

| State | Rendering |
|---|---|
| **Idle/empty** | As drawn in §1.1 |
| **Invalid URL** | Field border turns `#FF5449`, helper text below: "That doesn't look like an Instagram link." CTA stays disabled |
| **Extracting (native)** | CTA becomes a disabled row with a 20dp indeterminate white spinner + "Fetching media…". A skeleton preview card fades in below |
| **Extracting (fallback)** | Same card, label changes to "Trying backup engine…" with a `labelMedium` `#A3A3A3` subline "This takes a few seconds". Ⓘ Do not hide the engine switch from the user — it explains the latency |
| **Preview ready** | Media card (§1.4) + "Choose format" filled CTA |
| **Login required** | Error card: `error_outline`, "Sign in to download this", body "Stories, highlights, and private accounts need an Instagram session.", actions **Sign in** (filled) / **Dismiss** (text) |
| **Rate limited** | Error card: "Instagram is rate-limiting this device", body "Wait a few minutes before trying again. Retrying now can get your account temporarily locked.", action **Dismiss** only — Ⓘ deliberately no Retry button |
| **Not found / removed** | "This post isn't available", body "It may have been deleted or made private." |
| **Parser failure** | "Couldn't read this post", body "Both extraction engines failed. This usually means Instagram changed something — check for an app update.", actions **Retry** / **Copy debug info** |
| **Offline** | "No connection", **Retry** |

### 1.4 Media preview card

```text
┌────────────────────────────────────┐
│ ┌────────────────────────────────┐ │
│ │                                │ │ Ⓘ 16:9 or 4:5 thumbnail,
│ │          [thumbnail]      ▶︎   │ │   large radius top, Coil,
│ │                          0:14  │ │   ▶︎ overlay if video,
│ └────────────────────────────────┘ │   duration pill bottom-right
│  ◉ @username                       │ Ⓘ 24dp avatar + titleMedium
│  Caption text truncated to two…    │ Ⓘ bodyMedium #A3A3A3, maxLines 2
│  ▦ Carousel · 5 items · 12 Mar     │ Ⓘ labelMedium #A3A3A3
│                                    │
│  ┌──────────────┐ ┌──────────────┐ │
│  │   Preview    │ │   Download   │ │ Ⓘ outlined | filled, 48dp
│  └──────────────┘ └──────────────┘ │
└────────────────────────────────────┘
```

Card: `#0A0A0A`, 1dp `#262626`, 16dp radius, zero shadow.

### 1.5 Share-sheet fast path

`ACTION_SEND` opens Home with the URL prefilled and extraction already running, then auto-raises a **confirm sheet**:

```text
┌────────────────────────────────────┐
│              ▬▬▬                   │
│  Download from Instagram           │  titleLarge
│  ┌────┐  @username                 │
│  │thmb│  Reel · 0:14 · 1080p       │
│  └────┘                            │
│  ┌────────────────────────────────┐│
│  │  Download now                  ││  filled — uses saved defaults
│  └────────────────────────────────┘│
│  ┌────────────────────────────────┐│
│  │  Choose format…                ││  outlined → opens FormatSheet
│  └────────────────────────────────┘│
└────────────────────────────────────┘
```

Ⓘ Two taps maximum from Instagram to a running download. That is the whole point of this path.

### 1.6 Events

```kotlin
sealed interface HomeEvent {
    data class UrlChanged(val value: String) : HomeEvent
    data object PasteClicked : HomeEvent
    data object ClipboardChipAccepted : HomeEvent
    data object ClipboardChipDismissed : HomeEvent
    data object ExtractClicked : HomeEvent
    data object RetryClicked : HomeEvent
    data object PreviewClicked : HomeEvent
    data object ChooseFormatClicked : HomeEvent
    data object QuickDownloadClicked : HomeEvent
    data object SignInClicked : HomeEvent
    data object ErrorDismissed : HomeEvent
}
```

---

## 2. FormatBottomSheet

Modal sheet, `#141414`, 28dp top corners, drag handle, max 90% height, internally scrollable with a **sticky footer CTA**.

```text
┌────────────────────────────────────┐
│              ▬▬▬                   │
│  Choose format                  ✕  │  titleLarge
│                                    │
│  ┌────────┬────────┬────────┐      │  Segmented, full radius
│  │ Video  │ Audio  │ Image  │      │  selected = white/black
│  └────────┴────────┴────────┘      │  Ⓘ hide segments the post lacks
│                                    │
│  QUALITY                           │  labelMedium #A3A3A3, tracking
│  ┌────────────────────────────────┐│
│  │ ◉ 1080 × 1920   MP4  · ~24 MB  ││  Ⓘ radio rows, 56dp,
│  │ ○  720 × 1280   MP4  · ~11 MB  ││    white radio when selected,
│  │ ○  480 × 854    MP4  · ~5 MB   ││    size = estimate, labelled "~"
│  └────────────────────────────────┘│
│                                    │
│  ITEMS  (5)          [Select all]  │  Ⓘ carousel/story only
│  ┌────┐┌────┐┌────┐┌────┐┌────┐    │  88dp tiles, 3-col grid
│  │ ☑ ││ ☑ ││ ☐ ││ ☑ ││ ☐ │        │  white check on selected,
│  └────┘└────┘└────┘└────┘└────┘    │  60% black scrim on unselected
│                                    │
│  EXTRAS                            │
│  ☑ Save caption as .txt            │  Ⓘ default ON
│  ☐ Save metadata as .json          │
│  ☐ Also extract audio track        │  Ⓘ video tab only
│                                    │
│  Saving to  Movies/InstaSave   ›    │  tappable → SAF picker
├────────────────────────────────────┤
│  ┌────────────────────────────────┐│  sticky footer, top hairline
│  │  Download 3 items · ~40 MB     ││  filled white, 48dp
│  └────────────────────────────────┘│
└────────────────────────────────────┘
```

**Audio tab:** MP3 320 / MP3 192 / M4A (original, no re-encode) / FLAC. Ⓘ Mark "M4A — original, no quality loss, fastest" as recommended, because it avoids a transcode entirely. Add "Embed metadata tags" checkbox, default on.

**Image tab:** Original JPEG (recommended) / WebP / PNG, plus resolution rows where multiple candidates exist.

Rules:
- Footer CTA disabled with label "Select at least one item" when nothing is ticked.
- Sizes are estimates from `content-length` probes or bitrate×duration; always prefixed `~`.
- Format choices persist as the defaults used by the share-sheet quick path.
- Rows are 56dp; the whole row is the tap target, not just the radio.

---

## 3. DownloadsScreen

```text
┌──────────────────────────────────────┐
│  Downloads                      ⋮    │  Ⓘ ⋮ = pause all / clear finished
│  ┌──────┬──────┬──────┐              │
│  │Active│ Done │Failed│              │  tabs, white indicator
│  └──────┴──────┴──────┘              │
│                                      │
│  ACTIVE ─────────────────────────────│
│  ┌──────────────────────────────────┐│
│  │┌────┐ reel_Cx4f2_01.mp4     ⏸ ✕ ││ Ⓘ 56dp thumb, titleMedium
│  ││thmb│ @username · 1080p         ││   filename truncates middle
│  │└────┘ ████████████░░░░░░  62%    ││   4dp bar, white on #262626
│  │       14.2 MB / 23.1 MB          ││   labelMedium #A3A3A3, tnum
│  │       2.4 MB/s · 4s left         ││
│  └──────────────────────────────────┘│
│  ┌──────────────────────────────────┐│
│  │┌────┐ post_Bx91k_03.jpg      ✕   ││
│  ││thmb│ Queued · position 2        ││ Ⓘ queued: no bar, no speed
│  │└────┘                            ││
│  └──────────────────────────────────┘│
│  ┌──────────────────────────────────┐│
│  │┌────┐ reel_Az22p.mp3        ⏸ ✕  ││
│  ││thmb│ Converting audio…          ││ Ⓘ MUXING: indeterminate bar
│  │└────┘ ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬        ││
│  └──────────────────────────────────┘│
│                                      │
│  COMPLETED ──────────────────────────│
│  ┌──────────────────────────────────┐│
│  │┌────┐ reel_Qw88s.mp4       ✓ ⋮   ││ Ⓘ ✓ = #4ADE80
│  ││thmb│ 18.4 MB · 2 min ago        ││   ⋮ = open/share/delete
│  │└────┘                            ││
│  └──────────────────────────────────┘│
├──────────────────────────────────────┤
│   ⬇ Home    ⟳ Downloads ②  ⚙ Settings│
└──────────────────────────────────────┘
```

**Failed row:** `error_outline` in `#FF5449`, reason line ("Network lost", "Login required", "File not found on server"), **Retry** text button. Ⓘ Show the real reason — a generic "Failed" is unactionable.

**States**
- Empty active: `download` glyph, "No active downloads", "Paste a link on the Home tab."
- Empty done: "Nothing downloaded yet."
- Empty failed: "No failures. Nice."

**Interactions**
- Swipe-to-dismiss on completed rows → delete record, with an **Undo** snackbar. Swipe on active rows is disabled (too destructive).
- Long-press → multi-select mode with a contextual top bar (select all / delete / share).
- Tap a completed row → PreviewScreen for the local file.
- Progress recomposition throttled to ≤4 Hz; the list uses stable `key = task.id` to avoid full re-layout.

**Notifications** (mirrors this screen)
- Single active: determinate progress, title = filename, text = `62% · 2.4 MB/s`, actions **Pause** / **Cancel**.
- Multiple: one summary "Downloading 3 items" with aggregate progress; individual items grouped beneath it.
- Complete: "Download finished", text = filename, actions **Open** / **Share**, auto-cancel on tap.
- Failed: "Download failed", text = reason, action **Retry**.
- Channel: `downloads`, importance `LOW` (silent, no heads-up) for progress; `DEFAULT` for completion. Ⓘ POST_NOTIFICATIONS runtime permission requested on API 33+ at the moment of the first download, not at launch, with a rationale sheet if previously denied.

---

## 4. PreviewScreen

Full-screen, black, bottom bar hidden, system bars auto-hide after 3s of no interaction.

```text
┌──────────────────────────────────────┐
│  ✕                        ⋮          │  overlay chrome, fades out
│                                      │
│                                      │
│           [ media surface ]          │  ExoPlayer / zoomable image
│                                      │
│                                      │
│  ────────●───────────  0:07 / 0:14   │  scrubber: white thumb,
│  ⏮   ⏯   ⏭            🔊  ⛶         │  #262626 track, tnum time
│                                      │
│  ● ○ ○ ○ ○                           │  pager dots, carousel only
│  ┌────────────────────────────────┐  │
│  │  ⬇ Download this item          │  │  filled white, 48dp
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
```

- Video: Media3 `PlayerView` in `AndroidView`, custom Compose controls (no default UI). **`ExoPlayer.release()` in `DisposableEffect` onDispose** — non-negotiable, it is the top leak source in this app.
- Images: horizontal `HorizontalPager` + pinch-zoom (1×–5×), double-tap to toggle 1×/2.5×, drag to pan when zoomed.
- Caption: swipe up or tap `⋮ → Caption` for a bottom sheet with full text, hashtags as chips, and a copy button.
- Autoplay muted on entry; volume state persists across items.
- Audio focus requested/abandoned correctly; playback pauses on transient loss.

---

## 5. LoginCookieScreen

Full-screen. Two phases.

### Phase 1 — informed consent (shown before any WebView loads)

```text
┌──────────────────────────────────────┐
│  ←  Sign in to Instagram             │
│                                      │
│              ┌───────┐               │
│              │   🔒  │               │  96dp outline glyph
│              └───────┘               │
│  Sign in to download Stories,        │  titleLarge, centered
│  Highlights, and private posts       │
│                                      │
│  ✓  You sign in on Instagram's own   │  bodyMedium, #A3A3A3
│     page. Insta-Save never sees your │  each row with a white ✓
│     password.                        │
│  ✓  Your session is encrypted on     │
│     this device and never leaves it. │
│  ✓  You can sign out and erase it    │
│     at any time.                     │
│  ⚠  Automated downloading may breach │  ⚠ in #FF5449
│     Instagram's Terms of Use. Your   │
│     account could be rate-limited or │
│     restricted. Use at your own risk.│
│                                      │
│  ┌────────────────────────────────┐  │
│  │  Continue to Instagram         │  │  filled white
│  └────────────────────────────────┘  │
│  ┌────────────────────────────────┐  │
│  │  Not now                       │  │  text button
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
```

Ⓘ The ⚠ row is mandatory and must not be collapsed behind a "learn more". Users deserve to know the account risk before, not after.

### Phase 2 — sandboxed WebView

```text
┌──────────────────────────────────────┐
│  ←  instagram.com          🔒  ⟳     │  Ⓘ read-only host label +
├──────────────────────────────────────┤     lock icon; not editable
│  ▬▬▬▬▬▬▬▬░░░░░░░░░░░░░░░░░░          │  page load progress, white
│                                      │
│        [ Instagram's own login ]     │
│        [ page, unmodified      ]     │
│                                      │
├──────────────────────────────────────┤
│  Insta-Save can't see what you type  │  labelMedium #A3A3A3, fixed
└──────────────────────────────────────┘
```

- Loads `https://www.instagram.com/accounts/login/` and nothing else; navigation outside `instagram.com` is blocked by `shouldOverrideUrlLoading` and opened in the system browser.
- No `addJavascriptInterface`. No JS injected into the login form. `setSavePassword(false)`.
- On detecting `sessionid` in `CookieManager`, capture → encrypt → pop back with a success snackbar "Signed in as @handle".
- Signed-in state in Settings shows the masked handle plus **Sign out & erase session**, which clears `EncryptedSharedPreferences`, `CookieManager.removeAllCookies`, and `WebStorage.deleteAllData`.

---

## 6. SettingsScreen

```text
┌──────────────────────────────────────┐
│  Settings                            │
│                                      │
│  ACCOUNT                             │
│  ◉ Signed in as @username        ›   │  or "Not signed in · Sign in"
│  Sign out & erase session            │  #FF5449 label
│                                      │
│  DOWNLOADS                           │
│  Save location                   ›   │  Movies/InstaSave
│  Filename template               ›   │  {author}_{shortcode}_{index}
│  Max simultaneous downloads      3   │  slider 1–5
│  Download over Wi-Fi only        ⬤  │  switch, off
│                                      │
│  MEDIA                               │
│  Default video quality           ›   │  Best available
│  Default audio format            ›   │  M4A (original)
│  Save caption as .txt            ⬤  │  ON
│  Save metadata as .json          ○   │  OFF
│                                      │
│  BEHAVIOUR                           │
│  Check clipboard on open         ⬤  │
│  Extraction engine               ›   │  Auto / Native only / Backup only
│  Confirm before downloading      ⬤  │
│                                      │
│  ABOUT                               │
│  Version                    1.0.0    │
│  Source code                     ›   │
│  Licences                        ›   │
│  Legal notice                    ›   │  Ⓘ ToS + copyright disclaimer
└──────────────────────────────────────┘
```

Section headers: `labelMedium`, `#A3A3A3`, uppercase, 0.5sp tracking, 24dp top padding. Rows 56dp (72dp with a subtitle). Switch track `#262626` off / `#FFFFFF` on with a `#000000` thumb.

---

## 7. Cross-Cutting Patterns

### State template

Every async surface implements all four:

```kotlin
sealed interface Ui<out T> {
    data object Loading : Ui<Nothing>
    data object Empty : Ui<Nothing>
    data class Error(val kind: ErrorKind, val retryable: Boolean) : Ui<Nothing>
    data class Content<T>(val data: T) : Ui<T>
}
```

### Error copy table

| Kind | Title | Body | Actions |
|---|---|---|---|
| `LoginRequired` | Sign in to download this | Stories, highlights, and private accounts need an Instagram session. | Sign in / Dismiss |
| `RateLimited` | Instagram is rate-limiting this device | Wait a few minutes. Retrying now can get your account temporarily locked. | Dismiss |
| `NotFound` | This post isn't available | It may have been deleted or made private. | Dismiss |
| `ParserOutdated` | Couldn't read this post | Both engines failed — Instagram likely changed something. Check for an app update. | Retry / Copy debug info |
| `Network` | No connection | Check your network and try again. | Retry |
| `StorageFull` | Not enough space | Free up space or pick another save location. | Change location |
| `PermissionDenied` | Storage permission needed | Insta-Save needs permission to save files. | Grant / Cancel |

Ⓘ No error string is ever "Something went wrong."

### Permissions

| Permission | When requested | Rationale UI |
|---|---|---|
| `POST_NOTIFICATIONS` (33+) | First download | Sheet: "Show download progress" |
| `WRITE_EXTERNAL_STORAGE` (≤28) | First download | Sheet: "Save files to your gallery" |
| SAF tree URI | Only when the user changes save location | System picker directly |

### Loading skeletons

Preview card skeleton: 16:9 `#141414` block + two grey lines (60% / 40% width). History row skeleton: 56dp square + two lines. Shimmer 1200ms.

### Snackbars

Anchored above the bottom nav, `#1C1C1C`, white label, white action.
"Download started" · "Saved to Movies/InstaSave" (action: Open) · "Removed" (action: Undo) · "Signed in as @handle" · "Session erased" · "Copied to clipboard"

### Responsive

- ≥600dp width: two-column layout — URL + preview left, downloads list right; format sheet becomes a centered dialog.
- Landscape phone: Preview goes fully immersive, controls overlay only.
- Font scale to 200% and largest display size must reflow without clipping.
