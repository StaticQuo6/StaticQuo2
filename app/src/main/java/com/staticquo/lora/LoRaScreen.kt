package com.staticquo.lora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoRaScreen(
    viewModel: LoRaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("LoRa Radio", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(state.statusMessage, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(12.dp))

        if (!state.isConnected) {
            ConnectionPanel(
                isScanning = state.isScanning,
                deviceInfo = state.deviceInfo,
                onScan = { viewModel.scanForDevice() },
                onConnect = { viewModel.connect() }
            )
        } else {
            ConfigPanel(
                frequency = state.frequency,
                spreadingFactor = state.spreadingFactor,
                onFrequencyChange = { viewModel.setFrequency(it) },
                onSpreadingFactorChange = { viewModel.setSpreadingFactor(it) }
            )

            Spacer(Modifier.height(12.dp))

            Text("Messages (${state.packets.size})", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.packets.reversed(), key = { it.id }) { packet ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (packet.isOutgoing) Color(0xFFE3F2FD) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Row {
                                Text(
                                    if (packet.isOutgoing) "Sent" else "Received",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1A3A5C)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${packet.frequency} MHz",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                            Text(packet.payload, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            var messageText by remember { mutableStateOf("") }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("LoRa message") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText.trim())
                            messageText = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.disconnect() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Disconnect")
            }
        }
    }
}

@Composable
private fun ConnectionPanel(
    isScanning: Boolean,
    deviceInfo: String,
    onScan: () -> Unit,
    onConnect: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Cable,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.height(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("No LoRa module connected", color = Color.Gray)
        Text("Connect a USB LoRa radio module", color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        if (isScanning) {
            CircularProgressIndicator()
        } else {
            Button(onClick = onScan) {
                Icon(Icons.Default.SignalCellularAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan for Device")
            }
        }

        if (deviceInfo.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(deviceInfo, color = Color(0xFF1A3A5C))
            Spacer(Modifier.height(8.dp))
            Button(onClick = onConnect) {
                Text("Connect")
            }
        }
    }
}

@Composable
private fun ConfigPanel(
    frequency: Double,
    spreadingFactor: Int,
    onFrequencyChange: (Double) -> Unit,
    onSpreadingFactorChange: (Int) -> Unit
) {
    var freqExpanded by remember { mutableStateOf(false) }
    var sfExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.height(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Radio Config", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Frequency", style = MaterialTheme.typography.labelSmall)
                    OutlinedButton(onClick = { freqExpanded = true }) {
                        Text("${frequency.toInt()} MHz")
                    }
                    DropdownMenu(expanded = freqExpanded, onDismissRequest = { freqExpanded = false }) {
                        LoRaConstants.SUPPORTED_FREQUENCIES_MHZ.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text("${freq.toInt()} MHz") },
                                onClick = {
                                    onFrequencyChange(freq)
                                    freqExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Spreading Factor", style = MaterialTheme.typography.labelSmall)
                    OutlinedButton(onClick = { sfExpanded = true }) {
                        Text("SF$spreadingFactor")
                    }
                    DropdownMenu(expanded = sfExpanded, onDismissRequest = { sfExpanded = false }) {
                        LoRaConstants.SUPPORTED_SPREADING_FACTORS.forEach { sf ->
                            DropdownMenuItem(
                                text = { Text("SF$sf") },
                                onClick = {
                                    onSpreadingFactorChange(sf)
                                    sfExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
