# Design System — Insta-Save

**Theme:** pitch black, pure white. Dark-only. Material You dynamic colour **disabled by design**.

Rationale: Insta-Save's content is imagery. A true `#000000` canvas removes every competing hue, gives OLED power savings, and keeps thumbnails as the only chromatic element on screen. Monet tinting would inject wallpaper-derived colour into surfaces and defeat that. This is a deliberate departure from Seal.

---

## 1. Colour Tokens

| Role | Hex | Usage |
|---|---|---|
| `Surface` | `#000000` | App background, scaffold, all screen roots |
| `SurfaceContainerLowest` | `#000000` | Behind scrolled content |
| `SurfaceContainer` | `#0A0A0A` | Cards, list rows, bottom sheet body |
| `SurfaceContainerHigh` | `#141414` | Elevated sheet header, dialog, menu |
| `SurfaceContainerHighest` | `#1C1C1C` | Pressed/hover fill, text-field container |
| `Outline` | `#262626` | Hairline dividers, card borders, input borders |
| `OutlineVariant` | `#1A1A1A` | Subtle separators inside a card |
| `OnSurface` | `#FFFFFF` | Primary text, active icons |
| `OnSurfaceVariant` | `#A3A3A3` | Secondary text, metadata, inactive icons |
| `OnSurfaceDisabled` | `#5C5C5C` | Disabled labels, drag handle |
| `Primary` | `#FFFFFF` | Filled buttons, active selection, progress fill |
| `OnPrimary` | `#000000` | Text/icon on a filled white button |
| `Secondary` | `#1C1C1C` | Tonal button container |
| `OnSecondary` | `#FFFFFF` | Text on tonal button |
| `Error` | `#FF5449` | Error text, failed-task badge |
| `OnError` | `#000000` | On an error-filled surface |
| `ErrorContainer` | `#2A0E0C` | Error banner background |
| `Success` | `#4ADE80` | Completed badge, checkmark (custom token) |
| `Scrim` | `#000000` @ 60% | Behind modal sheets and dialogs |

Only three chromatic values exist in the entire system — `Error`, `ErrorContainer`, `Success`. Everything else is a greyscale step. Resist adding a fourth.

### Elevation on pure black

Material's tonal-elevation model tints surfaces with the primary colour. With a **white** primary on black, that produces grey washes that read as accidental. So: **disable tonal elevation and express hierarchy with the container steps above plus 1dp hairline `Outline` borders.** Drop shadows are invisible on `#000000` — set `shadowElevation = 0.dp` on every card and sheet.

### `Color.kt`

```kotlin
package com.instasave.app.presentation.theme

import androidx.compose.ui.graphics.Color

val Black             = Color(0xFF000000)
val Surface0A         = Color(0xFF0A0A0A)
val Surface14         = Color(0xFF141414)
val Surface1C         = Color(0xFF1C1C1C)
val Outline26         = Color(0xFF262626)
val OutlineVar1A      = Color(0xFF1A1A1A)
val White             = Color(0xFFFFFFFF)
val GreyA3            = Color(0xFFA3A3A3)
val Grey5C            = Color(0xFF5C5C5C)
val ErrorRed          = Color(0xFFFF5449)
val ErrorContainerRed = Color(0xFF2A0E0C)
val SuccessGreen      = Color(0xFF4ADE80)
```

### `Theme.kt`

```kotlin
package com.instasave.app.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val InstaSaveColorScheme = darkColorScheme(
    primary                 = White,
    onPrimary               = Black,
    primaryContainer        = Surface1C,
    onPrimaryContainer      = White,
    secondary               = Surface1C,
    onSecondary             = White,
    secondaryContainer      = Surface14,
    onSecondaryContainer    = White,
    tertiary                = White,
    onTertiary              = Black,
    background              = Black,
    onBackground            = White,
    surface                 = Black,
    onSurface               = White,
    surfaceVariant          = Surface14,
    onSurfaceVariant        = GreyA3,
    surfaceContainerLowest  = Black,
    surfaceContainerLow     = Surface0A,
    surfaceContainer        = Surface0A,
    surfaceContainerHigh    = Surface14,
    surfaceContainerHighest = Surface1C,
    outline                 = Outline26,
    outlineVariant          = OutlineVar1A,
    error                   = ErrorRed,
    onError                 = Black,
    errorContainer          = ErrorContainerRed,
    onErrorContainer        = ErrorRed,
    scrim                   = Black,
)

/** Tokens Material 3 has no slot for. */
data class ExtraColors(val success: Color = SuccessGreen)
val LocalExtraColors = staticCompositionLocalOf { ExtraColors() }

@Composable
fun InstaSaveTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    CompositionLocalProvider(LocalExtraColors provides ExtraColors()) {
        MaterialTheme(
            colorScheme = InstaSaveColorScheme,
            typography  = InstaSaveTypography,
            shapes      = InstaSaveShapes,
            content     = content,
        )
    }
}
```

`dynamicDarkColorScheme` is **never** called. `isSystemInDarkTheme()` is never branched on — the app is dark-only.

---

## 2. Typography

Font: **system default** (Roboto / device sans). No bundled font — it costs APK size for no identity gain in a monochrome UI.

| Style | Size / Line | Weight | Tracking | Usage |
|---|---|---|---|---|
| `displaySmall` | 36 / 44 | 400 | 0 | Empty-state headline |
| `headlineMedium` | 28 / 36 | 600 | 0 | Screen title (large top bar) |
| `titleLarge` | 22 / 28 | 600 | 0 | Sheet title |
| `titleMedium` | 16 / 24 | 600 | 0.15 | Card title, list-row primary |
| `bodyLarge` | 16 / 24 | 400 | 0.5 | URL input, body copy |
| `bodyMedium` | 14 / 20 | 400 | 0.25 | Captions, descriptions |
| `labelLarge` | 14 / 20 | 600 | 0.1 | Button text |
| `labelMedium` | 12 / 16 | 500 | 0.5 | Metadata, chips, timestamps |
| `labelSmall` | 11 / 16 | 500 | 0.5 | Badge text, progress percentage |

Numeric readouts (speed, ETA, byte counts) use `fontFeatureSettings = "tnum"` so digits don't jitter while updating.

```kotlin
val InstaSaveTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    titleLarge     = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium    = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.15.sp),
    bodyLarge      = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium     = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    labelLarge     = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
)

val MonoNumeric = TextStyle(fontFeatureSettings = "tnum")
```

---

## 3. Spacing

4dp base scale. Named tokens; no magic numbers in composables.

| Token | dp | Usage |
|---|---|---|
| `xs` | 4 | Icon-to-label gap |
| `sm` | 8 | Chip padding, tight stacks |
| `md` | 12 | Inside-card padding |
| `lg` | 16 | **Screen horizontal margin**, standard gap |
| `xl` | 24 | Section separation |
| `xxl` | 32 | Above a primary CTA |
| `xxxl` | 48 | Empty-state breathing room |

```kotlin
object Spacing {
    val xs = 4.dp;  val sm = 8.dp;   val md = 12.dp;  val lg = 16.dp
    val xl = 24.dp; val xxl = 32.dp; val xxxl = 48.dp
}
```

List row min height 56dp; with secondary text 72dp; with a 64dp thumbnail 88dp.

---

## 4. Shape

| Token | Radius | Applied to |
|---|---|---|
| `extraSmall` | 4dp | Badges, progress track |
| `small` | 8dp | Chips, small thumbnails |
| `medium` | 12dp | Text fields, list-row thumbnails |
| `large` | 16dp | Cards, media preview |
| `extraLarge` | 28dp | Bottom-sheet top corners, FAB, dialogs |
| `full` | 50% | Pill buttons, filter chips, avatars |

```kotlin
val InstaSaveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
```

---

## 5. Component Rules

**Cards.** `#0A0A0A`, `large` radius, **1dp `#262626` border**, `shadowElevation = 0.dp`, `tonalElevation = 0.dp`. The border is the only thing separating the card from the black canvas — without it the card is invisible.

**Buttons.**
| Variant | Container | Label | Shape | Height |
|---|---|---|---|---|
| Filled (primary) | `#FFFFFF` | `#000000` | `full` | 48dp |
| Tonal | `#1C1C1C` | `#FFFFFF` | `full` | 48dp |
| Outlined | transparent + 1dp `#262626` | `#FFFFFF` | `full` | 48dp |
| Text | none | `#FFFFFF` | — | 48dp target |
| Icon | none | `#FFFFFF` active / `#A3A3A3` inactive | `full` | 48dp target, 24dp glyph |

**Text fields.** Filled `#1C1C1C`, `medium` radius, white text, `#A3A3A3` placeholder, focus indicator is a 2dp **white** underline/border — there is no coloured accent to use.

**Bottom sheets.** `#141414`, `extraLarge` top corners, 32×4dp `#5C5C5C` drag handle, 60% black scrim. Sticky footer CTA sits on the same `#141414` with a top hairline.

**Progress.** Track `#262626`, indicator `#FFFFFF`, 4dp tall, `extraSmall` radius. Indeterminate **only** where progress is genuinely unknown (extraction); downloads are always determinate.

**Chips.** Unselected: transparent + 1dp `#262626`, `#A3A3A3` label. Selected: `#FFFFFF` container, `#000000` label. `full` shape, 32dp height, 48dp touch target via padding.

**Dividers.** 1dp `#1A1A1A`. Prefer spacing; use dividers only in dense settings lists.

**Ripple.** White — 12% pressed, 8% hovered, 10% focused.

**Skeletons.** `#141414` base, shimmer sweeping to `#1C1C1C`, 1200ms loop. Used for the media preview card and history list only — never buttons.

**Snackbar.** `#1C1C1C` container, white label, white text action, `medium` radius, above the nav bar inset.

---

## 6. Iconography

Material Symbols Rounded, 24dp, weight 400.

`content_paste` · `link` · `download` · `pause` · `play_arrow` · `close` · `check` · `refresh` · `folder_open` · `settings` · `history` · `image` · `movie` · `music_note` · `person` · `auto_stories` · `error_outline` · `login` · `logout` · `more_vert` · `share` · `delete` · `check_circle`

---

## 7. Motion

| Interaction | Duration | Easing |
|---|---|---|
| Ripple / state change | 100 ms | `LinearEasing` |
| Button press scale (0.97×) | 120 ms | `FastOutSlowInEasing` |
| Screen transition (slide + fade) | 300 ms | Emphasized |
| Bottom sheet enter | 350 ms | Emphasized decelerate |
| Bottom sheet exit | 250 ms | Emphasized accelerate |
| Skeleton shimmer | 1200 ms | `LinearEasing`, infinite |
| Progress value change | 200 ms | `LinearEasing` — animate, never snap |
| Snackbar | 200 in / 150 out | `FastOutSlowInEasing` |

Respect reduced motion: when `ANIMATOR_DURATION_SCALE == 0`, swap transitions for instant cross-fades.

---

## 8. Accessibility

- White on black = 21:1. `#A3A3A3` on black = 9.1:1. Both AAA. `#5C5C5C` is decorative/disabled only and must never be the sole carrier of information.
- Touch targets ≥ 48×48dp, no exceptions.
- Every icon-only control has a `contentDescription`.
- Progress exposed via `semantics { progressBarRangeInfo = ... }`.
- Layouts reflow to 200% font scale and largest display size — no fixed-height text containers.
- On pure black, pure white body text at small sizes can halate on OLED. Body copy uses `#FFFFFF` at 14sp+ only; anything smaller drops to `#A3A3A3`.
