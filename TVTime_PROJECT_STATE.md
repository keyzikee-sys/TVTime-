# TVTime Project — Permanent State / Handoff

## PROJECT ID
TVTime — Tubi TVTime / Liquid Glass Widget

## PROJECT LOCATION
/storage/internal_new/project/TVTime

## PURPOSE
Android TVTime app/widget centered around Tubi content, My Stuff,
Watchlist, and Terror on Tubi.

---

# CURRENT WORKING RULES

- Work step-by-step.
- Give ONE small terminal instruction at a time.
- Do NOT use `sed` unless specifically requested.
- Prefer `cat > file <<'EOF'` for complete file replacements.
- Never assume a change succeeded; verify it.
- Preserve working functionality when changing the visual theme.
- Build only after the relevant edits have been verified.
- Keep track of the exact stopping point.

---

# CURRENT BRAND / COLOR DIRECTION

Final TVTime palette:

- Tubi Pink: #FF00A6
- Tubi Broom: #FFFF13
- Black Russian: #0B0019

The old cyan / purple / neon-green visual identity is being removed.

Old colors that should NOT remain in active source:
- #FF1493
- #FF007F
- #00A82D
- #00E5FF
- #1E1E1E
- #0F0C1B

Old color naming that should NOT remain in active source:
- Cyan
- Neon Green
- Purple
- Dark Purple
- tubi_green
- accent_cyan

Backup files may still contain old values. Those are intentionally excluded
from active-source checks.

---

# CURRENT GLASS DESIGN

Liquid/glass visual direction:

- Black Russian base
- Tubi Pink accent
- Tubi Broom secondary accent
- Translucent glass surfaces
- White glass highlight/refraction
- Pink glass border
- Rounded corners
- No cyan branding

Active glass background:
app/src/main/res/drawable/glass_background_neon.xml

Active glass selector:
app/src/main/res/drawable/glass_item_selector.xml

---

# CURRENT COLOR RESOURCE

Active file:
app/src/main/res/values/colors.xml

Current intended palette includes:

tubi_pink = #FF00A6
tubi_broom = #FFFF13
black_russian = #0B0019

glass_surface = #331A0B19
glass_surface_dark = #1F0B0019
glass_surface_tint = #2B120022
glass_highlight = #4DFFFFFF
glass_border = #66FF00A6
glass_item_fill = #1AFFFFFF
glass_item_border = #33FFFFFF

text_primary = #F2F4F7
text_secondary = #AEB6C2
text_muted = #737D8C
text_white = #F2F4F7

progress_track = #303642
btn_delete = #FF5252

Compatibility aliases currently retained:
neon_pink
neon_pink_soft
bg_dark
bg_panel
card_bg
card_bg_light
colorPrimary
colorPrimaryDark
colorAccent

IMPORTANT:
tubi_green and accent_cyan were intended to be removed from the active
colors.xml.

---

# GLASS PRESETS

Active file:
app/src/main/java/com/tvtime/app/GlassPresets.kt

Final preset direction:

LIGHT:
background #E60B0019
accent #FF00A6
border #66FF00A6
corner radius 20
border thickness 2

DARK:
background #E60B0019
accent #FFFF13
border #66FFFF13
corner radius 20
border thickness 2

TINTED:
background #E60B0019
accent #FF00A6
border #66FFFF13
corner radius 22
border thickness 2

LIQUID:
background #D90B0019
accent #FF00A6
border #88FFFF13
corner radius 26
border thickness 2

---

# IMPORTANT ACTIVE FILES

Main application:
app/src/main/

Widget:
app/src/main/java/com/tvtime/app/TVTimeWidgetProvider.kt
app/src/main/java/com/tvtime/app/WatchlistWidgetService.kt
app/src/main/java/com/tvtime/app/WidgetConfigureActivity.kt
app/src/main/java/com/tvtime/app/WidgetConfigFragment.kt
app/src/main/java/com/tvtime/app/WidgetPreferences.kt
app/src/main/java/com/tvtime/app/GlassPresets.kt
app/src/main/java/com/tvtime/app/WatchlistStore.kt
app/src/main/java/com/tvtime/app/TubiRepository.kt

Layouts:
app/src/main/res/layout/widget_layout.xml
app/src/main/res/layout/widget_glass_layout.xml
app/src/main/res/layout/widget_list_item.xml
app/src/main/res/layout/list_item_show.xml
app/src/main/res/layout/fragment_widget_config.xml
app/src/main/res/layout/fragment_mystuff.xml
app/src/main/res/layout/fragment_watchlist.xml
app/src/main/res/layout/terror_item.xml

Glass resources:
app/src/main/res/drawable/glass_background_neon.xml
app/src/main/res/drawable/glass_item_selector.xml

Colors:
app/src/main/res/values/colors.xml

---

# WATCHLIST DATA

WatchlistStore stores:

- Watchlist
- My Stuff
- Terror on Tubi

Preferences:
TVTimeWatchlists

Keys:
watchlist_items
mystuff_items
terror_on_tubi_items

ShowItem stores:
- title
- subtitle
- progress
- imageUrl
- watchUrl

Terror on Tubi currently has seeded local entries and can later be
replaced/updated with real Tubi/API data.

---

# CURRENT WATCHLIST WIDGET SERVICE

WatchlistWidgetService.kt currently:

- Loads My Stuff using WatchlistStore.getMyStuff()
- Reads widget colors from tvtime_prefs
- Supports DynamicWidgetColors
- Uses saved title/subtitle colors as fallback
- Displays title
- Displays subtitle
- Displays progress
- Uses widget_list_item.xml
- Supports fill-in click intents

Current default title color was changed from old green to Tubi Broom:
#FFFF13

---

# MAIN APP SHOW ITEM

Active file:
app/src/main/res/layout/list_item_show.xml

Current design:
- Glass item selector
- 72dp x 104dp poster
- Tubi Pink title
- Tubi Pink progress label
- Muted secondary text
- Description
- Progress bar
- Tubi Pink progress

There is currently a malformed-looking XML line around the delete TextView
that must be checked before final build:

tv_delete line previously appeared as:
android:text="✕"android:contentDescription="Remove item"

Verify this before building if it has not already been corrected.

---

# WIDGET PAGES

Existing widget concept:

PAGE 1:
MY STUFF

PAGE 2:
TERROR

The second widget page is intended to be preserved and improved without
breaking page 1.

Terror on Tubi should retain a stronger Tubi Pink identity while still
using the final palette.

---

# TUBI REPOSITORY

TubiRepository.kt contains:
- fetchUserLists
- Tubi queue/content fetching
- fetchTerrorOnTubi

Terror fetching currently uses the Tubi true_horror category HTML and
regex parsing.

This may need further improvement later so Terror on Tubi uses real
content rather than only seeded local entries.

---

# PREVIOUS BUILD INFORMATION

A successful build was previously reached.

Previously reported APK:
app/build/outputs/apk/debug/app-debug.apk

Approximate size:
4.5–4.6 MB

A previous goal was to install and test the APK on the phone.

Do NOT assume the newest APK is current until a new build is performed.

---

# BACKUPS

Known backup files include:

app/src/main/res/drawable/glass_background_neon.xml.backup
app/src/main/res/values/colors.xml.backup
app/src/main/java/com/tvtime/app/GlassPresets.kt.backup

Backup files may contain the old palette. Do not treat them as active
source when checking the final palette.

---

# CURRENT IMMEDIATE STOPPING POINT

The final colors.xml was just rewritten successfully using a cat heredoc.

The intended active colors.xml no longer contains:
- tubi_green
- accent_cyan

The next task is to VERIFY colors.xml and then inspect/fix the remaining
old preset names in WidgetConfigFragment.kt.

Remaining known old preset labels found in active source:

WidgetConfigFragment.kt around lines 1187–1192:
- Cyan
- Neon Green
- Purple
- Dark Purple

These need to be replaced with the final TVTime naming/theme.

---

# KNOWN PREVIOUS PROBLEMS

Previously encountered build errors included:

1. WatchlistWidgetService.kt:
   unresolved reference to setProgressBarTintList

2. WatchlistWidgetService.kt:
   type mismatch where an Int was supplied where String was expected

These were worked through and a successful build was previously achieved,
but verify current source before assuming those problems are permanently
resolved.

---

# FINAL PALETTE CHECK

Active-source verification should exclude:
*.backup
*.swp

The final check should search for:

#FF1493
#FF007F
#00A82D
#00E5FF
#1E1E1E
#0F0C1B
"Cyan"
"Neon Green"
"Dark Purple"
"Purple"
tubi_green
accent_cyan

Any remaining active-source match should be investigated rather than
automatically deleted.

---

# PROJECT CONTINUATION INSTRUCTIONS

When continuing this project in another ChatGPT chat:

1. Read this file first.
2. Treat this file as the project handoff/state document.
3. Check the actual files before making assumptions.
4. Continue from CURRENT IMMEDIATE STOPPING POINT.
5. Do one small change at a time.
6. Verify each change.
7. Keep this file updated whenever a significant project decision,
   file change, build result, error, or stopping point changes.

