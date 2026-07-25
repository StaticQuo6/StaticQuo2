# Deferred Features

## Offline Routing

**Status:** Implemented with GraphHopper 11.0 (pure Java — no Kotlin metadata version conflict)

**Implementation:**
- `GraphHopperEngine.kt` — wraps `GraphHopper` for loading pre-built graph directories and calculating routes
- `RoutingRepository.kt` — downloads graph zip files from GitHub releases, extracts and manages routing regions
- `RoutingViewModel.kt` / `RoutingDao.kt` / `RoutingRegionEntity.kt` — standard Room-backed CRUD for routing data
- `DownloadRoutingScreen.kt` — download/remove routing regions
- `MapScreen.kt` — routing mode toggle (FAB "R"), tap-to-set origin/destination, route polyline overlay, info card with distance/time

**How to use:**
1. Create a GitHub Release with tag `graph-v1` containing a `.zip` file with pre-built GraphHopper graph directory
2. In Settings → Download Routing Data, download the region
3. On the Map screen, tap the "R" FAB to enter routing mode
4. Tap map to set origin, tap again to set destination, then tap "Calculate Route"
