// TODO: Reintroduce offline routing when a valhalla-mobile version compatible
// with Kotlin 2.0.x is available, or evaluate a different routing library.
// This screen is currently unreachable from the UI.
package com.staticquo.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.staticquo.routing.RoutingViewModel

@Composable
fun DownloadRoutingScreen(
    onBack: () -> Unit,
    viewModel: RoutingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var url by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Routing Tiles", style = MaterialTheme.typography.headlineSmall)
        Text("Valhalla offline routing data", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        Spacer(Modifier.height(16.dp))

        if (state.hasTiles) {
            Text("Routing tiles installed", color = Color(0xFF43A047))
            Spacer(Modifier.height(8.dp))
        }

        if (state.isDownloading) {
            LinearProgressIndicator(
                progress = state.downloadProgress,
                modifier = Modifier.fillMaxWidth()
            )
            Text("${(state.downloadProgress * 100).toInt()}%", modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            val demoUrl = "https://github.com/StaticQuo6/StaticQuo2/releases/download/demo-routing-tiles-v1/valhalla_tiles.tar"

            if (state.hasTiles) {
                Text("Demo tiles (Andorra) installed from assets.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Download URL") },
                placeholder = { Text(demoUrl) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.downloadTiles(url.ifBlank { demoUrl }) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Download Tiles")
            }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = { url = demoUrl },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use Demo URL")
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.error!!, color = Color(0xFFB00020))
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
