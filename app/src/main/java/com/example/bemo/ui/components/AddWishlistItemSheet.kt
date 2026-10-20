package com.example.bemo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.bemo.model.Formatters
import com.example.bemo.model.WishlistKind
import com.example.bemo.model.WishlistItem
import com.example.bemo.model.WishlistPriority
import com.example.ui.theme.LocalBemoColors
import java.util.Calendar

/**
 * Reusable modal sheet component for adding a new item to the wishlist or savings goals.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWishlistItemSheet(
    onDismiss: () -> Unit,
    onAddItem: (WishlistItem) -> Unit
) {
    val extended = LocalBemoColors.current

    var title by remember { mutableStateOf("") }
    var priceExpression by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🎯") }
    var selectedKind by remember { mutableStateOf(WishlistKind.WANT) }
    var selectedPriority by remember { mutableStateOf(WishlistPriority.HIGH) }
    var selectedTimelineMonths by remember { mutableStateOf<Int?>(null) }
    var notes by remember { mutableStateOf("") }

    val popularEmojis = listOf("💻", "📱", "🛟", "🏠", "🚗", "✈️", "🎓", "👟", "⌚", "🎮", "🚴", "📸", "🎧", "📺", "💎")

    val calculatedPesewas = remember(priceExpression) {
        Formatters.evaluateExpressionToPesewas(priceExpression) ?: 0L
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("add_wishlist_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add to Wishlist & Goals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close sheet", tint = extended.textMuted)
                }
            }

            // Kind Selector: Want vs Goal
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(extended.surfaceAlt)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(
                    WishlistKind.WANT to "Want (Product / Item)",
                    WishlistKind.GOAL to "Goal (Savings Target)"
                ).forEach { (kind, label) ->
                    val isSelected = selectedKind == kind
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedKind = kind }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("kind_tab_${kind.name.lowercase()}")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else extended.textMuted
                        )
                    }
                }
            }

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Item Name") },
                placeholder = { Text("e.g. MacBook Pro M3 or Emergency Fund") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("add_wishlist_title_input")
            )

            // Target Price Field
            OutlinedTextField(
                value = priceExpression,
                onValueChange = { priceExpression = it },
                label = { Text("Target Price / Amount") },
                prefix = { Text("₵ ") },
                placeholder = { Text("0.00") },
                supportingText = {
                    if (calculatedPesewas > 0) {
                        Text("Target: ${Formatters.formatPesewas(calculatedPesewas)}", color = MaterialTheme.colorScheme.primary)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("add_wishlist_price_input")
            )

            // Emoji Picker
            Column {
                Text("Pick Icon", style = MaterialTheme.typography.labelMedium, color = extended.textMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    popularEmojis.forEach { emoji ->
                        val isSelected = selectedEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) extended.primaryTint else extended.surfaceAlt)
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 20.sp)
                        }
                    }
                }
            }

            // Priority Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Priority:", style = MaterialTheme.typography.labelMedium, color = extended.textMuted)
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WishlistPriority.values().forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.testTag("priority_${priority.name.lowercase()}")
                        )
                    }
                }
            }

            // Target Timeline Chips
            Column {
                Text("Target Timeline (Optional):", style = MaterialTheme.typography.labelMedium, color = extended.textMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        null to "No date",
                        3 to "3 Months",
                        6 to "6 Months",
                        12 to "1 Year"
                    ).forEach { (months, label) ->
                        FilterChip(
                            selected = selectedTimelineMonths == months,
                            onClick = { selectedTimelineMonths = months },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes & details (optional)") },
                placeholder = { Text("Store link, model specifications, or reasons...") },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("add_wishlist_notes_input")
            )

            // Submit Button
            Button(
                onClick = {
                    if (title.isNotBlank() && calculatedPesewas > 0) {
                        val targetDate = selectedTimelineMonths?.let { months ->
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.MONTH, months)
                            cal.timeInMillis
                        }

                        val newItem = WishlistItem(
                            title = title.trim(),
                            targetAmountPesewas = calculatedPesewas,
                            savedAmountPesewas = 0L,
                            emoji = selectedEmoji,
                            kind = selectedKind,
                            priority = selectedPriority,
                            targetDateMillis = targetDate,
                            notes = notes.trim()
                        )
                        onAddItem(newItem)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && calculatedPesewas > 0,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_wishlist_submit_button")
            ) {
                Text(
                    text = "Add to Wishlist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
