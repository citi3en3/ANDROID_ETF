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
./gradlew testDebugUnitTest      # run JVM unit tests (module: app)
./gradlew connectedAndroidTest   # run instrumented tests on a device/emulator
./gradlew lint                   # Android lint
```

Filter a single test with Gradle's standard flag, e.g.
`./gradlew testDebugUnitTest --tests "*ReturnCalculatorTest.*"`.

On Windows use `gradlew.bat` instead of `./gradlew`.

If dependency resolution fails with `PKIX path building failed`, the JVM is not trusting the
machine's TLS-inspecting proxy. Append `-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT` to the Gradle
command so it uses the Windows certificate store.

## Architecture

Single-Activity Compose app, MVVM, Hilt for DI. Package root: `com.iurie.etfwatch`.

- `di/` — `DbModule` (Room) and `NetworkModule` (Retrofit/OkHttp/Moshi) provide singletons app-wide.
  A single Retrofit client talks to FMP, with an `apikey` query-param interceptor appending
  `BuildConfig.FMP_API_KEY`.
- `data/db/` — Room. Three entities: `EtfEntity` (static metadata: ticker/name/exchange/sector/
  flags for leveraged/Hamilton/user-added/watchlist), `QuoteEntity` (price/change/yield/market cap
  plus `week1/2/3/5ReturnPct` and `monthReturnPct`/`twoMonthReturnPct`, refreshed independently),
  `PriceAlertEntity`. `EtfWithQuote` is the `@Embedded` join type used throughout the UI layer.
- `data/remote/FmpService.kt` — quotes (batch), historical-price-full (per-symbol), search, and
  profile (used only to read `isEtf`, which gates watchlist adds since `/search` also returns
  stocks, mutual funds and trusts). See `Dtos.kt` for the JSON shapes. Moshi adapters are generated
  by `moshi-kotlin-codegen` via KSP — every JSON-backed model needs `@JsonClass(generateAdapter =
  true)` **and** a ProGuard keep, or release builds parse nothing.
- `data/remote/FmpSymbols.kt` — symbol spellings FMP accepts. TSX USD unit classes are `BASE-U.TO`
  (`HYLD-U.TO`); the dotted `BASE.U.TO` form returns `[]` rather than an error, so those rows
  silently never price. `EtfRepository.migrateLegacySymbols()` rewrites any legacy rows on launch.
- `data/scrape/HamiltonScraper.kt` — Jsoup-scrapes hamiltonetfs.com/performance/ for tickers,
  yields, and sector tags (via keyword matching in `SECTOR_RULES`). Always wrapped in
  `runCatching`; scrape failures degrade to whatever is already in the DB / the seed data.
- `data/filter/EtnFilter.kt` — ETNs are never tracked (the app follows ETFs only). `isEtn()` matches
  on name (a word-bounded `ETN`, "exchange traded note", or an ETN-only issuer brand such as
  MicroSectors/iPath/ETRACS/VelocityShares) plus a denylist of US-listed ETN symbols. It gates every
  entry point: seed load, FMP search results, `addToWatchlist`, and the Hamilton scrape;
  `EtfRepository.purgeEtns()` also drops any ETN already in the DB on each seed pass.
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
- `work/` — `WorkScheduler` enqueues one unique periodic `WorkManager` job (`quote_refresh`) at an
  interval read from `UserPrefs` (DataStore), requiring network connectivity. `QuoteRefreshWorker`
  refreshes quotes and *then* evaluates alerts, notifying via `AlertNotifier`; alerts deliberately
  do not run as their own job, because a separate job had no ordering guarantee and judged
  thresholds against stale prices. `EtfApp.onCreate()` seeds the DB, schedules periodic work, and
  fires an immediate refresh only if the data is older than the refresh interval.
- `ui/nav/AppNav.kt` — single `NavHost` with 5 bottom-tab routes (home, watchlist, hamilton,
  leveraged, settings) plus a `detail/{ticker}` route reachable from any tab and via the
  `etfwatch://detail/{ticker}` deep link.
- `ui/<feature>/` — each screen is `XScreen.kt` (Compose) + `XViewModel.kt` (Hilt `@HiltViewModel`,
  exposes `StateFlow` to the screen). `ui/common/` holds shared composables: `EtfRow`, `SortFilterBar`,
  and `MpChartView` (a Compose wrapper around the MPAndroidChart `View`).

### Data flow for a typical refresh

`WorkManager` → `EtfRepository.refreshAll()` (guarded by a `Mutex`, since passes read-modify-write
the same quote rows and would otherwise clobber each other) → `refreshHamiltonYields()` (Jsoup
scrape → merge yield into `QuoteEntity`, upsert new Hamilton tickers into `EtfEntity`) →
`EtfDao.tickersToRefresh()` → `QuoteRepository.refresh()` (FMP batch quotes: price/change/market
cap) → `QuoteRepository.refreshReturnsFromFmp()` (per-ticker FMP historical close lookup →
1W…2M returns, `.copy()`'d onto the row `refresh()` just wrote so price isn't clobbered) → Room
`Flow`s auto-update subscribed ViewModels/Compose screens.

The scrape runs **first** on purpose: it can introduce Hamilton tickers that aren't in the DB yet,
and snapshotting `tickersToRefresh()` before it left those rows priceless until the next cycle.

Pull-to-refresh in the ViewModels calls the repository directly. It must not *also* enqueue
`WorkScheduler.runOnceNow()` — doing both ran the whole pipeline twice per gesture.

### Conventions worth knowing

- TSX-listed tickers carry the `.TO` suffix for FMP lookups (e.g. `HDIV.TO`); USD unit classes use
  `-U.TO` (e.g. `HYLD-U.TO`). Always route symbols through `FmpSymbols` rather than concatenating.
  Of the bundled seeds, all 141 leveraged tickers resolve on FMP; 7 Hamilton ones don't
  (`CMVP.TO`, `CWIN.TO`, `HBND-U.TO`, `HFN.TO`, `MIX.TO`, `SMVP.TO`, `SWIN.TO`) — FMP simply has no
  listing for them, so they show a scraped yield but no price. That's expected, not a bug.
- FMP call budget: this project's key is on a 300 calls/minute plan (not the ~250/day free tier) —
  `refreshReturnsFromFmp()` deliberately costs one call per tracked ticker per refresh since there's
  no batch historical endpoint; that's fine at this plan's rate but would not be on the free tier.
- Nearly all repository-level network calls are wrapped in `runCatching { }.onFailure { Timber.w(...) }`
  with a safe fallback (empty list / existing DB row) — network/scrape failures are expected and
  must never crash the refresh pipeline.
- Room migrations: `AppDatabase.ALL_MIGRATIONS` holds the real migration path (currently
  `MIGRATION_4_5`, which adds the alert `armed` latch), with `fallbackToDestructiveMigration()` kept
  only as a backstop. Add a `Migration` when bumping the version — relying on the fallback wipes the
  user's watchlist.
- Price alerts latch: `AlertEvaluator` fires once on crossing, disarms, and re-arms only when the
  price returns to the safe side. Re-checking a still-true condition must stay silent, or an alert
  notifies on every refresh for as long as it holds.
- Numbers are formatted through `ui/common/Format.kt` (always `Locale.US`) and parsed with
  `Format.parseDecimal` (accepts `,` or `.`). Don't use bare `"%.2f".format(x)` — on a
  comma-decimal device it prints a value the alert dialog's parser then rejects.
- Up/down colours come from `ui/theme/TrendColors.kt`, which has distinct light/dark variants.
  MPAndroidChart colours must be applied in the `AndroidView` `update` block, not `factory`, or they
  survive a theme switch and leave the axes unreadable.
