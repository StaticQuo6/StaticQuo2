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
