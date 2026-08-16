## Goal
- Build/polish the TVTime glass-widget Android app (3-tab launcher + configurable glass widget) showing real Tubi data: catalog titles and the user's synced personal Tubi queues.

## Constraints & Preferences
- 3 tabs: WatchList, My Stuff, Widget Config.
- API key `pmx_b7c9c4361f273e03c70d48956f1891e6` (parse.bot) must NEVER be hardcoded/committed — stored only in SharedPreferences.
- Build requires `android.aapt2FromMavenOverride=/usr/bin/aapt2` glibc/aapt2 shim.
- No git remote; user installs APK via Android IDE. Git user: KeyziKee-sys.
- Widget must show real movie posters (not bare glass) and be uncluttered.

## Progress
### Done
- parse.bot catalog via plain REST: `GET https://api.parse.bot/scraper/{scraper_id}/{endpoint_name}` + `X-API-Key`. Public Tubi scraper `3b4482fa-50a4-475d-a612-75d5c78654eb`.
- Catalog loader `loadTubiCatalog` fetches `list_categories?limit=100`, filters personalized/linear slugs, fetches `list_content?category=<slug>&limit=10` for first 8 categories, merges+dedupes; stores `poster_url`→imageUrl, `watch_url`→watchUrl.
- Committed `4b1f8f6` "Show real show list in widget + sync user's Tubi history".
- Tubi personal-history sync: `TubiRepository.fetchUserLists` GETs `https://user-queue.production-public.tubi.io/api/v2/queues` with `Bearer <jwt>`. The queue API returns only `content_id` + `type`, so `parseQueueIds` collects them and `fetchContentDetails` batch-resolves via `https://content.production-public.tubi.io/cms/content?content_ids=<csv>&platform=web&device_id=<x>` → maps id→{title, description/year, thumbnails[]}. Mapping (final): **WatchList** = continue_watching + view_history; **My Stuff** = bookmarks (`watch_later`/`saved`/my_list). Also tries `view_history` endpoints best-effort.
- **WatchList recommendations**: `WidgetConfigFragment.loadTubiCatalog` now primarily loads Tubi's `recommended_for_you` category (`list_content?category=recommended_for_you&limit=20` via parse.bot) into WatchList, falling back to merged general categories. `syncTubi` fills WatchList with recommendations when continue-watching is empty.
- `TubiAccount.login` fetches JWT from `/oz/auth/loadAuth` via `connect.sid` cookie; `authDebug` diagnostic; sync status shows HTTP code + loadAuth body.
- Widget redesigned (this session): collection now a 2-column **GridView poster wall** (`grid_watchlist`/`grid_mystuff`) of poster cards (poster + title/year + cyan progress bar over gradient scrim). Compact **tab header** (WatchList | My Stuff) flips the ViewFlipper; bottom buttons removed. Accent changed from magenta `#FFFF1493` to cyan `#4DD0E1` (defaults in WidgetPreferences, WidgetConfigFragment, GlassPresets, MainActivity, progress tint). Poster cache download bumped to 100×150.
- Build green after redesign (no errors).

### In Progress
- (none)

### Blocked
- (none)

## Key Decisions
- parse.bot used as plain REST (simpler than MCP JSON-RPC) — confirmed working with real titles.
- Widget is a RemoteViewsService collection. Android home widgets cannot do a true horizontal RecyclerView scroll, so "horizontal posters" is implemented as a 2-column GridView poster wall.
- Personal Tubi data needs JWT Bearer; `/oz/auth/loadAuth` fetch added. Queue items carry only `content_id` → resolved via content cms endpoint.
- Account-service login (`tus-http.../user/login`) is nginx-401 gated → abandoned in favor of loadAuth JWT.
- Tab switching uses broadcast `ACTION_PAGE` with `page` extra; `WidgetPreferences.currentPage` persisted; active tab highlighted in accent, inactive gray `#8A93A3`.

## Next Steps
- Reinstall APK, re-sign-in (captures JWT), tap "Load Catalog" or "Sync my Tubi" → WatchList shows recommended_for_you posters, My Stuff shows 37 bookmarks.
- Verify view-history endpoint actually returns items (currently best-effort; if it 404s, WatchList relies on recommendations fallback).
- Commit the widget-redesign + content-resolution + recommendations changes when confirmed working.

## Critical Context
- Build: `export ANDROID_HOME=/opt/android_sdk JAVA_HOME=/opt/java/jdk-11.0.32+9 ANDROID_SDK_ROOT=/opt/android_sdk PATH=$JAVA_HOME/bin:$PATH && ./gradlew assembleDebug -Pandroid.aapt2FromMavenOverride=/usr/bin/aapt2`
- parse.bot: `GET …/scraper/3b4482fa-50a4-475d-a612-75d5c78654eb/list_content?limit=50` + `X-API-Key`; categories `list_categories?limit=100` → `categories[].slug`; filter `list_content?category=<slug>&limit=10`.
- Tubi user-queue: `GET https://user-queue.production-public.tubi.io/api/v2/queues` Header `Authorization: Bearer <jwt>`. 401 if no/invalid token. Response `queues[]` with `type` (watch_later/continue_watching/...) + `content_id`.
- Tubi content: `GET https://content.production-public.tubi.io/cms/content?content_ids=100061708,100060814&platform=web&device_id=<uuid>` → map id→{title, description, year, thumbnails[]}. 400 without device_id+platform.
- Tubi login: `POST https://tubitv.com/oz/auth/login/` + CSRF; then `GET https://tubitv.com/oz/auth/loadAuth` with `connect.sid` cookie → JWT (`access_token`). Stored `TubiAccount` KEY_AT.
- Current HEAD: `4b1f8f6`. Working tree has uncommitted changes: widget redesign + TubiRepository content resolution.
- `ShowItem`: `(title, subtitle, progress, imageUrl="", watchUrl="")`.
- Widget ids: `grid_watchlist`, `grid_mystuff`, `tv_empty_p1`, `tv_empty_p2`, `view_flipper`, `iv_widget_bg`, `tv_tab_watchlist`, `tv_tab_mystuff`. Accent `#4DD0E1`, inactive tab `#8A93A3`.

## Relevant Files
- `app/src/main/java/com/tvtime/app/WidgetConfigFragment.kt` — catalog loader, Tubi login + "Sync my Tubi" button, parseCatalog; default accent `#4DD0E1`.
- `app/src/main/java/com/tvtime/app/TubiRepository.kt` — `fetchUserLists` (user-queue), `parseQueueIds`, `fetchViewHistoryIds`, `fetchContentDetails` (content cms), `tryGetAuth`/`tryGetPlain`.
- `app/src/main/java/com/tvtime/app/TubiAccount.kt` — login + `fetchLoadAuth` JWT, `authDebug`, `pickToken`.
- `app/src/main/java/com/tvtime/app/WatchlistWidgetService.kt` — RemoteViewsFactory builds poster-card rows, downloads+caches posters (100×150).
- `app/src/main/java/com/tvtime/app/TVTimeWidgetProvider.kt` — `ACTION_PAGE` tab flip, grid remote adapters, accent/inactive tab colors, `notifyDataChanged` uses grid ids.
- `app/src/main/java/com/tvtime/app/WatchDetailsActivity.kt` — opens watchUrl via browser.
- `app/src/main/java/com/tvtime/app/ShowListAdapter.kt` — `ShowItem` data class (imageUrl/watchUrl).
- `app/src/main/java/com/tvtime/app/WatchlistStore.kt` — persists `ShowItem`.
- `app/src/main/res/layout/widget_layout.xml` — tab header + ViewFlipper of two GridViews + empty texts.
- `app/src/main/res/layout/widget_list_item.xml` — poster card (poster + title/year + progress over scrim).
- `app/src/main/res/drawable/widget_poster_scrim.xml` — gradient scrim for poster cards.
- `app/src/main/AndroidManifest.xml` — `WatchlistWidgetService` (BIND_REMOTEVIEWS) + `WatchDetailsActivity` registered.
