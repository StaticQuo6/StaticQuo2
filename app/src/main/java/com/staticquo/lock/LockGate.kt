package com.staticquo.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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

@Composable
fun LockGate(
    onUnlocked: @Composable () -> Unit,
    viewModel: PinViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    when (state.screen) {
        PinScreen.LOADING -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A3A5C)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "StaticQuo",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        PinScreen.SETUP -> {
            PinSetupScreen(
                viewModel = viewModel,
                onComplete = { /* handled by state flow */ }
            )
        }
        PinScreen.UNLOCK -> {
            PinUnlockScreen(
                viewModel = viewModel,
                onComplete = { /* handled by state flow */ }
            )
        }
        PinScreen.MAIN_APP -> {
            onUnlocked()
        }
    }
}
