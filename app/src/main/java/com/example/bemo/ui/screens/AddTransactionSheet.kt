package com.example.bemo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bemo.model.*
import com.example.bemo.ui.BemoViewModel
import com.example.ui.theme.LocalBemoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    viewModel: BemoViewModel,
    initialType: TransactionType = TransactionType.EXPENSE,
    onDismiss: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val templates by viewModel.quickAddTemplates.collectAsState()
    val duplicateWarning by viewModel.duplicateWarningTxn.collectAsState()
    val extended = LocalBemoColors.current

    var selectedType by remember { mutableStateOf(initialType) }
    var expressionInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    val expenseCategories = remember(categories) { categories.filter { it.kind == CategoryKind.EXPENSE } }
    val incomeCategories = remember(categories) { categories.filter { it.kind == CategoryKind.INCOME } }

    var selectedCategoryId by remember(selectedType, categories) {
        val initialList = if (selectedType == TransactionType.INCOME) incomeCategories else expenseCategories
        mutableStateOf(initialList.firstOrNull()?.id)
    }

    var selectedAccountId by remember(accounts) {
        mutableStateOf(accounts.firstOrNull { it.isDefault }?.id ?: accounts.firstOrNull()?.id)
    }

    var transferToAccountId by remember(accounts) {
        mutableStateOf(accounts.getOrNull(1)?.id)
    }

    // Evaluated amount in pesewas
    val currentPesewas = remember(expressionInput) {
        Formatters.evaluateExpressionToPesewas(expressionInput) ?: 0L
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("add_txn_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Type Segmented Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Segmented tabs
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(extended.surfaceAlt)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TransactionType.values().forEach { type ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedType = type }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("type_tab_${type.name.lowercase()}")
                        ) {
                            Text(
                                text = type.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else extended.textMuted
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close sheet", tint = extended.textMuted)
                }
            }

            // Quick add templates chips
            if (selectedType == TransactionType.EXPENSE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    templates.forEach { tpl ->
                        SuggestionChip(
                            onClick = {
                                expressionInput = (tpl.amountPesewas / 100.0).toString()
                                selectedCategoryId = tpl.categoryId
                                selectedAccountId = tpl.accountId
                                noteInput = tpl.label.substringBefore("₵").trim()
                            },
                            label = { Text(tpl.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Large Amount Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(extended.surfaceAlt)
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (expressionInput.isEmpty()) "₵ 0.00" else "₵ $expressionInput",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    if (expressionInput.any { it in "+-*/×÷" }) {
                        Text(
                            text = "= ${Formatters.formatPesewas(currentPesewas)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Numeric Keypad with Calculator Keys
            val keyRows = listOf(
                listOf("7", "8", "9", "÷"),
                listOf("4", "5", "6", "×"),
                listOf("1", "2", "3", "−"),
                listOf(".", "0", "⌫", "+")
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                keyRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { key ->
                            val isOperator = key in listOf("÷", "×", "−", "+")
                            val isAction = key == "⌫"
                            val btnBg = when {
                                isOperator -> extended.primaryTint
                                isAction -> extended.surfaceAlt
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val btnText = when {
                                isOperator -> extended.onPrimaryTint
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(btnBg)
                                    .border(1.dp, extended.border, RoundedCornerShape(12.dp))
                                    .clickable {
                                        when (key) {
                                            "⌫" -> {
                                                if (expressionInput.isNotEmpty()) {
                                                    expressionInput = expressionInput.dropLast(1)
                                                }
                                            }
                                            "÷" -> expressionInput += "÷"
                                            "×" -> expressionInput += "×"
                                            "−" -> expressionInput += "-"
                                            "+" -> expressionInput += "+"
                                            else -> expressionInput += key
                                        }
                                    }
                                    .testTag("keypad_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "⌫") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = extended.textMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = btnText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Picker (if not Transfer)
            if (selectedType != TransactionType.TRANSFER) {
                val availableCats = if (selectedType == TransactionType.INCOME) incomeCategories else expenseCategories
                Column {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = extended.textMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableCats.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text("${cat.emoji} ${cat.name}") },
                                modifier = Modifier.testTag("cat_chip_${cat.name.lowercase().take(5)}")
                            )
                        }
                    }
                }
            } else {
                // Transfer Account selection: From -> To
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("From Account", style = MaterialTheme.typography.labelSmall, color = extended.textMuted)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            accounts.forEach { acc ->
                                FilterChip(
                                    selected = selectedAccountId == acc.id,
                                    onClick = { selectedAccountId = acc.id },
                                    label = { Text("${acc.emoji} ${acc.name}") }
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("To Account", style = MaterialTheme.typography.labelSmall, color = extended.textMuted)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            accounts.forEach { acc ->
                                FilterChip(
                                    selected = transferToAccountId == acc.id,
                                    onClick = { transferToAccountId = acc.id },
                                    label = { Text("${acc.emoji} ${acc.name}") }
                                )
                            }
                        }
                    }
                }
            }

            // Account Picker (for Expense / Income / Saving)
            if (selectedType != TransactionType.TRANSFER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Account: ",
                        style = MaterialTheme.typography.labelMedium,
                        color = extended.textMuted
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text("${acc.emoji} ${acc.name}") },
                                modifier = Modifier.testTag("acc_chip_${acc.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Note Input
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                placeholder = { Text("Add optional note (e.g. Bolt to office)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = extended.surfaceAlt,
                    unfocusedContainerColor = extended.surfaceAlt,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth().testTag("add_txn_note_input")
            )

            // Duplicate warning reminder banner if triggered
            if (duplicateWarning != null) {
                Surface(
                    color = extended.warningTint,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ A similar transaction was logged in the last 2 minutes. Tap Save again to proceed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.onWarning,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (currentPesewas > 0 && selectedAccountId != null) {
                        viewModel.saveTransaction(
                            type = selectedType,
                            amountPesewas = currentPesewas,
                            accountId = selectedAccountId!!,
                            toAccountId = if (selectedType == TransactionType.TRANSFER) transferToAccountId else null,
                            categoryId = selectedCategoryId,
                            note = noteInput.trim()
                        )
                    }
                },
                enabled = currentPesewas > 0 && selectedAccountId != null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_txn_save_button")
            ) {
                Text(
                    text = if (duplicateWarning != null) "Confirm & Save" else "Save Transaction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
