package com.example.bemo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.bemo.ui.BemoViewModel
import com.example.ui.theme.LocalBemoColors

@Composable
fun LockScreen(
    viewModel: BemoViewModel,
    onUnlocked: () -> Unit
) {
    val extended = LocalBemoColors.current
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Branding & PIN dots
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_bemo_logo),
                    contentDescription = "Bemo Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Bemo Locked",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isError) "Incorrect PIN. Try again." else "Enter your 4-digit security PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) extended.dangerText else extended.textMuted
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4 PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError -> extended.dangerText
                                        isFilled -> MaterialTheme.colorScheme.primary
                                        else -> extended.surfaceAlt
                                    }
                                )
                                .border(1.dp, if (isError) extended.dangerText else extended.border, CircleShape)
                        )
                    }
                }
            }

            // Numeric Dial Pad
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val padRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "⌫")
                )

                padRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { digit ->
                            if (digit.isEmpty()) {
                                Spacer(modifier = Modifier.size(64.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(extended.surfaceAlt)
                                        .clickable {
                                            isError = false
                                            if (digit == "⌫") {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                }
                                            } else if (enteredPin.length < 4) {
                                                enteredPin += digit
                                                if (enteredPin.length == 4) {
                                                    if (viewModel.unlockApp(enteredPin)) {
                                                        onUnlocked()
                                                    } else {
                                                        isError = true
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("pin_dial_$digit"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (digit == "⌫") {
                                        Icon(
                                            Icons.Default.Backspace,
                                            contentDescription = "Backspace",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        Text(
                                            text = digit,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = { showForgotDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally).testTag("forgot_pin_button")
                ) {
                    Text("Forgot PIN?", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text("Reset PIN Protection") },
            text = { Text("If you forgot your PIN, you can reset PIN protection and re-enter Bemo.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removePin()
                        showForgotDialog = false
                        onUnlocked()
                    }
                ) {
                    Text("Reset PIN & Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
