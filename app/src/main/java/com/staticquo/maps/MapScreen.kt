package com.staticquo.maps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import java.io.File

@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val region = state.activeRegion
    val mbtilesFile = if (region != null) File(region.mbtilesPath) else null
    val hasTiles = mbtilesFile?.exists() == true

    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasTiles) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No map data",
                    color = Color(0xFF1A3A5C),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Open Settings to download a region.",
                    color = Color(0xFF1A3A5C).copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = { viewModel.refreshRegion() }) {
                    Text("Refresh")
                }
            }
        } else {
            var mapView by remember { mutableStateOf<MapView?>(null) }

            AndroidView(
                factory = { ctx ->
                    MapView(ctx).also { mv ->
                        mv.onCreate(null)
                        mapView = mv
                        mv.getMapAsync { mapLibreMap ->
                            val styleJson = buildOfflineStyle(mbtilesFile!!.absolutePath)
                            mapLibreMap.setStyle(Style.Builder().fromJson(styleJson))
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> mapView?.onStart()
                        Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                        Lifecycle.Event.ON_STOP -> mapView?.onStop()
                        Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    mapView?.onDestroy()
                }
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = Color(0xFFB00020),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
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
