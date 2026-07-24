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

## v1.2.0 — Phase 3: Encrypted Local Vault

- Tink AES256-GCM encryption with keys stored in Android Keystore
- VaultEntry Room table for encrypted item metadata
- Create encrypted text notes (AES-256-GCM, files stored in app-internal storage)
- View decrypted notes on demand
- Delete vault entries (removes both DB row and encrypted file)
- Vault tab in bottom navigation (Map / Vault / Settings)
- `VaultEncryptionManager` — sealed result type for all crypto operations (no silent failures)
- `VaultRepository` — sealed `VaultResult` for all I/O operations
- Auto-generated encryption keyset persisted via `AndroidKeysetManager` in SharedPreferences

## v1.3.0 — Phase 4: BLE Mesh Engine

- `MeshAdvertiser` — BLE peripheral mode: custom service UUID advertising with `BluetoothLeAdvertiser`
- `MeshScanner` — BLE central mode: scanning for nearby StaticQuo peers by device name prefix
- `MeshGattServer` — GATT server with message-write characteristic, incoming message parsing via JSON
- `MeshGattClient` — GATT client, connects to peers, discovers mesh service, writes outgoing messages
- `MeshRepository` — coordinates all BLE components; sealed `MeshInitResult` (Success / PermissionsDenied / BluetoothNotAvailable / Error)
- `MeshMessage` — data model with sender, content, timestamp, hop-count TTL (store-and-forward up to 5 hops)
- Device identity: stable node ID from `ANDROID_ID`, visible as `StaticQuo-{id}` in BLE scans
- Mesh tab in bottom navigation (Map / Mesh / Vault / Settings)

## v1.4.0 — Phase 5: Heatmap / Beacon Overlay

- 5 beacon types: MEDIC (red), NEED (amber), SUPPLY (green), SAFEZONE (blue), DANGER (red)
- Tap on offline map to place a beacon at that location
- All beacons persisted in Room (`heatmap_beacons` table)
- Layer toggle to show/hide beacons on map
- Legend drawer with filter by beacon type
- Beacon type selector dialog with color-coded type dropdown

## v1.5.0 — Phase 6: LoRa Radio Integration

- USB Host API integration for LoRa radio modules (SX127x-family via USB-to-UART)
- `LoRaSerialDevice` — raw USB bulk-transfer serial communication, no external dependencies
- Packet framing protocol with start/end bytes, length prefix, CRC check
- Configurable frequency (433/868/915 MHz), spreading factor (SF7–SF12), bandwidth (125/250/500 kHz)
- `LoRaRepository` — sealed `LoRaResult` for all hardware I/O (connect / send / receive / find device)
- LoRa tab in bottom navigation (Map / Mesh / Vault / LoRa / Settings)

## v1.6.0 — Phase 7: Offline Reference Search

- Room FTS4 full-text search index (auto-syncs with content table)
- 5 bundled reference documents: First Aid, Legal Rights, Communication Protocols, Emergency Contacts, De-escalation
- Automatic indexing from assets on first launch
- Prefix-matching search query with wildcard expansion
- Snippet generation with context around match
- Result detail dialog for full content
- Search tab in bottom navigation
