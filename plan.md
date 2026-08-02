## Scope

This workspace builds web apps and cannot compile Kotlin/Gradle or emit an `.apk`. Per your choice, I'll produce the **complete written specification** you take into Android Studio — no running UI here.

Design system is locked to **pitch black (#000000) surfaces with pure white (#FFFFFF) type**, not Material You dynamic tinting.

## Files I'll create

```text
/PRD.md
/architecture.md
/agent.md
/docs/design-system.md
/docs/ui-ux-spec.md
/docs/implementation-blueprint.md
/docs/gradle-setup.md
```

### 1. `PRD.md`
Executive summary, audience, vision. Full feature matrix (Reels, Posts/Carousels, Stories, Highlights, IGTV, Audio extraction, Profile pictures, Captions/metadata). UX features: clipboard auto-detect, ACTION_SEND share-sheet, format selector, batch queue, in-app preview, download manager. Privacy/cookie-sync section. Non-functional targets (launch <500ms, parse <1.2s, API 26–35, split ABI). Success criteria. Plus an explicit **legal & ToS risk section** — Instagram's terms prohibit automated scraping and Google Play policy forbids downloader apps of this kind; distribution realistically means F-Droid/GitHub sideload. You should read that before investing build time.

### 2. `architecture.md`
Clean Architecture layer diagram, module boundaries, UDF/StateFlow contract. Hybrid extraction pipeline (InstaNativeParser primary → YtDlpAndroidEngine fallback) with sequence diagrams for URL-extraction and download-execution flows. Scoped storage/MediaStore layout, SAF custom paths. Cookie encryption path via Android Keystore + EncryptedSharedPreferences. Room schema (entities, DAOs, migrations). WorkManager + Foreground Service lifecycle. Anti-detection strategy (UA rotation, header shaping) with an honest note on what TLS spoofing can and can't do from OkHttp.

### 3. `agent.md`
The five core rules (no relay servers, Clean Architecture enforcement, mobile touch standards, encrypted credentials, no plaintext cookie logging). Permitted library table with pinned versions. Naming conventions. Pre-commit verification checklist.

### 4. `docs/design-system.md` — pitch-black theme
- Color tokens: `Surface #000000`, `SurfaceContainer #0A0A0A`, `SurfaceContainerHigh #141414`, `Outline #262626`, `OnSurface #FFFFFF`, `OnSurfaceVariant #A3A3A3`, `Primary #FFFFFF` / `OnPrimary #000000`, `Error #FF5449`.
- Written as a ready-to-paste `Color.kt` + `Theme.kt` with `darkColorScheme()`, dynamic color explicitly disabled, edge-to-edge and status/nav bar config.
- Type scale (`Type.kt`), 4dp spacing scale, corner radius scale, elevation-by-tint rules (no shadows on pure black — use hairline outlines), 48dp touch targets, motion durations/easing.

### 5. `docs/ui-ux-spec.md` — complete UI/UX
Screen-by-screen ASCII wireframes and component specs for:
- **HomeScreen** — URL field, clipboard chip, empty/loading/preview/error states, recent downloads strip
- **FormatBottomSheet** — media-type tabs, resolution rows, audio format rows, carousel item checklist, sticky download CTA
- **DownloadsScreen** — active queue cards (progress, speed, ETA, pause/resume/cancel) + history with type filters
- **PreviewScreen** — ExoPlayer surface, image pager with zoom, caption sheet
- **LoginCookieScreen** — sandboxed WebView, consent copy, cookie-captured confirmation
- **SettingsScreen** — storage path, clipboard toggle, engine preference, theme, about
- Plus: navigation graph, share-sheet entry flow, notification designs, permission dialogs, snackbar/error copy, and every loading/empty/error/success state.

### 6. `docs/implementation-blueprint.md`
File-by-file plan for `com.instasave.app`: package tree, each class's responsibility, key public signatures, Hilt module wiring, ViewModel state/event sealed classes, and unit-test targets (URL regex table, JSON parser fixtures).

### 7. `docs/gradle-setup.md`
Copy-paste `settings.gradle.kts`, root and app `build.gradle.kts`, version catalog, `AndroidManifest.xml` (permissions, service, share-sheet intent filters), and R8 rules.

## Technical notes
- Docs only — no `.kt`, `.gradle`, or `.xml` files added to this repo, since a half-scaffolded Android project here would neither build nor be useful. All code lives inside the markdown as fenced, paste-ready blocks.
- Extraction reality check documented rather than glossed over: Instagram breaks unauthenticated endpoints frequently, and Stories/private content requires a logged-in session. The fallback engine is a mitigation, not a guarantee of the ">98% success" target.
