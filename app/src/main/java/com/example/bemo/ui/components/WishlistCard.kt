package com.example.bemo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Savings
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
import com.example.bemo.model.WishlistItem
import com.example.bemo.model.WishlistPriority
import com.example.ui.theme.LocalBemoColors

/**
 * Reusable, modern UI component for an individual wishlist item.
 * Displays target prices, animated savings progress bar with milestone indicators,
 * and quick-contribution buttons.
 */
@Composable
fun WishlistCard(
    item: WishlistItem,
    hideAmounts: Boolean = false,
    onQuickContribute: (amountPesewas: Long) -> Unit,
    onCustomContributeClick: () -> Unit,
    onTogglePurchased: () -> Unit,
    onDeleteItem: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalBemoColors.current
    val animatedProgress by animateFloatAsState(targetValue = item.progressFraction, label = "wishlist_prog")
    var isExpanded by remember { mutableStateOf(false) }

    val priorityColor = when (item.priority) {
        WishlistPriority.HIGH -> extended.primaryTint to extended.onPrimaryTint
        WishlistPriority.MEDIUM -> extended.warningTint to extended.onWarning
        WishlistPriority.LOW -> extended.surfaceAlt to extended.textMuted
    }

    val cardBg = if (item.isPurchased) extended.surfaceAlt else MaterialTheme.colorScheme.surface

    BemoBentoCard(
        backgroundColor = cardBg,
        borderColor = if (item.isFunded && !item.isPurchased) extended.success else extended.border,
        modifier = modifier.fillMaxWidth().testTag("wishlist_card_${item.id}")
    ) {
        // 1. Header: Emoji, Title, Category Tag, Priority & Complete Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(extended.surfaceAlt),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.emoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Priority Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(priorityColor.first)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.priority.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = priorityColor.second,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (item.isPurchased) "Purchased & Acquired ✓" else item.milestoneText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (item.isFunded || item.isPurchased) extended.successText else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Quick Mark Complete / Purchased Button
            IconButton(
                onClick = onTogglePurchased,
                modifier = Modifier.testTag("wishlist_purchased_button_${item.id}")
            ) {
                Icon(
                    imageVector = if (item.isPurchased) Icons.Filled.CheckCircle else Icons.Default.Check,
                    contentDescription = if (item.isPurchased) "Mark not purchased" else "Mark purchased",
                    tint = if (item.isPurchased) extended.successText else extended.textDisabled,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Price and Target Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Saved so far",
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.textMuted
                )
                Text(
                    text = Formatters.formatPesewas(item.savedAmountPesewas, hideAmounts),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Target Price",
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.textMuted
                )
                Text(
                    text = Formatters.formatPesewas(item.targetAmountPesewas, hideAmounts),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Visual Progress Bar with Milestone Markers
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(extended.surfaceAlt)
        ) {
            // Animated fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(
                        if (item.isFunded) extended.success else MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(6.dp)
                    )
            )

            // Milestone ticks at 25%, 50%, 75%
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 4. Remaining Amount & Target projection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.remainingPesewas > 0 && !item.isPurchased) {
                Text(
                    text = "${Formatters.formatPesewas(item.remainingPesewas, hideAmounts)} needed",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = extended.textMuted
                )
            } else {
                Text(
                    text = "Target reached 🎉",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = extended.successText
                )
            }

            if (item.targetDateMillis != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Event,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = extended.textMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Target: ${Formatters.formatDate(item.targetDateMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textMuted
                    )
                }
            }
        }

        // 5. Quick Add Savings Action Chips
        if (!item.isPurchased) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = extended.border.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick add:",
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.textMuted
                )

                // +₵50 (5000 pesewas)
                SuggestionChip(
                    onClick = { onQuickContribute(5000L) },
                    label = { Text("+₵50", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("quick_add_50_${item.id}")
                )

                // +₵100 (10000 pesewas)
                SuggestionChip(
                    onClick = { onQuickContribute(10000L) },
                    label = { Text("+₵100", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("quick_add_100_${item.id}")
                )

                // +₵200 (20000 pesewas)
                SuggestionChip(
                    onClick = { onQuickContribute(20000L) },
                    label = { Text("+₵200", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("quick_add_200_${item.id}")
                )

                // Custom...
                FilledTonalButton(
                    onClick = onCustomContributeClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).testTag("custom_contribute_${item.id}")
                ) {
                    Icon(Icons.Outlined.Savings, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Custom", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // 6. Expandable Notes & Details
        if (item.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide details" else "View details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = onDeleteItem,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete item",
                                tint = extended.dangerText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete item", color = extended.dangerText, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
