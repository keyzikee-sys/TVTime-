# TVTime

A polished Android **home-screen widget** that launches Tubi with a customizable
glassmorphism ("frosted glass") style. Pick from built-in presets or fine-tune the
background, accent, border, corner radius, opacity and blur to match your home screen.

## Features

- **Two-page launcher widget** — *WatchList* opens your chosen streaming app (or its Play
  Store page if not installed); *My Stuff* jumps straight to widget customization. Flip
  between pages from the widget itself.
- **Multiple streaming services** — launch Tubi, Netflix, Prime Video, Disney+, Hulu or
  YouTube, selected in the config UI.
- **Glass customization** — Live-preview presets (Frosted Light, Dark Obsidian, Tinted
  Neon, Liquid Blur, Liquid No-Blur) plus manual control of background ARGB, accent, stroke,
  stroke width, corner radius and background opacity.
- **Real glass rendering** — the selected style is rendered to a bitmap and applied to the
  widget background (not just a static color). On Android 12+ a true `RenderEffect` blur is
  applied to the fill; the liquid preset also adds a subtle highlight.
- **Configure on add** — when you place the widget, a configuration screen opens so you can
  style it before it appears on the home screen.
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

## Release signing

Debug builds need no setup. To produce a **signed release APK**, add the following to
`local.properties` (which is git-ignored):

```properties
RELEASE_STORE_FILE=../tvtime-release.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=tvtime
RELEASE_KEY_PASSWORD=your_key_password
```

Generate a keystore once with:

```bash
keytool -genkey -v -keystore tvtime-release.jks -alias tvtime -keyalg RSA -keysize 2048 -validity 10000
```

The `release` build type automatically picks these up; without them it produces an
unsigned APK.

## Testing

`ColorUtilsTest` covers hex parsing, alpha application and hex formatting.
