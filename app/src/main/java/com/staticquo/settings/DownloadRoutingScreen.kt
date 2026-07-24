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
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Download URL") },
                placeholder = { Text("https://github.com/.../tiles.tar") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.downloadTiles(url) },
                enabled = url.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Download Tiles")
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
