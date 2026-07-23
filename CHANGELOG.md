# Changelog

## v1.0.0 — Phase 1: App Lock Foundation

- Project structure: Gradle 8.7, Kotlin 2.0, Compose, Hilt, Room
- PIN lock screen with setup and unlock flows
- Argon2id hashing for PIN storage
- Progressive lockout: 5 attempts → 30s, 10 → 5min, 15 → 30min
- CI/CD workflow: automated signed release APK on tag push
- Release artifacts: signed APK attached to GitHub Release

## v1.1.0 — Phase 2: Offline Maps Foundation

- MapLibre GL Native integration with offline .mbtiles rendering
- Bottom navigation with Map and Settings tabs
- **Pre-installed NYC demo region** (36 tiles, zoom 10–14, ~1.2 MB raster tiles from OSM)
- Auto-install demo tiles from APK assets on first launch
- GitHub Releases API integration for downloadable region tiles (`tiles-*` tags)
- Map screen renders raster tiles immediately with no network calls
- Region download manager with progress tracking
- Map region metadata stored in Room database
- Location permission request and GPS coordinate display
- Build tool: `tools/generate_demo_tiles.py` for producing .mbtiles files
