# ETF Watch (Android)

Personal Android app: watchlist for US + Canada ETFs, Hamilton ETFs with dividend yields,
leveraged ETFs grouped by sector, and per-ticker chart detail view.

**Stack**: Kotlin · Jetpack Compose · Hilt · Room · Retrofit/Moshi · WorkManager · MPAndroidChart
**Min SDK**: 26 (Android 8) · **Target SDK**: 35
**Data provider**: Financial Modeling Prep (FMP). Hamilton list scraped with hardcoded seed fallback.

---

## Setup

1. **Clone** and open the folder in **Android Studio Koala (or newer)**.
2. **API key**: copy `local.properties.example` to `local.properties` and set your FMP key:
   ```
   FMP_API_KEY=your_key_here
   sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   ```
   `local.properties` is gitignored — never commit it.
3. **Launcher icon**: project uses the system default icon to keep the repo asset-free.
   Replace with your own via *File → New → Image Asset* in Android Studio, then set
   `android:icon="@mipmap/ic_launcher"` in `AndroidManifest.xml`.
4. **Gradle wrapper**: if `gradlew` is missing, run once from a shell with Gradle installed:
   ```
   gradle wrapper --gradle-version 8.9
   ```
   Or just let Android Studio sync — it will create the wrapper for you.
5. **Build**:
   ```
   ./gradlew assembleDebug
   ```
6. **Run**: pick a device/emulator (API 26+) and hit Run.

---

## Architecture

- MVVM + single-Activity Compose Navigation
- `data/` — Room DB, FMP Retrofit service, Hamilton scraper, repositories
- `ui/` — screens (watchlist, hamilton, leveraged, detail, settings) + shared composables
- `work/` — `QuoteRefreshWorker` (periodic) + `AlertCheckWorker` (notifications)
- `di/` — Hilt modules
- `assets/seed_*.json` — seed lists with sector tags, loaded on first run

## Notes

- TSX tickers use the `.TO` suffix on FMP (e.g. `HDIV.TO`).
- FMP free tier ≈ 250 calls/day — quotes are batched and refresh defaults to every 30 min.
- Hamilton scraper failures fall back silently to the bundled seed list.

## Scope (v1)

✅ 4 tabs, detail + chart, price alerts, offline cache, sort/filter
❌ Portfolio P&L, order placement, news, multi-currency, widget, Play Store
