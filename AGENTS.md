# StaticQuo — Agent Notes

## Build

```bash
# Debug (local development)
./gradlew assembleDebug

# Release (requires signing env vars)
STORE_FILE=signing-keystore.jks STORE_PASSWORD=... KEY_ALIAS=... KEY_PASSWORD=... ./gradlew assembleRelease
```

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

- `com.staticquo.lock` — app lock / PIN
- `com.staticquo.vault` — encrypted local storage
- `com.staticquo.mesh` — BLE/Wi-Fi mesh engine
- `com.staticquo.maps` — offline map rendering & routing
- `com.staticquo.heatmap` — needs/medic beacon overlay
- `com.staticquo.lora` — LoRa radio integration
- `com.staticquo.search` — offline reference search
- `com.staticquo.data` — database, repositories
- `com.staticquo.di` — Hilt modules

## Testing

- Unit tests: `./gradlew test`
- No local emulator testing assumed — all device testing is manual by product owner
