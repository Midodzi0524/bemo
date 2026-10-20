package com.example.bemo.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.bemo.model.Formatters
import com.example.bemo.model.ThemeMode
import com.example.bemo.ui.BemoViewModel
import com.example.bemo.ui.SubScreen
import com.example.bemo.ui.components.BemoBentoCard
import com.example.ui.theme.LocalBemoColors
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BemoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val extended = LocalBemoColors.current
    val scrollState = rememberScrollState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf<String?>(null) } // "terms" or "privacy"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Privacy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // About Bemo Branding Card with custom logo
            BemoBentoCard(
                backgroundColor = extended.surfaceAlt,
                borderColor = extended.border
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_bemo_logo),
                        contentDescription = "Bemo Logo",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Bemo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Give every cedi a job.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Version 1.0 · Ghana Edition",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textMuted
                        )
                    }
                }
            }

            // Firebase Account & Sync Card
            val currentUser = Firebase.auth.currentUser
            val coroutineScope = rememberCoroutineScope()
            val credentialManager = remember { CredentialManager.create(context) }

            BemoBentoCard(
                backgroundColor = extended.surfaceAlt,
                borderColor = extended.border
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("☁️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Firebase Cloud Sync Active",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = currentUser?.email ?: currentUser?.displayName ?: "Google Account Connected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            Firebase.auth.signOut()
                            coroutineScope.launch {
                                try {
                                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                } catch (e: Exception) {
                                    // Log or ignore
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Text("Sign Out", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Quick Shortcuts / Tools
            Text(
                text = "Financial Tools & Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            BemoBentoCard {
                // Recurring & Subscriptions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(SubScreen.RECURRING) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Recurring Bills & Subscriptions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Manage ECG, rent, airtime, and recurring rules", style = MaterialTheme.typography.bodySmall, color = extended.textMuted)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = extended.textMuted, modifier = Modifier.size(18.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = extended.border)

                // Financial Insights & Health Score
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(SubScreen.INSIGHTS) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Financial Health & Insights", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("View health score, category donuts, and trends", style = MaterialTheme.typography.bodySmall, color = extended.textMuted)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = extended.textMuted, modifier = Modifier.size(18.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = extended.border)

                // Accounts & Wallets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(SubScreen.ACCOUNTS) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏦", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Accounts & Wallets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Cash, Mobile Money, and bank accounts", style = MaterialTheme.typography.bodySmall, color = extended.textMuted)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = extended.textMuted, modifier = Modifier.size(18.dp))
                }
            }

            // Appearance & Privacy Preferences
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            BemoBentoCard {
                // Theme Mode Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.values().forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.updateTheme(mode) },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                modifier = Modifier.weight(1f).testTag("theme_chip_${mode.name.lowercase()}")
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = extended.border)

                // Hide Amounts Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide Amounts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Mask financial figures with •••• for privacy in public", style = MaterialTheme.typography.bodySmall, color = extended.textMuted)
                    }
                    Switch(
                        checked = settings.hideAmounts,
                        onCheckedChange = { viewModel.toggleHideAmounts() },
                        modifier = Modifier.testTag("settings_hide_amounts_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = extended.border)

                // App PIN Lock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App PIN Lock", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (settings.pinEnabled) "PIN code protection is enabled" else "Require PIN code to open Bemo",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textMuted
                        )
                    }
                    if (settings.pinEnabled) {
                        TextButton(
                            onClick = { viewModel.removePin() },
                            modifier = Modifier.testTag("settings_remove_pin_button")
                        ) {
                            Text("Remove", color = extended.dangerText)
                        }
                    } else {
                        Button(
                            onClick = {
                                pinInput = ""
                                showPinDialog = true
                            },
                            modifier = Modifier.testTag("settings_enable_pin_button")
                        ) {
                            Text("Set PIN")
                        }
                    }
                }
            }

            // Data & Export
            Text(
                text = "Data & Backup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            BemoBentoCard {
                // Export CSV / Text summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Export Data (CSV)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Share transaction history as spreadsheet-compatible CSV", style = MaterialTheme.typography.bodySmall, color = extended.textMuted)
                    }
                    Button(
                        onClick = {
                            val csvBuilder = StringBuilder()
                            csvBuilder.append("Date,Type,Amount (GHS),Category,Note\n")
                            transactions.filter { !it.isDeleted }.forEach { t ->
                                val amt = t.amountPesewas / 100.0
                                val catName = categories.find { it.id == t.categoryId }?.name ?: ""
                                csvBuilder.append("${Formatters.formatDate(t.dateMillis)},${t.type.name},$amt,\"$catName\",\"${t.note}\"\n")
                            }

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Bemo Financial Data Export")
                                putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export Bemo Data"))
                        },
                        modifier = Modifier.testTag("settings_export_button")
                    ) {
                        Text("Export")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = extended.border)

                // Refresh / Reset Demo Data
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Refresh Sample Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Restore the standard sample dataset for demonstrations", style = MaterialTheme.typography.bodySmall, color = extended.textMuted)
                    }
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("settings_reset_demo_button")
                    ) {
                        Text("Refresh")
                    }
                }
            }

            // Privacy & Legal
            Text(
                text = "Privacy & Legal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            BemoBentoCard {
                Text(
                    text = "🔒 100% Private & Device-Local",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bemo does not read your SMS, does not link to bank credentials, and contains zero advertising or tracking SDKs. Your budgets and financial records remain under your control.",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textMuted
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = { showLegalDialog = "terms" }) {
                        Text("Terms of Service")
                    }
                    TextButton(onClick = { showLegalDialog = "privacy" }) {
                        Text("Privacy Policy")
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // PIN Setup Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set 4-Digit Security PIN") },
            text = {
                Column {
                    Text("Enter a 4-digit code to protect Bemo upon opening:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pin_setup_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            viewModel.setPin(pinInput)
                            showPinDialog = false
                        }
                    },
                    enabled = pinInput.length == 4,
                    modifier = Modifier.testTag("confirm_set_pin_button")
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Demo Confirmation
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Refresh Sample Data?") },
            text = { Text("This will reset your accounts, budgets, and transactions back to the clean starter sample pack.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDemo()
                        showResetDialog = false
                    }
                ) {
                    Text("Confirm Refresh")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Legal Terms & Privacy Dialog
    showLegalDialog?.let { legalType ->
        AlertDialog(
            onDismissRequest = { showLegalDialog = null },
            title = { Text(if (legalType == "terms") "Terms of Service" else "Privacy Policy") },
            text = {
                Text(
                    text = if (legalType == "terms")
                        "Bemo is provided for personal budgeting and wishlist planning. You retain full ownership of all data entered. All calculations are provided for informational tracking without warranty."
                    else
                        "Bemo respects your privacy. All entries are stored locally on your device. We do not transmit or sell personal or financial information to third parties."
                )
            },
            confirmButton = {
                Button(onClick = { showLegalDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}
