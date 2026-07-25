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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.staticquo.heatmap.BeaconType
import com.staticquo.heatmap.HeatmapViewModel
// TODO: Re-enable offline routing when valhalla-mobile is replaced
// import com.staticquo.routing.RoutePoint
// import com.staticquo.routing.RoutingViewModel
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import java.io.File

@Composable
fun MapScreen(
    mapViewModel: MapViewModel = hiltViewModel(),
    heatmapViewModel: HeatmapViewModel = hiltViewModel(),
    // routingViewModel: RoutingViewModel = hiltViewModel(),  // TODO: re-enable with routing
) {
    val mapState by mapViewModel.uiState.collectAsState()
    val heatmapState by heatmapViewModel.uiState.collectAsStateWithLifecycle()
    // val routingState by routingViewModel.uiState.collectAsStateWithLifecycle()  // TODO: routing

    var pendingBeaconLat by remember { mutableStateOf(0.0) }
    var pendingBeaconLng by remember { mutableStateOf(0.0) }
    // var routingMode by remember { mutableStateOf(false) }  // TODO: re-enable with routing
    // var polylineRef by remember { mutableStateOf<org.maplibre.android.annotations.Polyline?>(null) }  // TODO: routing

    if (mapState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val region = mapState.activeRegion
    val mbtilesFile = if (region != null) File(region.mbtilesPath) else null
    val hasTiles = mbtilesFile?.exists() == true

    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasTiles) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No map data", color = Color(0xFF1A3A5C), modifier = Modifier.padding(bottom = 8.dp))
                Text("Open Settings to download a region.", color = Color(0xFF1A3A5C).copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 16.dp))
                Button(onClick = { mapViewModel.refreshRegion() }) { Text("Refresh") }
            }
        } else {
            var mapView by remember { mutableStateOf<MapView?>(null) }
            var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

            AndroidView(
                factory = { ctx ->
                    MapView(ctx).also { mv ->
                        mv.onCreate(null)
                        mapView = mv
                        mv.getMapAsync { map ->
                            val styleJson = buildOfflineStyle(mbtilesFile!!.absolutePath)
                            map.setStyle(Style.Builder().fromJson(styleJson))
                            mapLibreMap = map

                            map.addOnMapClickListener { point ->
                                // TODO: re-enable routingMode branch when routing is reintroduced
                                if (!heatmapState.showAddDialog) {
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

            // TODO: re-enable route polyline overlay when routing is reintroduced
            // LaunchedEffect(routingState.route, ...) { ... }

            LaunchedEffect(heatmapState.beacons, heatmapState.showHeatmap, heatmapState.activeFilter) {
                val map = mapLibreMap ?: return@LaunchedEffect
                map.removeAnnotations()

                if (!heatmapState.showHeatmap) return@LaunchedEffect

                val filtered = if (heatmapState.activeFilter != null) {
                    heatmapState.beacons.filter { it.beaconType == heatmapState.activeFilter.name }
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

            // TODO: re-enable routing mode hint when routing is reintroduced
            // if (routingMode) { ... }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
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

                // TODO: re-enable routing R FAB when routing is reintroduced
                // FloatingActionButton(...) { Text("R") }
            }

            // TODO: re-enable route info card when routing is reintroduced
            // val route = routingState.route
            // if (route != null) { Card(...) }

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

private fun buildOfflineStyle(mbtilesPath: String): String {
    return """{
        "version": 8,
        "name": "StaticQuo Offline",
        "sources": {
            "offline": {
                "type": "raster",
                "url": "mbtiles://$mbtilesPath",
                "tileSize": 256
            }
        },
        "layers": [
            {
                "id": "offline-layer",
                "type": "raster",
                "source": "offline"
            }
        ]
    }"""
}
