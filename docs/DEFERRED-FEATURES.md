# Deferred Features

## Offline Routing

**Status:** Deferred

**Reason:** The `valhalla-mobile:0.5.1` dependency was compiled with Kotlin 2.3.0 and is incompatible with this project's Kotlin 2.0.21 toolchain. The dependency and all routing-specific code (`RoutingRepository`, `RoutingViewModel`, `RoutingRegionEntity`, `DownloadRoutingScreen`) have been removed to restore the build.

**Plan:** Reintroduce offline routing as an isolated feature task, evaluating either an older `valhalla-mobile` version compiled against Kotlin 2.0.x or a different routing library entirely.
