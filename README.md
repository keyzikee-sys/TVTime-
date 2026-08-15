# TVTime

A polished Android **home-screen widget** that launches Tubi with a customizable
glassmorphism ("frosted glass") style. Pick from built-in presets or fine-tune the
background, accent, border, corner radius, opacity and blur to match your home screen.

## Features

- **Two-page launcher widget** — *WatchList* opens Tubi (or its Play Store page if not
  installed); *My Stuff* jumps straight to widget customization. Flip between pages from
  the widget itself.
- **Glass customization** — Live-preview presets (Frosted Light, Dark Obsidian, Tinted
  Neon, Liquid Blur, Liquid No-Blur) plus manual control of background ARGB, accent color,
  stroke color, stroke width, corner radius and background opacity.
- **Real glass rendering** — the selected style is rendered to a bitmap and applied to the
  widget background (not just a static color), with the liquid preset adding a subtle
  highlight.
- **In-app config** — the *Widget Config* tab mirrors the widget styling with a live preview.

## Architecture

- Plain `AppCompatActivity` / `Fragment` shell with a `BottomNavigationView`
  (WatchList · My Stuff · Widget Config).
- `TVTimeWidgetProvider` (`AppWidgetProvider`) renders `widget_layout` and wires the
  page-flip and launch `PendingIntent`s.
- `GlassBitmapRenderer` draws the glass container; `ColorUtils` holds pure, testable color
  helpers; `WidgetPreferences` is the single source of truth for saved settings.
- Minimal dependency surface (AndroidX core / appcompat / material / constraintlayout).

## Build

Requires Android SDK (compileSdk 31) and JDK 11+.

```bash
./gradlew assembleDebug      # build the debug APK
./gradlew testDebugUnitTest  # run unit tests
```

Add the widget from your launcher's widget picker, then open **Widget Config** in the app
to style it.

## Testing

`ColorUtilsTest` covers hex parsing, alpha application and hex formatting.
