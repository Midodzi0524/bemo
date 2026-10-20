package com.example.bemo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bemo.model.*
import com.example.ui.theme.LocalBemoColors
import java.util.Calendar

/**
 * 'Recent Transactions' List Component.
 * Displays recent income and expenses, and allows users to manually log transactions
 * with a category tag, date, and amount to keep their budget current.
 */
@Composable
fun RecentTransactionsComponent(
    transactions: List<Transaction>,
    categories: List<Category>,
    accounts: List<Account>,
    hideAmounts: Boolean = false,
    maxItems: Int = 5,
    onAddTransaction: (type: TransactionType, amountPesewas: Long, categoryId: String?, accountId: String, note: String, dateMillis: Long) -> Unit,
    onDeleteTransaction: (id: String) -> Unit,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val extended = LocalBemoColors.current
    var showManualAddDialog by remember { mutableStateOf(false) }

    val catMap = remember(categories) { categories.associateBy { it.id } }
    val accMap = remember(accounts) { accounts.associateBy { it.id } }

    val recentTxns = remember(transactions, maxItems) {
        transactions.filter { !it.isDeleted && it.status == TransactionStatus.POSTED }.take(maxItems)
    }

    BemoBentoCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = extended.border,
        modifier = modifier.fillMaxWidth().testTag("recent_transactions_card")
    ) {
        // 1. Header: Title, "+ Add" button, and optional "See all"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Keep your budget current",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onSeeAllClick != null && transactions.size > maxItems) {
                    TextButton(
                        onClick = onSeeAllClick,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.testTag("recent_txns_see_all")
                    ) {
                        Text("See all", style = MaterialTheme.typography.labelSmall)
                    }
                }

                FilledTonalButton(
                    onClick = { showManualAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("recent_txns_manual_add_btn")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Transaction List Rows
        if (recentTxns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🧾", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No recent transactions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap Add above to log your first income or expense",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textMuted
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recentTxns.forEach { txn ->
                    val cat = txn.categoryId?.let { catMap[it] }
                    val acc = accMap[txn.accountId]
                    val isIncome = txn.type == TransactionType.INCOME

                    val emoji = when (txn.type) {
                        TransactionType.INCOME -> cat?.emoji ?: "💰"
                        TransactionType.EXPENSE -> cat?.emoji ?: "💸"
                        TransactionType.SAVING -> "🎯"
                        TransactionType.TRANSFER -> "🔄"
                    }

                    val title = if (txn.note.isNotBlank()) txn.note else (cat?.name ?: txn.type.label)
                    val dateLabel = "${Formatters.formatDate(txn.dateMillis)} · ${Formatters.formatTime(txn.dateMillis)}"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(extended.surfaceAlt.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isIncome) extended.successTint else extended.surfaceAlt),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Category Tag Chip
                                    if (cat != null) {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(" · ", style = MaterialTheme.typography.labelSmall, color = extended.textDisabled)
                                    }
                                    Text(
                                        text = "${acc?.name ?: "Account"} · $dateLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = extended.textMuted
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val amountText = when (txn.type) {
                                TransactionType.INCOME -> Formatters.formatPesewas(txn.amountPesewas, hideAmounts, showSign = true)
                                TransactionType.EXPENSE -> "−" + Formatters.formatPesewas(txn.amountPesewas, hideAmounts)
                                else -> Formatters.formatPesewas(txn.amountPesewas, hideAmounts)
                            }
                            val amountColor = if (isIncome) extended.incomeLine else MaterialTheme.colorScheme.onSurface

                            Text(
                                text = amountText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = amountColor
                            )
                            IconButton(
                                onClick = { onDeleteTransaction(txn.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = extended.textDisabled,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Manual Add Income / Expense Dialog
    if (showManualAddDialog) {
        ManualAddTransactionDialog(
            categories = categories,
            accounts = accounts,
            onDismiss = { showManualAddDialog = false },
            onConfirm = { type, amountPesewas, categoryId, accountId, note, dateMillis ->
                onAddTransaction(type, amountPesewas, categoryId, accountId, note, dateMillis)
                showManualAddDialog = false
            }
        )
    }
}

/**
 * Clean dialog for manually adding an income or expense transaction
 * with category tag, date, amount, account, and note.
 */
@Composable
fun ManualAddTransactionDialog(
    categories: List<Category>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (type: TransactionType, amountPesewas: Long, categoryId: String?, accountId: String, note: String, dateMillis: Long) -> Unit
) {
    val extended = LocalBemoColors.current

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    val expenseCats = remember(categories) { categories.filter { it.kind == CategoryKind.EXPENSE } }
    val incomeCats = remember(categories) { categories.filter { it.kind == CategoryKind.INCOME } }

    var selectedCatId by remember(selectedType) {
        val list = if (selectedType == TransactionType.INCOME) incomeCats else expenseCats
        mutableStateOf(list.firstOrNull()?.id)
    }

    var selectedAccId by remember(accounts) {
        mutableStateOf(accounts.firstOrNull { it.isDefault }?.id ?: accounts.firstOrNull()?.id ?: "")
    }

    // Date options: 0 = Today, 1 = Yesterday, 2 = 2 days ago
    var dateOffsetDays by remember { mutableStateOf(0) }

    val evaluatedPesewas = remember(amountInput) {
        Formatters.evaluateExpressionToPesewas(amountInput) ?: 0L
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Transaction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Type Switcher: Expense vs Income
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(extended.surfaceAlt)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(TransactionType.EXPENSE to "Expense", TransactionType.INCOME to "Income").forEach { (type, label) ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedType = type }
                                .padding(vertical = 8.dp)
                                .testTag("manual_type_${label.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else extended.textMuted
                            )
                        }
                    }
                }

                // 2. Amount Input
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount") },
                    prefix = { Text("₵ ") },
                    placeholder = { Text("0.00") },
                    supportingText = {
                        if (evaluatedPesewas > 0) {
                            Text("Total: ${Formatters.formatPesewas(evaluatedPesewas)}", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("manual_amount_input")
                )

                // 3. Category Tag Selector
                Column {
                    Text("Category Tag", style = MaterialTheme.typography.labelSmall, color = extended.textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    val availableCats = if (selectedType == TransactionType.INCOME) incomeCats else expenseCats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableCats.forEach { cat ->
                            FilterChip(
                                selected = selectedCatId == cat.id,
                                onClick = { selectedCatId = cat.id },
                                label = { Text("${cat.emoji} ${cat.name}") },
                                modifier = Modifier.testTag("manual_cat_${cat.name.lowercase().take(5)}")
                            )
                        }
                    }
                }

                // 4. Date Selection Pills (Today, Yesterday, 2 Days Ago)
                Column {
                    Text("Date", style = MaterialTheme.typography.labelSmall, color = extended.textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "Today", 1 to "Yesterday", 2 to "2 days ago").forEach { (offset, label) ->
                            FilterChip(
                                selected = dateOffsetDays == offset,
                                onClick = { dateOffsetDays = offset },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 5. Account Selection
                Column {
                    Text("Account", style = MaterialTheme.typography.labelSmall, color = extended.textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = selectedAccId == acc.id,
                                onClick = { selectedAccId = acc.id },
                                label = { Text("${acc.emoji} ${acc.name}") }
                            )
                        }
                    }
                }

                // 6. Note (Optional)
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Note / Description (Optional)") },
                    placeholder = { Text("e.g. Lunch, Groceries, Project stipend") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("manual_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (evaluatedPesewas > 0 && selectedAccId.isNotBlank()) {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, -dateOffsetDays)
                        onConfirm(
                            selectedType,
                            evaluatedPesewas,
                            selectedCatId,
                            selectedAccId,
                            noteInput.trim(),
                            cal.timeInMillis
                        )
                    }
                },
                enabled = evaluatedPesewas > 0 && selectedAccId.isNotBlank(),
                modifier = Modifier.testTag("manual_save_btn")
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
