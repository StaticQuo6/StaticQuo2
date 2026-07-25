package com.staticquo.routing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.staticquo.data.db.RoutingRegionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadRoutingScreen(
    onBack: () -> Unit,
    viewModel: DownloadRoutingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Routing Data", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.isInitialLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                if (state.downloadedRegions.isEmpty() && state.availableRegions.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No routing regions available",
                                fontSize = 16.sp,
                                color = Color(0xFF49454F)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Create a GitHub Release with tag 'graph-v1'\ncontaining a .zip file with pre-built graph data.",
                                fontSize = 12.sp,
                                color = Color(0xFF49454F).copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                if (state.downloadedRegions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Downloaded Regions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A3A5C),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(state.downloadedRegions) { region ->
                        DownloadedRoutingItem(
                            name = region.name,
                            size = region.sizeBytes,
                            onRemove = { viewModel.removeRegion(region) }
                        )
                        HorizontalDivider()
                    }
                    item {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(16.dp))
                    }
                }

                if (state.availableRegions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Available Regions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A3A5C),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(state.availableRegions) { region ->
                        AvailableRoutingItem(
                            name = region.name,
                            size = region.sizeBytes,
                            onDownload = { viewModel.downloadRegion(region) }
                        )
                        HorizontalDivider()
                    }
                }

                if (state.isDownloading) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = state.downloadProgressText,
                            fontSize = 14.sp,
                            color = Color(0xFF1A3A5C)
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.downloadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (state.error != null) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = state.error!!,
                            fontSize = 14.sp,
                            color = Color(0xFFB00020)
                        )
                    }
                }

                if (state.successMessage != null) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = state.successMessage!!,
                            fontSize = 14.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadedRoutingItem(
    name: String,
    size: Long,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(
                text = formatSize(size),
                fontSize = 13.sp,
                color = Color(0xFF49454F)
            )
        }
        Button(onClick = onRemove) {
            Text("Remove")
        }
    }
}

@Composable
private fun AvailableRoutingItem(
    name: String,
    size: Long,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(
                text = formatSize(size),
                fontSize = 13.sp,
                color = Color(0xFF49454F)
            )
        }
        Button(onClick = onDownload) {
            Text("Download")
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000 -> "${bytes / 1_000_000} MB"
        bytes >= 1_000 -> "${bytes / 1_000} KB"
        else -> "$bytes B"
    }
}
