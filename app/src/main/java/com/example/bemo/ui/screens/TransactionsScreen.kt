package com.example.bemo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bemo.model.Formatters
import com.example.bemo.model.Transaction
import com.example.bemo.model.TransactionStatus
import com.example.bemo.model.TransactionType
import com.example.bemo.ui.BemoViewModel
import com.example.bemo.ui.components.BemoBentoCard
import com.example.ui.theme.LocalBemoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: BemoViewModel,
    onOpenAdd: () -> Unit
) {
    val txns by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedTypeFilter.collectAsState()
    val extended = LocalBemoColors.current

    val catMap = remember(categories) { categories.associateBy { it.id } }
    val accMap = remember(accounts) { accounts.associateBy { it.id } }

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedTxnForDetail by remember { mutableStateOf<Transaction?>(null) }

    // Filter transactions
    val filteredTxns = remember(txns, searchQuery, selectedFilter, selectedCategoryId) {
        txns.filter { t ->
            if (t.isDeleted) return@filter false
            if (selectedFilter != null && t.type != selectedFilter) return@filter false
            if (selectedCategoryId != null && t.categoryId != selectedCategoryId) return@filter false
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                val noteMatch = t.note.lowercase().contains(q)
                val catMatch = t.categoryId?.let { catMap[it]?.name?.lowercase()?.contains(q) } ?: false
                val accMatch = accMap[t.accountId]?.name?.lowercase()?.contains(q) ?: false
                val amtMatch = (t.amountPesewas / 100.0).toString().contains(q)
                noteMatch || catMatch || accMatch || amtMatch
            } else {
                true
            }
        }
    }

    val totalFilteredExpenses = remember(filteredTxns) {
        filteredTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPesewas }
    }
    val totalFilteredIncome = remember(filteredTxns) {
        filteredTxns.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPesewas }
    }

    // Group transactions by date label
    val groupedTxns = remember(filteredTxns) {
        filteredTxns.groupBy { t ->
            if (t.status == TransactionStatus.PLANNED) "Planned"
            else Formatters.formatDate(t.dateMillis)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("txns_fab_add")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar & Filter Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search transactions...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = extended.textMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = extended.surfaceAlt,
                        unfocusedContainerColor = extended.surfaceAlt,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("txn_search_input")
                )

                // Type Filter pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { viewModel.selectedTypeFilter.value = null },
                            label = { Text("All Types") },
                            modifier = Modifier.testTag("txn_filter_all")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == TransactionType.EXPENSE,
                            onClick = { viewModel.selectedTypeFilter.value = TransactionType.EXPENSE },
                            label = { Text("Expenses") },
                            modifier = Modifier.testTag("txn_filter_expense")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == TransactionType.INCOME,
                            onClick = { viewModel.selectedTypeFilter.value = TransactionType.INCOME },
                            label = { Text("Income") },
                            modifier = Modifier.testTag("txn_filter_income")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == TransactionType.SAVING,
                            onClick = { viewModel.selectedTypeFilter.value = TransactionType.SAVING },
                            label = { Text("Savings") },
                            modifier = Modifier.testTag("txn_filter_saving")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == TransactionType.TRANSFER,
                            onClick = { viewModel.selectedTypeFilter.value = TransactionType.TRANSFER },
                            label = { Text("Transfers") },
                            modifier = Modifier.testTag("txn_filter_transfer")
                        )
                    }
                }

                // Category Filter pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("All Categories") }
                        )
                    }
                    categories.forEach { cat ->
                        item(key = cat.id) {
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = {
                                    selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id
                                },
                                label = { Text("${cat.emoji} ${cat.name}") }
                            )
                        }
                    }
                }

                // Summary Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredTxns.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = extended.textMuted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (totalFilteredIncome > 0) {
                            Text(
                                text = "+${Formatters.formatPesewas(totalFilteredIncome, settings.hideAmounts)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = extended.incomeLine
                            )
                        }
                        if (totalFilteredExpenses > 0) {
                            Text(
                                text = "-${Formatters.formatPesewas(totalFilteredExpenses, settings.hideAmounts)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = extended.expenseLine
                            )
                        }
                    }
                }
            }

            // Transaction List
            if (filteredTxns.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🧾", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No transactions found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to log your first expense or income",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedTxns.forEach { (dateHeader, dateList) ->
                        item {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = extended.textMuted,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(dateList, key = { it.id }) { txn ->
                            val cat = txn.categoryId?.let { catMap[it] }
                            val acc = accMap[txn.accountId]
                            val toAcc = txn.toAccountId?.let { accMap[it] }

                            val emoji = when (txn.type) {
                                TransactionType.EXPENSE -> cat?.emoji ?: "💸"
                                TransactionType.INCOME -> cat?.emoji ?: "💰"
                                TransactionType.SAVING -> "🎯"
                                TransactionType.TRANSFER -> "🔄"
                            }

                            val title = when (txn.type) {
                                TransactionType.TRANSFER -> "${acc?.name ?: "Account"} → ${toAcc?.name ?: "Account"}"
                                else -> if (txn.note.isNotBlank()) txn.note else (cat?.name ?: txn.type.label)
                            }

                            val subtitle = when (txn.type) {
                                TransactionType.TRANSFER -> Formatters.formatTime(txn.dateMillis)
                                else -> "${cat?.name ?: txn.type.label} · ${acc?.name ?: "Account"} · ${Formatters.formatTime(txn.dateMillis)}"
                            }

                            val isPositive = txn.type == TransactionType.INCOME
                            val amountColor = when (txn.type) {
                                TransactionType.INCOME -> extended.incomeLine
                                TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
                                TransactionType.SAVING -> extended.onPrimaryTint
                                TransactionType.TRANSFER -> extended.textMuted
                            }

                            val amountText = when (txn.type) {
                                TransactionType.INCOME -> Formatters.formatPesewas(txn.amountPesewas, settings.hideAmounts, showSign = true)
                                TransactionType.EXPENSE -> "−" + Formatters.formatPesewas(txn.amountPesewas, settings.hideAmounts)
                                else -> Formatters.formatPesewas(txn.amountPesewas, settings.hideAmounts)
                            }

                            BemoBentoCard(
                                backgroundColor = MaterialTheme.colorScheme.surface,
                                onClick = { selectedTxnForDetail = txn },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(extended.surfaceAlt),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(emoji, fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = extended.textMuted,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = amountText,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = amountColor
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteTransaction(txn.id) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete transaction",
                                                tint = extended.textDisabled,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(70.dp))
                    }
                }
            }
        }
    }

    // Transaction Detail Dialog
    selectedTxnForDetail?.let { detail ->
        val cat = detail.categoryId?.let { catMap[it] }
        val acc = accMap[detail.accountId]
        val toAcc = detail.toAccountId?.let { accMap[it] }

        AlertDialog(
            onDismissRequest = { selectedTxnForDetail = null },
            title = { Text(detail.type.label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = Formatters.formatPesewas(detail.amountPesewas, settings.hideAmounts),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (detail.note.isNotBlank()) {
                        Text("Note: ${detail.note}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (cat != null) {
                        Text("Category: ${cat.emoji} ${cat.name}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("Account: ${acc?.emoji ?: ""} ${acc?.name ?: "Unknown"}", style = MaterialTheme.typography.bodyMedium)
                    if (toAcc != null) {
                        Text("To Account: ${toAcc.emoji} ${toAcc.name}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("Date: ${Formatters.formatDate(detail.dateMillis)} at ${Formatters.formatTime(detail.dateMillis)}", style = MaterialTheme.typography.bodySmall, color = extended.textMuted)
                }
            },
            confirmButton = {
                Button(onClick = { selectedTxnForDetail = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(detail.id)
                        selectedTxnForDetail = null
                    }
                ) {
                    Text("Delete", color = extended.dangerText)
                }
            }
        )
    }
}
