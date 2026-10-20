package com.example.bemo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bemo.model.*
import com.example.bemo.ui.BemoViewModel
import com.example.bemo.ui.components.BemoBentoCard
import com.example.ui.theme.LocalBemoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: BemoViewModel,
    onBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val dashboardState by viewModel.dashboardState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val extended = LocalBemoColors.current

    var showTransferDialog by remember { mutableStateOf(false) }
    var transferFromId by remember { mutableStateOf<String?>(null) }
    var transferToId by remember { mutableStateOf<String?>(null) }
    var transferAmountInput by remember { mutableStateOf("") }

    var reconcilingAccount by remember { mutableStateOf<Account?>(null) }
    var reconcileAmountInput by remember { mutableStateOf("") }

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var newAccName by remember { mutableStateOf("") }
    var newAccEmoji by remember { mutableStateOf("💳") }
    var newAccKind by remember { mutableStateOf(AccountKind.CASH) }
    var newAccOpeningBalance by remember { mutableStateOf("0") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts & Balances", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("accounts_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddAccountDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Account")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Net & Available Summary
                BemoBentoCard(
                    backgroundColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Net Balance",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = Formatters.formatPesewas(dashboardState.netBalancePesewas, settings.hideAmounts),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Available: ${Formatters.formatPesewas(dashboardState.availableBalancePesewas, settings.hideAmounts)} (after earmarked goals)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            transferFromId = accounts.firstOrNull()?.id
                            transferToId = accounts.getOrNull(1)?.id
                            showTransferDialog = true
                        },
                        modifier = Modifier.weight(1f).testTag("accounts_transfer_button")
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Transfer")
                    }

                    OutlinedButton(
                        onClick = {
                            val target = accounts.firstOrNull()
                            if (target != null) {
                                reconcilingAccount = target
                                reconcileAmountInput = ""
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("accounts_reconcile_button")
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reconcile")
                    }
                }
            }

            item {
                Text(
                    text = "Your Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(accounts, key = { it.id }) { acc ->
                // Calculate balance
                var balance = acc.openingBalancePesewas
                transactions.filter { !it.isDeleted && it.status == TransactionStatus.POSTED }.forEach { t ->
                    when {
                        t.type == TransactionType.INCOME && t.accountId == acc.id -> balance += t.amountPesewas
                        t.type == TransactionType.EXPENSE && t.accountId == acc.id -> balance -= t.amountPesewas
                        t.type == TransactionType.SAVING && t.accountId == acc.id -> balance -= t.amountPesewas
                        t.type == TransactionType.TRANSFER && t.toAccountId == acc.id -> balance += t.amountPesewas
                        t.type == TransactionType.TRANSFER && t.accountId == acc.id -> balance -= t.amountPesewas
                    }
                }

                BemoBentoCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    onClick = {
                        reconcilingAccount = acc
                        reconcileAmountInput = (balance / 100.0).toString()
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(extended.surfaceAlt),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(acc.emoji, fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = acc.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = acc.kind.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.textMuted
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Formatters.formatPesewas(balance, settings.hideAmounts),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap to reconcile",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Transfer dialog
    if (showTransferDialog) {
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text("Transfer Money") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("From:")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = transferFromId == acc.id,
                                onClick = { transferFromId = acc.id },
                                label = { Text("${acc.emoji} ${acc.name}") },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                    Text("To:")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = transferToId == acc.id,
                                onClick = { transferToId = acc.id },
                                label = { Text("${acc.emoji} ${acc.name}") },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = transferAmountInput,
                        onValueChange = { transferAmountInput = it },
                        label = { Text("Transfer amount") },
                        prefix = { Text("₵ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pesewas = Formatters.evaluateExpressionToPesewas(transferAmountInput) ?: 0L
                        if (transferFromId != null && transferToId != null && pesewas > 0) {
                            viewModel.repository.transferBetweenAccounts(transferFromId!!, transferToId!!, pesewas)
                        }
                        showTransferDialog = false
                    },
                    modifier = Modifier.testTag("confirm_transfer_button")
                ) {
                    Text("Complete Transfer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reconcile dialog
    reconcilingAccount?.let { acc ->
        AlertDialog(
            onDismissRequest = { reconcilingAccount = null },
            title = { Text("Reconcile ${acc.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the real current balance of your ${acc.name} account. Bemo will automatically calculate the adjustment.")
                    OutlinedTextField(
                        value = reconcileAmountInput,
                        onValueChange = { reconcileAmountInput = it },
                        label = { Text("Actual balance") },
                        prefix = { Text("₵ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reconcile_amount_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pesewas = Formatters.evaluateExpressionToPesewas(reconcileAmountInput) ?: 0L
                        viewModel.repository.reconcileAccount(acc.id, pesewas)
                        reconcilingAccount = null
                    },
                    modifier = Modifier.testTag("confirm_reconcile_button")
                ) {
                    Text("Reconcile")
                }
            },
            dismissButton = {
                TextButton(onClick = { reconcilingAccount = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Account Dialog
    if (showAddAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("New Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newAccName,
                        onValueChange = { newAccName = it },
                        label = { Text("Account name (e.g. Fidelity Bank)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_account_name_input")
                    )
                    OutlinedTextField(
                        value = newAccEmoji,
                        onValueChange = { newAccEmoji = it },
                        label = { Text("Emoji icon") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAccOpeningBalance,
                        onValueChange = { newAccOpeningBalance = it },
                        label = { Text("Opening balance") },
                        prefix = { Text("₵ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAccName.isNotBlank()) {
                            val openBal = Formatters.evaluateExpressionToPesewas(newAccOpeningBalance) ?: 0L
                            viewModel.repository.addAccount(
                                Account(
                                    name = newAccName.trim(),
                                    emoji = newAccEmoji.trim().ifEmpty { "💳" },
                                    kind = newAccKind,
                                    openingBalancePesewas = openBal
                                )
                            )
                        }
                        showAddAccountDialog = false
                        newAccName = ""
                        newAccOpeningBalance = "0"
                    },
                    modifier = Modifier.testTag("confirm_add_account_button")
                ) {
                    Text("Create Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
