# StaticQuo — Agent Notes

## Build

```bash
# Debug (local development)
./gradlew assembleDebug

# Release (requires signing env vars)
STORE_FILE=signing-keystore.jks STORE_PASSWORD=... KEY_ALIAS=... KEY_PASSWORD=... ./gradlew assembleRelease

# Run all checks (tests + TODO/FIXME scan + lint)
./gradlew check lint
```

## CI Workflows

- `build-release.yml` — triggered on `v*` tag push. Builds signed release APK, attaches to GitHub Release.
- `build-check.yml` — triggered on every branch push and PR to `master`. Runs `check` (unit tests + TODO/FIXME/stub scan), `lint`, and `assembleDebug`.

## Code Conventions

- Kotlin with Jetpack Compose for UI
- Hilt for dependency injection
- Room for local database
- No comments unless the logic is genuinely obscure (this is a safety-critical app — code should be self-explanatory)
- All async operations use Kotlin coroutines
- ViewModel exposes state via `StateFlow`
- Repository pattern for data access
- Every feature has an `I/O reality check` note in the plan — the code must handle real I/O failure, not assume success

## Architecture

- `com.staticquo.lock` — app lock / PIN (Argon2id via argon2kt)
- `com.staticquo.vault` — encrypted local storage (Tink AES256-GCM, keys in Android Keystore, sealed result types for all crypto/I/O)
- `com.staticquo.mesh` — BLE/Wi-Fi mesh engine (permission-first init, checks BLE hardware + runtime perms before starting; components: MeshScanner, MeshAdvertiser, MeshGattServer, MeshGattClient; GATT-based message exchange over custom UUID; store-and-forward with hop-count TTL)
- `com.staticquo.maps` — offline map rendering & routing (MapLibre 11.1.0, .mbtiles via filesystem)
- `com.staticquo.heatmap` — needs/medic beacon overlay (5 beacon types: MEDIC, NEED, SUPPLY, SAFEZONE, DANGER; placed via tap on map; filtered by type; persisted in Room)
- `com.staticquo.lora` — LoRa radio integration (USB Host API, SX127x-family protocol framing, configurable frequency/SF/BW)
- `com.staticquo.search` — offline reference search (Room FTS4, 5 bundled reference docs, prefix matching with wildcards)
- `com.staticquo.data` — database, repositories
- `com.staticquo.di` — Hilt modules

## Permissions

- Location: runtime request in MapScreen for GPS
- BLE: runtime request in mesh init — `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE` (API 31+), `ACCESS_FINE_LOCATION` (all API levels). All declared in manifest with `maxSdkVersion=30` for legacy BLE perms. BLE hardware is `required=false`.
- `MeshRepository.initialize()` returns a sealed result: `Success`, `PermissionsDenied(missing)`, `BluetoothNotAvailable(detail)`, `Error(throwable)`.

## Testing

- Unit tests: `./gradlew test`
- TODO/FIXME/stub scan: `./gradlew checkNoTodos` (runs as part of `check`)
- No local emulator testing assumed — all device testing is manual by product owner

## Anchored Summary

### Phase 8 — WebDAV Backup (`ac3680a`)
- `WebDavClient.kt` — HTTP client for WebDAV PROPFIND/PUT/GET/DELETE with basic auth over HTTPS
- `BackupRepository.kt` — exports vault entries and heatmap beacons to JSON, uploads to WebDAV; restore downloads and re-inserts
- `BackupViewModel.kt` — `StateFlow<BackupUiState>`, triggers backup/restore on IO dispatcher, exposes progress/error/success
- `BackupScreen.kt` — Composable with URL, username, password fields; backup/restore buttons; progress indicator
- `MainActivity.kt` — added Settings item → BackupScreen navigation
- `SettingsFragment.kt` (or equivalent) — wired backup navigation

### Phase 9 — Duress PIN Wipe (`331d2b8`)
- `DuressWipeManager.kt` — clears `VaultEntryDao.clearAll()`, `HeatmapBeaconDao.clearAll()`, and PIN stored prefs; returns `Result`
- `DuressWipeManager` is a Hilt `@Singleton` injected into `PinViewModel`
- `PinViewModel.unlock()` detects duress PIN and triggers wipe instead of unlock
- Adds `clearAll()` to `VaultEntryDao` and `HeatmapBeaconDao`

### Phase 10 — Valhalla Offline Routing (`6a58470`)
- Dependency: `com.maplibre:valhalla-mobile:0.6.0`
- `RoutingRegionEntity.kt` — Room entity for downloaded routing tiles (region name, file path, version)
- `RoutingDao.kt` — CRUD for routing regions
- `RoutingRepository.kt` — download, check, list, delete routing tiles; calls valhalla-mobile for route calculation
- `RoutingViewModel.kt` — `StateFlow<RoutingUiState>` with origin/destination/route/calculating state
- `MapScreen.kt` — routing mode toggle (FAB "R"), set origin/destination by tap, route polyline overlay, route info card (distance + duration)
- `DownloadRoutingScreen.kt` — list available routing regions, download progress, delete button
- `AppDatabase.kt` — v5→6 migration adding `routing_regions` table
- `DatabaseModule.kt` — provider for `RoutingDao`

### Demo Routing Tiles Release (`4fabc14`)
- Downloaded Andorra demo routing tiles (2.8MB) from valhalla-mobile test assets
- Bundled in `app/src/main/assets/routing/andorra.mpack`
- Auto-install from assets on first launch
- Created GitHub Release `demo-routing-tiles-v1` with the .mpack as asset
- UI download button fetches from GitHub release URL

### Audit — Bugs Found & Fixed
1. **Missing `buildOfflineStyle`** — Function deleted in Phase 5 but call site at `MapScreen.kt:116` left orphaned. **RESTORED** with original JSON template (raster tile source pointing to local .mbtiles). Would cause compilation failure.
2. **Search FTS `source_file` column** — NO ISSUE. Column present in `SearchIndexFts` entity, `SearchDocument` content entity, DAO `SELECT`, and row mapper `SearchResultRow`.
3. **`SearchResult` name collision** — NO ISSUE. Three distinct names: `SearchResult<T>` (sealed), `SearchResultItem` (data), `SearchResultRow` (data).
4. **Silent exception swallow in `ensureIndexed()`** — `catch (_: Exception) {}` at `SearchRepository.kt:41` swallows I/O errors during initial doc indexing. Violates I/O reality check policy. NOT FIXED yet.
5. **Null error message in `search()`** — `"Search failed: ${e.message}"` could produce `"Search failed: null"`. NOT FIXED yet.

### CI Secrets Required
- `ANDROID_SDK_ROOT` — Android SDK path
- `KEY_STORE_BASE64` — Base64-encoded JKS keystore (for release builds)
- `KEY_STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` — keystore credentials
