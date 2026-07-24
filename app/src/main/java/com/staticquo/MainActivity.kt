package com.staticquo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.staticquo.lock.LockGate
import com.staticquo.maps.MapScreen
import com.staticquo.settings.DownloadMapsScreen
import com.staticquo.settings.SettingsScreen
import com.staticquo.vault.VaultScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LockGate(onUnlocked = { MainNavigation() })
            }
        }
    }
}

data class NavTab(
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    NavTab("Map", Icons.Default.Map),
    NavTab("Vault", Icons.Default.Lock),
    NavTab("Settings", Icons.Default.Settings)
)

@Composable
private fun MainNavigation() {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showDownloadMaps by rememberSaveable { mutableStateOf(false) }

    if (showDownloadMaps) {
        DownloadMapsScreen(onBack = { showDownloadMaps = false })
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1A3A5C)
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                tint = if (selectedTab == index) Color.White
                                       else Color.White.copy(alpha = 0.6f)
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                color = if (selectedTab == index) Color.White
                                        else Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = Color(0xFFF5F5F5)
        ) {
            when (selectedTab) {
                0 -> MapScreen()
                1 -> VaultScreen()
                2 -> SettingsScreen(
                    onNavigateToDownloadMaps = { showDownloadMaps = true }
                )
            }
        }
    }
}
