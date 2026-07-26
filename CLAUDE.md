# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

ETF Watch — personal Android app for tracking US + Canada ETFs: a watchlist, Hamilton ETFs (with
scraped dividend yields), leveraged ETFs grouped by sector, and a per-ticker chart detail view.

**Stack**: Kotlin · Jetpack Compose · Hilt · Room · Retrofit/Moshi · WorkManager · MPAndroidChart
**Min SDK**: 26 · **Target/Compile SDK**: 35 · **Java target**: 17

## Setup

- Requires `local.properties` (gitignored) with `FMP_API_KEY=<key>` and `sdk.dir=<android sdk path>`.
  Copy from `local.properties.example`. Without a key, FMP-backed quote refresh silently fails
  (caught and logged, not fatal).
- Launcher icon is intentionally the system default; no app assets committed.

## Common commands

```
./gradlew assembleDebug          # build debug APK
./gradlew assembleRelease        # build release APK (minified, uses proguard-rules.pro)
./gradlew test                   # run JVM unit tests (module: app, currently no test sources)
./gradlew connectedAndroidTest   # run instrumented tests on a device/emulator
./gradlew lint                   # Android lint
```

There is no single-test filter set up beyond Gradle's standard `--tests "ClassName.methodName"`
flag, since there are no unit tests in the repo yet.

On Windows use `gradlew.bat` instead of `./gradlew`.

## Architecture

Single-Activity Compose app, MVVM, Hilt for DI. Package root: `com.iurie.etfwatch`.

- `di/` — `DbModule` (Room) and `NetworkModule` (Retrofit/OkHttp/Moshi) provide singletons app-wide.
  A single Retrofit client talks to FMP, with an `apikey` query-param interceptor appending
  `BuildConfig.FMP_API_KEY`.
- `data/db/` — Room. Three entities: `EtfEntity` (static metadata: ticker/name/exchange/sector/
  flags for leveraged/Hamilton/user-added/watchlist), `QuoteEntity` (price/change/yield/market cap
  plus `week1/2/3/5ReturnPct` and `monthReturnPct`/`twoMonthReturnPct`, refreshed independently),
  `PriceAlertEntity`. `EtfWithQuote` is the `@Embedded` join type used throughout the UI layer.
- `data/remote/FmpService.kt` — quotes (batch), historical-price-full (per-symbol), search. See
  `Dtos.kt` for the JSON shapes.
- `data/scrape/HamiltonScraper.kt` — Jsoup-scrapes hamiltonetfs.com/performance/ for tickers,
  yields, and sector tags (via keyword matching in `SECTOR_RULES`). Always wrapped in
  `runCatching`; scrape failures degrade to whatever is already in the DB / the seed data.
- `data/seed/SeedLoader.kt` — loads `assets/seed_hamilton.json` and `assets/seed_leveraged.json`
  on every app launch via `insertAllIgnore` (idempotent upsert-if-absent on primary key), so
  shipping new seed tickers never clobbers user edits (e.g. watchlist flags).
- `data/repo/` — three repositories sit between DB/network and ViewModels:
  - `EtfRepository` — orchestrates seeding, watchlist mutations, and `refreshAll()`/
    `refreshHamilton()` (calls FMP batch quotes, then FMP per-ticker historical returns, then the
    Hamilton scraper for yields).
  - `QuoteRepository` — owns FMP batch quote fetching (`refresh()`, chunked at `BATCH_SIZE=50`),
    FMP per-ticker return calculation for 1W/2W/3W/5W/1M/2M periods (`refreshReturnsFromFmp()`, one
    `historical-price-full` call per ticker — FMP has no batch historical endpoint — parallelized
    with `async`/`awaitAll`), and historical chart data mapping (`history()`).
  - `AlertRepository` — price alert CRUD/evaluation.
- `work/` — `WorkScheduler` enqueues two unique periodic `WorkManager` jobs (`quote_refresh`,
  `alert_check`) at an interval read from `UserPrefs` (DataStore), both requiring network
  connectivity. `QuoteRefreshWorker` / `AlertCheckWorker` are the Hilt-injected `CoroutineWorker`s.
  `EtfApp.onCreate()` seeds the DB, schedules periodic work, and fires one immediate refresh.
- `ui/nav/AppNav.kt` — single `NavHost` with 5 bottom-tab routes (home, watchlist, hamilton,
  leveraged, settings) plus a `detail/{ticker}` route reachable from any tab and via the
  `etfwatch://detail/{ticker}` deep link.
- `ui/<feature>/` — each screen is `XScreen.kt` (Compose) + `XViewModel.kt` (Hilt `@HiltViewModel`,
  exposes `StateFlow` to the screen). `ui/common/` holds shared composables: `EtfRow`, `SortFilterBar`,
  and `MpChartView` (a Compose wrapper around the MPAndroidChart `View`).

### Data flow for a typical refresh

`WorkManager` → `EtfRepository.refreshAll()` → `EtfDao.tickersToRefresh()` → `QuoteRepository.refresh()`
(FMP batch quotes: price/change/market cap) → `QuoteRepository.refreshReturnsFromFmp()` (per-ticker
FMP historical close lookup → 1M/2M return, `.copy()`'d onto the row `refresh()` just wrote so price
isn't clobbered) → `refreshHamiltonYields()` (Jsoup scrape → merge yield into `QuoteEntity`, upsert
new Hamilton tickers into `EtfEntity`) → Room `Flow`s auto-update subscribed ViewModels/Compose
screens.

### Conventions worth knowing

- TSX-listed tickers carry the `.TO` suffix for FMP lookups (e.g. `HDIV.TO`).
- FMP call budget: this project's key is on a 300 calls/minute plan (not the ~250/day free tier) —
  `refreshReturnsFromFmp()` deliberately costs one call per tracked ticker per refresh since there's
  no batch historical endpoint; that's fine at this plan's rate but would not be on the free tier.
- Nearly all repository-level network calls are wrapped in `runCatching { }.onFailure { Timber.w(...) }`
  with a safe fallback (empty list / existing DB row) — network/scrape failures are expected and
  must never crash the refresh pipeline.
- Room migrations use `fallbackToDestructiveMigration()` — there is no real migration path; bumping
  `AppDatabase` version just wipes local data on schema change.
