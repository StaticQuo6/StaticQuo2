package com.staticquo.mesh

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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
fun MeshScreen(
    viewModel: MeshViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Mesh Network", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(state.statusMessage, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(12.dp))

        if (state.isStarted) {
            Button(
                onClick = { viewModel.stopMesh() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB00020)
                )
            ) {
                Text("Stop Mesh")
            }
            Spacer(Modifier.height(12.dp))
        }

        if (!state.isStarted) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.height(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("Mesh not started", color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.startMesh() }) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Mesh")
                }
            }
            return
        }

        if (state.peers.isEmpty()) {
            Text("Scanning for nearby devices...", color = Color.Gray)
        } else {
            Text("Peers (${state.peers.size})", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            state.peers.forEach { peer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF1A3A5C),
                        modifier = Modifier.height(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(peer.name, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text("${peer.rssi} dBm", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Messages (${state.messages.size})", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.messages.reversed(), key = { it.id }) { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (msg.senderId == meshRepository.nodeId)
                            Color(0xFFE3F2FD) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Row {
                            Text(msg.senderName, style = MaterialTheme.typography.labelSmall, color = Color(0xFF1A3A5C))
                            Spacer(Modifier.width(8.dp))
                            Text("hop ${msg.hopCount}/${msg.maxHops}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Text(msg.content, style = MaterialTheme.typography.bodyMedium)
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
                placeholder = { Text("Type a message") },
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
    }
}
