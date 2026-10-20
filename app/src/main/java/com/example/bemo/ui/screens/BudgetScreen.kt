package com.example.bemo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Edit
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
import com.example.bemo.model.Category
import com.example.bemo.model.CategoryKind
import com.example.bemo.model.Formatters
import com.example.bemo.ui.BemoViewModel
import com.example.bemo.ui.CategorySpendProgress
import com.example.bemo.ui.components.BemoBentoCard
import com.example.bemo.ui.components.BudgetOverviewDashboard
import com.example.bemo.ui.components.PaceProgressBar
import com.example.ui.theme.LocalBemoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: BemoViewModel) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val extended = LocalBemoColors.current

    var editingCategory by remember { mutableStateOf<CategorySpendProgress?>(null) }
    var editAmountInput by remember { mutableStateOf("") }

    var showMoveMoneyDialog by remember { mutableStateOf(false) }
    var moveSourceCatId by remember { mutableStateOf<String?>(null) }
    var moveDestCatId by remember { mutableStateOf<String?>(null) }
    var moveAmountInput by remember { mutableStateOf("") }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }
    var newCatEmoji by remember { mutableStateOf("🏷️") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCategoryDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("budget_fab_add_cat")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Category")
            }
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
                BudgetOverviewDashboard(
                    dashboardState = dashboardState,
                    hideAmounts = settings.hideAmounts,
                    onSetLimits = { incomeLimit, expenseLimit ->
                        viewModel.updateBudgetLimits(incomeLimit, expenseLimit)
                    }
                )
            }

            item {
                // Summary card
                BemoBentoCard(
                    backgroundColor = if (dashboardState.unassignedPesewas >= 0) extended.primaryTint else extended.dangerTint,
                    borderColor = if (dashboardState.unassignedPesewas >= 0) extended.primaryTint else extended.danger
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Zero-Based Budget",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (dashboardState.unassignedPesewas >= 0) extended.onPrimaryTint else extended.dangerText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (dashboardState.unassignedPesewas >= 0)
                                    "Unassigned: ${Formatters.formatPesewas(dashboardState.unassignedPesewas, settings.hideAmounts)}"
                                else
                                    "Over-assigned: ${Formatters.formatPesewas(dashboardState.unassignedPesewas, settings.hideAmounts)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (dashboardState.unassignedPesewas >= 0) extended.onPrimaryTint else extended.dangerText
                            )
                        }

                        IconButton(
                            onClick = {
                                if (dashboardState.topCategories.isNotEmpty()) {
                                    moveSourceCatId = dashboardState.topCategories.first().category.id
                                    moveDestCatId = dashboardState.topCategories.getOrNull(1)?.category?.id
                                    showMoveMoneyDialog = true
                                }
                            },
                            modifier = Modifier.testTag("budget_move_money_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Move money",
                                tint = if (dashboardState.unassignedPesewas >= 0) extended.onPrimaryTint else extended.dangerText
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Income ${Formatters.formatPesewas(dashboardState.totalIncomePesewas, settings.hideAmounts)} · Assigned ${Formatters.formatPesewas(dashboardState.totalAssignedPesewas, settings.hideAmounts)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textMuted
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Allocations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap row to adjust",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textMuted
                    )
                }
            }

            items(dashboardState.topCategories, key = { it.category.id }) { item ->
                BemoBentoCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    onClick = {
                        editingCategory = item
                        editAmountInput = (item.assignedPesewas / 100.0).toString()
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(extended.surfaceAlt),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(item.category.emoji, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = item.category.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Spent ${Formatters.formatPesewas(item.spentPesewas, settings.hideAmounts)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.textMuted
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = Formatters.formatPesewas(item.assignedPesewas, settings.hideAmounts),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(14.dp),
                                    tint = extended.textDisabled
                                )
                            }
                            val statusText = when {
                                item.isOver -> "Over by ${Formatters.formatPesewas(item.spentPesewas - item.assignedPesewas, settings.hideAmounts)}"
                                item.isAheadOfPace -> "${(item.progressFraction * 100).toInt()}% ⚠ ahead"
                                else -> "${Formatters.formatPesewas(item.leftPesewas, settings.hideAmounts)} left"
                            }
                            val statusColor = when {
                                item.isOver -> extended.dangerText
                                item.isAheadOfPace -> extended.onWarning
                                else -> extended.successText
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    PaceProgressBar(progressFraction = item.progressFraction)
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Edit Allocation Dialog
    editingCategory?.let { progressItem ->
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Edit ${progressItem.category.name} Budget") },
            text = {
                Column {
                    Text("Enter monthly assigned amount in cedis:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editAmountInput,
                        onValueChange = { editAmountInput = it },
                        prefix = { Text("₵ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_allocation_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pesewas = Formatters.evaluateExpressionToPesewas(editAmountInput) ?: 0L
                        viewModel.repository.updateCategoryAllocation(progressItem.category.id, pesewas)
                        editingCategory = null
                    },
                    modifier = Modifier.testTag("save_allocation_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Move Money Dialog
    if (showMoveMoneyDialog) {
        AlertDialog(
            onDismissRequest = { showMoveMoneyDialog = false },
            title = { Text("Move Money Between Categories") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("From:")
                    // Simple category selector
                    val expenseCats = categories.filter { it.kind == CategoryKind.EXPENSE }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        expenseCats.take(4).forEach { cat ->
                            FilterChip(
                                selected = moveSourceCatId == cat.id,
                                onClick = { moveSourceCatId = cat.id },
                                label = { Text("${cat.emoji} ${cat.name}") },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                    Text("To:")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        expenseCats.take(4).forEach { cat ->
                            FilterChip(
                                selected = moveDestCatId == cat.id,
                                onClick = { moveDestCatId = cat.id },
                                label = { Text("${cat.emoji} ${cat.name}") },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = moveAmountInput,
                        onValueChange = { moveAmountInput = it },
                        label = { Text("Amount to move") },
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
                        val amt = Formatters.evaluateExpressionToPesewas(moveAmountInput) ?: 0L
                        if (moveSourceCatId != null && moveDestCatId != null && amt > 0) {
                            viewModel.repository.moveBudgetAllocation(moveSourceCatId!!, moveDestCatId!!, amt)
                        }
                        showMoveMoneyDialog = false
                    }
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveMoneyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("New Budget Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_cat_name_input")
                    )
                    OutlinedTextField(
                        value = newCatEmoji,
                        onValueChange = { newCatEmoji = it },
                        label = { Text("Emoji icon") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            val cat = Category(
                                name = newCatName.trim(),
                                emoji = newCatEmoji.trim().ifEmpty { "🏷️" },
                                kind = CategoryKind.EXPENSE
                            )
                            viewModel.repository.addCategory(cat)
                            viewModel.repository.updateCategoryAllocation(cat.id, 0L)
                        }
                        showAddCategoryDialog = false
                        newCatName = ""
                    },
                    modifier = Modifier.testTag("confirm_add_cat_button")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
