package com.example.bemo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.bemo.ui.BemoViewModel
import com.example.ui.theme.LocalBemoColors

@Composable
fun WelcomeScreen(
    viewModel: BemoViewModel,
    onEnterApp: () -> Unit
) {
    val extended = LocalBemoColors.current
    var showConsentSheet by remember { mutableStateOf(false) }
    var ageChecked by remember { mutableStateOf(true) }
    var termsChecked by remember { mutableStateOf(true) }

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
            Spacer(modifier = Modifier.height(30.dp))

            // Center branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_bemo_logo),
                    contentDescription = "Bemo Logo",
                    modifier = Modifier
                        .size(108.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Bemo",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Give every cedi a job.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Calm, offline-first personal budgeting and wishlist planning for Ghana.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = extended.textMuted,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { showConsentSheet = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("welcome_google_button")
                ) {
                    Text("Continue with Google", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }

                OutlinedButton(
                    onClick = { showConsentSheet = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("welcome_email_button")
                ) {
                    Text("Use Email", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                }

                TextButton(
                    onClick = {
                        viewModel.finishOnboarding()
                        onEnterApp()
                    },
                    modifier = Modifier.testTag("welcome_demo_button")
                ) {
                    Text(
                        text = "Try the demo",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "By continuing you accept the Terms and Privacy Policy.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = extended.textDisabled
                )
            }
        }
    }

    if (showConsentSheet) {
        AlertDialog(
            onDismissRequest = { showConsentSheet = false },
            title = { Text("Before We Start") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = ageChecked, onCheckedChange = { ageChecked = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("I am 16 or older", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = termsChecked, onCheckedChange = { termsChecked = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("I accept the Terms and Privacy Policy", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "If you are under 18, a parent or guardian has agreed to your use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConsentSheet = false
                        viewModel.finishOnboarding()
                        onEnterApp()
                    },
                    enabled = ageChecked && termsChecked,
                    modifier = Modifier.testTag("consent_continue_button")
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsentSheet = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
