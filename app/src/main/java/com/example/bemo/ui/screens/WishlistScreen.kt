package com.example.bemo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.bemo.model.Formatters
import com.example.bemo.model.WishlistKind
import com.example.bemo.model.WishlistItem
import com.example.bemo.ui.BemoViewModel
import com.example.bemo.ui.components.AddWishlistItemSheet
import com.example.bemo.ui.components.BemoBentoCard
import com.example.bemo.ui.components.PaceProgressBar
import com.example.bemo.ui.components.WishlistCard
import com.example.ui.theme.LocalBemoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(viewModel: BemoViewModel) {
    val items by viewModel.wishlistItems.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val extended = LocalBemoColors.current

    var selectedFilterKind by remember { mutableStateOf<WishlistKind?>(null) }
    var showAddItemSheet by remember { mutableStateOf(false) }

    var customContributingItem by remember { mutableStateOf<WishlistItem?>(null) }
    var customContributeAmountInput by remember { mutableStateOf("") }
    var customContributeAccountId by remember { mutableStateOf<String?>(null) }

    var itemToDelete by remember { mutableStateOf<WishlistItem?>(null) }

    val filteredItems = remember(items, selectedFilterKind) {
        if (selectedFilterKind == null) items
        else items.filter { it.kind == selectedFilterKind }
    }

    val totalTargetPesewas = remember(items) { items.sumOf { it.targetAmountPesewas } }
    val totalSavedPesewas = remember(items) { items.sumOf { it.savedAmountPesewas } }
    val overallProgress = if (totalTargetPesewas > 0) (totalSavedPesewas.toFloat() / totalTargetPesewas.toFloat()).coerceIn(0f, 1f) else 0f
    val overallPercent = (overallProgress * 100).toInt()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("wishlist_fab_add")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add to Wishlist")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Overall Wishlist & Goals Progress Bento Card
                BemoBentoCard(
                    backgroundColor = extended.primaryTint,
                    borderColor = extended.primaryTint
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Wishlist & Goals Portfolio",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = extended.onPrimaryTint
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${Formatters.formatPesewas(totalSavedPesewas, settings.hideAmounts)} saved",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = extended.onPrimaryTint
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$overallPercent% funded",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    PaceProgressBar(progressFraction = overallProgress)
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Total targets: ${Formatters.formatPesewas(totalTargetPesewas, settings.hideAmounts)} across ${items.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.onPrimaryTint.copy(alpha = 0.85f)
                    )
                }
            }

            // Filter Pills: All, Goals, Wants
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilterKind == null,
                            onClick = { selectedFilterKind = null },
                            label = { Text("All Items (${items.size})") },
                            modifier = Modifier.testTag("wishlist_filter_all")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilterKind == WishlistKind.GOAL,
                            onClick = { selectedFilterKind = WishlistKind.GOAL },
                            label = { Text("Savings Goals (${items.count { it.kind == WishlistKind.GOAL }})") },
                            modifier = Modifier.testTag("wishlist_filter_goals")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilterKind == WishlistKind.WANT,
                            onClick = { selectedFilterKind = WishlistKind.WANT },
                            label = { Text("Wants (${items.count { it.kind == WishlistKind.WANT }})") },
                            modifier = Modifier.testTag("wishlist_filter_wants")
                        )
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎯", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No wishlist items here",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the + button to add a product or goal",
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.textMuted
                            )
                        }
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    WishlistCard(
                        item = item,
                        hideAmounts = settings.hideAmounts,
                        onQuickContribute = { quickPesewas ->
                            val defaultAccId = accounts.firstOrNull { it.isDefault }?.id ?: accounts.firstOrNull()?.id
                            if (defaultAccId != null) {
                                viewModel.repository.contributeToWishlist(item.id, quickPesewas, defaultAccId)
                            }
                        },
                        onCustomContributeClick = {
                            customContributingItem = item
                            customContributeAccountId = accounts.firstOrNull()?.id
                            customContributeAmountInput = ""
                        },
                        onTogglePurchased = {
                            viewModel.repository.updateWishlistItem(item.copy(isPurchased = !item.isPurchased))
                        },
                        onDeleteItem = {
                            itemToDelete = item
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Add Wishlist Item Bottom Sheet
    if (showAddItemSheet) {
        AddWishlistItemSheet(
            onDismiss = { showAddItemSheet = false },
            onAddItem = { newItem ->
                viewModel.repository.addWishlistItem(newItem)
            }
        )
    }

    // Custom Contribution Dialog
    customContributingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { customContributingItem = null },
            title = { Text("Contribute to ${item.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customContributeAmountInput,
                        onValueChange = { customContributeAmountInput = it },
                        label = { Text("Contribution amount") },
                        prefix = { Text("₵ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_contribute_input")
                    )

                    Text("Deduct from account:")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        accounts.take(3).forEach { acc ->
                            FilterChip(
                                selected = customContributeAccountId == acc.id,
                                onClick = { customContributeAccountId = acc.id },
                                label = { Text("${acc.emoji} ${acc.name}") },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pesewas = Formatters.evaluateExpressionToPesewas(customContributeAmountInput) ?: 0L
                        if (pesewas > 0 && customContributeAccountId != null) {
                            viewModel.repository.contributeToWishlist(item.id, pesewas, customContributeAccountId!!)
                        }
                        customContributingItem = null
                    },
                    modifier = Modifier.testTag("confirm_custom_contribute_button")
                ) {
                    Text("Save Contribution")
                }
            },
            dismissButton = {
                TextButton(onClick = { customContributingItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Item Confirmation Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Wishlist Item?") },
            text = { Text("Are you sure you want to remove '${item.title}' from your wishlist?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.repository.deleteWishlistItem(item.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = extended.danger)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
