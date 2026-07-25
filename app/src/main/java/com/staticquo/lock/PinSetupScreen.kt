package com.staticquo.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PinSetupScreen(
    viewModel: PinViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.screen) {
        if (state.screen == PinScreen.MAIN_APP) onComplete()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1A3A5C)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Set App Lock PIN",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (state.error == "Confirm your PIN") "Confirm your PIN"
                       else "Choose a 4-8 digit PIN",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            PinDotRow(pinLength = state.pinEntry.length, maxLength = 8)

            Spacer(modifier = Modifier.height(8.dp))

            if (state.error != null && state.error != "Confirm your PIN") {
                Text(
                    text = state.error!!,
                    color = Color(0xFFFF5252),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            PinKeypad(
                onDigit = { digit -> viewModel.onPinDigit(digit) },
                onDelete = { viewModel.onDeleteDigit() },
                onConfirm = {
                    if (state.pinEntry.isNotEmpty()) {
                        viewModel.onSetupPin(state.pinEntry)
                    }
                },
                confirmLabel = if (state.error == "Confirm your PIN") "Confirm" else "Next"
            )
        }
    }
}

@Composable
fun PinUnlockScreen(
    viewModel: PinViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.screen) {
        if (state.screen == PinScreen.MAIN_APP) onComplete()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1A3A5C)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "StaticQuo",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter PIN to unlock",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            PinDotRow(pinLength = state.pinEntry.length, maxLength = 8)

            Spacer(modifier = Modifier.height(8.dp))

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = Color(0xFFFF5252),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (state.lockoutRemainingMs > 0) {
                Text(
                    text = "Locked: ${state.lockoutRemainingMs / 1000}s remaining",
                    color = Color(0xFFFFD740),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            PinKeypad(
                onDigit = { digit -> viewModel.onPinDigit(digit) },
                onDelete = { viewModel.onDeleteDigit() },
                onConfirm = {
                    if (state.pinEntry.isNotEmpty()) {
                        viewModel.onUnlockPin(state.pinEntry)
                    }
                },
                confirmLabel = "Unlock"
            )
        }
    }
}

@Composable
fun PinDotRow(pinLength: Int, maxLength: Int) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val visibleCount = maxOf(pinLength, 4)
            for (index in 0 until visibleCount) {
                Box(
                modifier = Modifier
                    .size(if (index < pinLength) 16.dp else 12.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < pinLength) Color.White
                        else Color.White.copy(alpha = 0.3f)
                    )
            )
            }
        }
    }
}

@Composable
fun PinKeypad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        for (row in listOf("123", "456", "789")) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { digit ->
                    KeypadButton(
                        label = digit.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = { onDigit(digit) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.5f),
                shape = MaterialTheme.shapes.medium,
                color = Color.White.copy(alpha = 0.15f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = confirmLabel,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            KeypadButton(
                label = "0",
                modifier = Modifier.weight(1f),
                onClick = { onDigit('0') }
            )

            KeypadButton(
                label = "⌫",
                modifier = Modifier.weight(1f),
                onClick = onDelete
            )
        }
    }
}

@Composable
fun KeypadButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.aspectRatio(1.5f),
        shape = MaterialTheme.shapes.medium,
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = if (label.length > 1) 20.sp else 28.sp
            )
        }
    }
}
