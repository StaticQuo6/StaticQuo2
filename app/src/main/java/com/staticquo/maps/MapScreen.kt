package com.staticquo.maps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.FileWriter
import com.staticquo.heatmap.BeaconType
import com.staticquo.heatmap.HeatmapViewModel
import com.staticquo.routing.RoutingViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import java.io.File

@Composable
fun MapScreen(
    mapViewModel: MapViewModel = hiltViewModel(),
    heatmapViewModel: HeatmapViewModel = hiltViewModel(),
    routingViewModel: RoutingViewModel = hiltViewModel(),
) {
    val mapState by mapViewModel.uiState.collectAsState()
    val heatmapState by heatmapViewModel.uiState.collectAsStateWithLifecycle()
    val routingState by routingViewModel.uiState.collectAsState()

    var pendingBeaconLat by remember { mutableStateOf(0.0) }
    var pendingBeaconLng by remember { mutableStateOf(0.0) }

    if (mapState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val region = mapState.activeRegion
    val mbtilesFile = if (region != null) File(region.mbtilesPath) else null
    val hasTiles = mbtilesFile?.exists() == true
    val mbtilesValid = hasTiles && isValidMbtiles(mbtilesFile!!)

    val ctx = LocalContext.current
    if (mbtilesFile != null && hasTiles) {
        Log.d("StaticQuoMap", "mbtiles path: ${mbtilesFile.absolutePath}")
        Log.d("StaticQuoMap", "mbtiles exists: $hasTiles, valid: $mbtilesValid")
        diagnosticLog(ctx, "mbtiles path: ${mbtilesFile.absolutePath}")
        diagnosticLog(ctx, "mbtiles exists: $hasTiles, valid: $mbtilesValid")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasTiles || !mbtilesValid) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (mbtilesFile == null || !hasTiles) "No map data"
                           else "Map data unavailable",
                    color = Color(0xFF1A3A5C),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = if (mbtilesFile == null || !hasTiles) "Open Settings to download a region."
                           else "Please re-download the region in Settings.",
                    color = Color(0xFF1A3A5C).copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = { mapViewModel.refreshRegion() }) { Text("Refresh") }
            }
        } else {
            var mapView by remember { mutableStateOf<MapView?>(null) }
            var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

            AndroidView(
                    factory = { ctx ->
                        try {
                            MapLibre.getInstance(ctx)
                        } catch (_: Exception) {
                            MapLibre.getInstance(ctx, "", WellKnownTileServer.MapLibre)
                        }
                        MapView(ctx).also { mv ->
                            mv.onCreate(null)
                            mapView = mv
                            mv.getMapAsync { map ->
                                val mbtilesPath = mbtilesFile.absolutePath
                                val styleJson = buildOfflineStyle(mbtilesPath)
                                diagnosticLog(ctx, "--- style JSON ---")
                                diagnosticLog(ctx, styleJson)
                                diagnosticLog(ctx, "--- end style JSON ---")
                                map.addOnDidFailLoadingMapListener { errorMessage ->
                                    diagnosticLog(ctx, "MAP LOAD ERROR: $errorMessage")
                                }
                                map.setStyle(Style.Builder().fromJson(styleJson)) {
                                    val camera = readMbtilesCenter(mbtilesPath)
                                    if (camera != null) {
                                        diagnosticLog(ctx, "setting camera: lat=${camera.first} lng=${camera.second} zoom=${camera.third}")
                                        map.moveCamera(
                                            CameraUpdateFactory.newCameraPosition(
                                                CameraPosition.Builder()
                                                    .target(LatLng(camera.first, camera.second))
                                                    .zoom(camera.third)
                                                    .build()
                                            )
                                        )
                                    }
                                    val actualZoom = map.cameraPosition.zoom
                                    diagnosticLog(ctx, "map rendered, camera zoom: $actualZoom")
                                }
                                mapLibreMap = map

                                map.addOnMapClickListener { point ->
                                if (routingState.enabled) {
                                    if (routingState.origin == null) {
                                        routingViewModel.setOrigin(point.latitude, point.longitude)
                                    } else if (routingState.destination == null) {
                                        routingViewModel.setDestination(point.latitude, point.longitude)
                                    } else {
                                        routingViewModel.setOrigin(point.latitude, point.longitude)
                                        routingViewModel.setDestination(null)
                                    }
                                } else if (!heatmapState.showAddDialog) {
                                    pendingBeaconLat = point.latitude
                                    pendingBeaconLng = point.longitude
                                    heatmapViewModel.showAddDialog()
                                }
                                true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            @Suppress("DEPRECATION")
            LaunchedEffect(
                heatmapState.beacons, heatmapState.showHeatmap, heatmapState.activeFilter,
                routingState.routePoints, routingState.origin, routingState.destination
            ) {
                val map = mapLibreMap ?: return@LaunchedEffect
                val origin = routingState.origin
                val dest = routingState.destination
                val routePoints = routingState.routePoints

                map.clear()

                if (origin != null) {
                    map.addMarker(
                        org.maplibre.android.annotations.MarkerOptions()
                            .position(LatLng(origin.first, origin.second))
                            .title("Origin")
                            .snippet("Start point")
                    )
                }
                if (dest != null) {
                    map.addMarker(
                        org.maplibre.android.annotations.MarkerOptions()
                            .position(LatLng(dest.first, dest.second))
                            .title("Destination")
                            .snippet("End point")
                    )
                }
                if (routePoints.isNotEmpty()) {
                    val latLngs = routePoints.map { LatLng(it.first, it.second) }
                    map.addPolyline(
                        org.maplibre.android.annotations.PolylineOptions()
                            .addAll(latLngs)
                            .color(android.graphics.Color.parseColor("#1A3A5C"))
                            .width(5f)
                    )
                }

                if (!heatmapState.showHeatmap) return@LaunchedEffect

                val activeFilter = heatmapState.activeFilter
                val filtered = if (activeFilter != null) {
                    heatmapState.beacons.filter { it.beaconType == activeFilter.name }
                } else {
                    heatmapState.beacons
                }

                for (beacon in filtered) {
                    try {
                        map.addMarker(
                            org.maplibre.android.annotations.MarkerOptions()
                                .position(LatLng(beacon.latitude, beacon.longitude))
                                .title(beacon.title)
                                .snippet(beacon.description)
                        )
                    } catch (_: Exception) {}
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> mapView?.onStart()
                        Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                        Lifecycle.Event.ON_STOP -> mapView?.onStop()
                        Lifecycle.Event.ON_DESTROY -> {
                            mapView?.onDestroy()
                            mapLibreMap = null
                            mapView = null
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            if (mapState.error != null) {
                Text(
                    mapState.error!!,
                    color = Color(0xFFB00020),
                    modifier = Modifier.padding(8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp, 8.dp, 8.dp, 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = { heatmapViewModel.toggleHeatmap() },
                    modifier = Modifier.size(40.dp),
                    containerColor = if (heatmapState.showHeatmap) Color(0xFF1A3A5C) else Color.Gray
                ) {
                    Icon(Icons.Default.Layers, "Toggle beacons", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                FloatingActionButton(
                    onClick = { heatmapViewModel.toggleLegend() },
                    modifier = Modifier.size(40.dp),
                    containerColor = Color(0xFF1A3A5C)
                ) {
                    Icon(Icons.Default.MyLocation, "Legend", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                FloatingActionButton(
                    onClick = { routingViewModel.toggleRouting() },
                    modifier = Modifier.size(40.dp),
                    containerColor = if (routingState.enabled) Color(0xFF1A3A5C) else Color.Gray
                ) {
                    Text(
                        "R",
                        color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            if (heatmapState.showLegend) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(0.9f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Beacon Legend", style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { heatmapViewModel.toggleLegend() }) {
                                Icon(Icons.Default.Close, "Close", modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(BeaconType.entries) { type ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            heatmapViewModel.setFilter(
                                                if (heatmapState.activeFilter == type) null else type
                                            )
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(type.color))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(type.label, modifier = Modifier.weight(1f))
                                    if (heatmapState.activeFilter == type) {
                                        Text("active", color = Color(0xFF1A3A5C), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        if (heatmapState.activeFilter != null) {
                            TextButton(onClick = { heatmapViewModel.setFilter(null) }) {
                                Text("Clear filter")
                            }
                        }
                    }
                }
            }

            if (routingState.enabled) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .fillMaxWidth(0.6f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Routing", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1A3A5C))
                            IconButton(onClick = { routingViewModel.disableRouting() }) {
                                Icon(Icons.Default.Close, "Close routing", modifier = Modifier.size(16.dp))
                            }
                        }
                        when {
                            routingState.origin == null -> Text("Tap map to set origin", fontSize = 12.sp, color = Color(0xFF49454F))
                            routingState.destination == null -> Text("Origin set. Tap to set destination.", fontSize = 12.sp, color = Color(0xFF49454F))
                            routingState.routePoints.isNotEmpty() -> Text(
                                "${formatDistance(routingState.distanceMeters)} | ${formatTime(routingState.timeMillis)}",
                                fontSize = 12.sp, color = Color(0xFF1A3A5C)
                            )
                            else -> Text("Both points set. Calculate route.", fontSize = 12.sp, color = Color(0xFF49454F))
                        }
                        if (routingState.origin != null && routingState.destination != null && routingState.routePoints.isEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { routingViewModel.calculateRoute() },
                                enabled = !routingState.isCalculating,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (routingState.isCalculating) "Calculating..." else "Calculate Route", fontSize = 12.sp)
                            }
                        }
                        if (routingState.routePoints.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Button(onClick = { routingViewModel.calculateRoute() }, modifier = Modifier.weight(1f)) {
                                    Text("Recalculate", fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(4.dp))
                                Button(onClick = { routingViewModel.clearRoute() }, modifier = Modifier.weight(1f)) {
                                    Text("Clear", fontSize = 12.sp)
                                }
                            }
                        }
                        if (routingState.error != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(routingState.error!!, fontSize = 11.sp, color = Color(0xFFB00020))
                        }
                    }
                }
            }
        }
    }

    if (heatmapState.showAddDialog) {
        AddBeaconDialog(
            onDismiss = { heatmapViewModel.hideAddDialog() },
            onConfirm = { type, title, desc ->
                heatmapViewModel.addBeacon(pendingBeaconLat, pendingBeaconLng, type, title, desc)
            }
        )
    }
}

@Composable
private fun AddBeaconDialog(
    onDismiss: () -> Unit,
    onConfirm: (BeaconType, String, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(BeaconType.NEED) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Place Beacon") },
        text = {
            Column {
                Text("Type", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Box {
                    Button(onClick = { typeExpanded = true }) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(selectedType.color))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(selectedType.label)
                    }
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        BeaconType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(type.color))
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedType, title.trim(), description.trim()) },
                enabled = title.isNotBlank()
            ) { Text("Place") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun readMbtilesCenter(mbtilesPath: String): Triple<Double, Double, Double>? {
    var db: SQLiteDatabase? = null
    var cursor: android.database.Cursor? = null
    try {
        db = SQLiteDatabase.openDatabase(mbtilesPath, null, SQLiteDatabase.OPEN_READONLY)
        cursor = db.rawQuery("SELECT value FROM metadata WHERE name = 'center'", null)
        if (cursor.moveToFirst()) {
            val parts = cursor.getString(0).split(",")
            if (parts.size >= 3) {
                return Triple(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble())
            }
        }
        return null
    } catch (_: Exception) {
        return null
    } finally {
        cursor?.close()
        db?.close()
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) "${"%.1f".format(meters / 1000)} km" else "${meters.toInt()} m"
}

private fun formatTime(millis: Long): String {
    val totalSec = millis / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    return if (h > 0) "${h}h ${m}min" else "${m}min"
}

private fun diagnosticLog(context: Context, message: String) {
    try {
        val dir = context.getExternalFilesDir(null) ?: return
        val file = File(dir, "map_debug.log")
        FileWriter(file, true).use { it.appendLine("[${java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())}] $message") }
    } catch (_: Exception) {}
}

private fun buildOfflineStyle(mbtilesPath: String): String {
    return """{
        "version": 8,
        "name": "StaticQuo Offline",
        "sources": {
            "offline": {
                "type": "raster",
                "url": "mbtiles://$mbtilesPath",
                "tileSize": 256,
                "minzoom": 0,
                "maxzoom": 22
            }
        },
        "layers": [
            {
                "id": "offline-layer",
                "type": "raster",
                "source": "offline",
                "minzoom": 0,
                "maxzoom": 22
            }
        ]
    }"""
}

private fun isValidMbtiles(file: File): Boolean {
    var db: SQLiteDatabase? = null
    var cursor: android.database.Cursor? = null
    try {
        db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='tiles'", null)
        if (!cursor.moveToFirst()) {
            Log.w("StaticQuoMap", "mbtiles missing 'tiles' table: ${file.absolutePath}")
            return false
        }
        cursor.close()
        cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='metadata'", null)
        if (!cursor.moveToFirst()) {
            Log.w("StaticQuoMap", "mbtiles missing 'metadata' table: ${file.absolutePath}")
            return false
        }
        cursor.close()
        cursor = db.rawQuery("SELECT count(*) FROM tiles", null)
        cursor.moveToFirst()
        val tileCount = cursor.getInt(0)
        if (tileCount == 0) {
            Log.w("StaticQuoMap", "mbtiles has zero tiles: ${file.absolutePath}")
            return false
        }
        return true
    } catch (e: Exception) {
        Log.e("StaticQuoMap", "mbtiles validation failed: ${file.absolutePath}", e)
        return false
    } finally {
        cursor?.close()
        db?.close()
    }
}
