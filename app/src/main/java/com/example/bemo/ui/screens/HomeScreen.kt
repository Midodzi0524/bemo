package com.example.bemo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Flag
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
import com.example.bemo.model.AlertThreshold
import com.example.bemo.model.Formatters
import com.example.bemo.model.TransactionType
import com.example.bemo.ui.BemoTab
import com.example.bemo.ui.BemoViewModel
import com.example.bemo.ui.SubScreen
import com.example.bemo.ui.components.BemoBentoCard
import com.example.bemo.ui.components.PaceProgressBar
import com.example.bemo.ui.components.RecentTransactionsComponent
import com.example.ui.theme.LocalBemoColors

/**
 * Clean, Simple, and Minimalist Home Screen.
 * Presents key financial vitals at a glance without visual clutter:
 * 1. Clean Balance Card with inline Income & Expense stats
 * 2. Quick Action Pills for effortless logging
 * 3. Minimalist Monthly Budget Glance with progress and remaining runway
 * 4. Active Savings Goal preview (if available)
 * 5. Recent Transactions with category tags and manual logging
 */
@Composable
fun HomeScreen(
    viewModel: BemoViewModel,
    onOpenAdd: () -> Unit
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val wishlistItems by viewModel.wishlistItems.collectAsState()
    val txns by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val extended = LocalBemoColors.current
    val scrollState = rememberScrollState()

    val featuredGoal = wishlistItems.firstOrNull { !it.isPurchased }
    val alerts by viewModel.alerts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Budget Alert Banner (80% or 100% threshold reached)
        val activeAlerts = alerts.filter { !it.isRead }
        if (activeAlerts.isNotEmpty()) {
            val mostSevere = activeAlerts.firstOrNull { it.threshold == AlertThreshold.LIMIT_100 } ?: activeAlerts.first()
            val is100 = mostSevere.threshold == AlertThreshold.LIMIT_100

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { viewModel.navigateTo(SubScreen.NOTIFICATIONS) }
                    .testTag("home_budget_alert_banner"),
                color = if (is100) MaterialTheme.colorScheme.errorContainer else extended.surfaceAlt,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (is100) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else extended.warning.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (is100) "🚨" else "⚠️", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (is100) "Limit Reached" else "80% Limit Warning",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (is100) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (activeAlerts.size > 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (is100) MaterialTheme.colorScheme.error else extended.warning.copy(alpha = 0.3f)
                                ) {
                                    Text(
                                        text = "+${activeAlerts.size - 1} more",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        color = if (is100) MaterialTheme.colorScheme.onError else extended.textMuted
                                    )
                                }
                            }
                        }
                        Text(
                            text = mostSevere.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (is100) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f) else extended.textMuted,
                            maxLines = 1
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View alerts",
                        tint = if (is100) MaterialTheme.colorScheme.error else extended.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 1. Minimalist Primary Balance Card
        BemoBentoCard(
            backgroundColor = MaterialTheme.colorScheme.surface,
            borderColor = extended.border,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_balance_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL BALANCE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = extended.textMuted
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = extended.surfaceAlt
                ) {
                    Text(
                        text = "October",
                        style = MaterialTheme.typography.labelSmall,
                        color = extended.textMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Formatters.formatPesewas(dashboardState.netBalancePesewas, settings.hideAmounts),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("home_net_balance_text")
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = extended.border.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Inline Income & Spent Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Income
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(extended.successTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = null,
                            tint = extended.incomeLine,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall,
                            color = extended.textMuted
                        )
                        Text(
                            text = "+${Formatters.formatPesewasShort(dashboardState.totalIncomePesewas, settings.hideAmounts)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = extended.incomeLine
                        )
                    }
                }

                // Spent
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(extended.dangerTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDownward,
                            contentDescription = null,
                            tint = extended.expenseLine,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = extended.textMuted
                        )
                        Text(
                            text = "-${Formatters.formatPesewasShort(dashboardState.totalSpentPesewas, settings.hideAmounts)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 2. Minimalist Quick Action Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_quick_actions"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quick Expense
            Surface(
                onClick = { viewModel.openAddSheet(TransactionType.EXPENSE) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, extended.border),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("home_quick_expense_btn")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = extended.expenseLine
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Quick Income
            Surface(
                onClick = { viewModel.openAddSheet(TransactionType.INCOME) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, extended.border),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("home_quick_income_btn")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = extended.incomeLine
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Quick Goals
            Surface(
                onClick = { viewModel.selectTab(BemoTab.WISHLIST) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, extended.border),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("home_quick_goals_btn")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎯", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Goals",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Quick Insights
            Surface(
                onClick = { viewModel.navigateTo(com.example.bemo.ui.SubScreen.INSIGHTS) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, extended.border),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("home_quick_insights_btn")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📊", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 2b. Subtle Upcoming Bill Notice (Minimalist & Functional)
        val nextBill = dashboardState.upcomingBills.firstOrNull()
        if (nextBill != null) {
            val dueDays = ((nextBill.nextDueDateMillis - System.currentTimeMillis()) / 86400000L).coerceAtLeast(0)
            Surface(
                onClick = { viewModel.navigateTo(com.example.bemo.ui.SubScreen.RECURRING) },
                shape = RoundedCornerShape(12.dp),
                color = extended.surfaceAlt,
                border = androidx.compose.foundation.BorderStroke(1.dp, extended.border.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_upcoming_bill_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⚡", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Next bill: ${nextBill.title} · ${Formatters.formatPesewas(nextBill.amountPesewas, settings.hideAmounts)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = if (dueDays == 0L) "Due today" else "in $dueDays d",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = extended.primaryTint.let { MaterialTheme.colorScheme.primary }
                    )
                }
            }
        }

        // 3. Minimalist Monthly Budget Glance
        BemoBentoCard(
            backgroundColor = MaterialTheme.colorScheme.surface,
            borderColor = extended.border,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_budget_glance_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Budget",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${Formatters.formatPesewas(dashboardState.remainingBudgetPesewas, settings.hideAmounts)} remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textMuted
                    )
                }

                TextButton(
                    onClick = { viewModel.selectTab(BemoTab.BUDGET) },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.testTag("home_view_budget_btn")
                ) {
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sleek progress line
            PaceProgressBar(
                progressFraction = dashboardState.budgetSpentFraction.coerceIn(0f, 1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(dashboardState.budgetSpentFraction * 100).toInt()}% of ${Formatters.formatPesewasShort(dashboardState.monthlyExpenseLimitPesewas, settings.hideAmounts)} limit",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textMuted
                )
                Text(
                    text = "${dashboardState.daysLeftInMonth} days left",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = extended.textMuted
                )
            }
        }

        // 4. Featured Goal Preview (if active)
        if (featuredGoal != null) {
            val percent = if (featuredGoal.targetAmountPesewas > 0) {
                (featuredGoal.savedAmountPesewas * 100 / featuredGoal.targetAmountPesewas).toInt()
            } else 0

            BemoBentoCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = extended.border,
                onClick = { viewModel.selectTab(BemoTab.WISHLIST) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_featured_goal_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(featuredGoal.emoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = featuredGoal.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "${Formatters.formatPesewas(featuredGoal.savedAmountPesewas, settings.hideAmounts)} of ${Formatters.formatPesewas(featuredGoal.targetAmountPesewas, settings.hideAmounts)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.textMuted
                            )
                        }
                    }

                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = extended.successText
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                PaceProgressBar(progressFraction = (percent / 100f).coerceIn(0f, 1f))
            }
        }

        // 5. Minimalist Recent Transactions
        RecentTransactionsComponent(
            transactions = txns,
            categories = categories,
            accounts = accounts,
            hideAmounts = settings.hideAmounts,
            maxItems = 4,
            onAddTransaction = { type, amountPesewas, categoryId, accountId, note, dateMillis ->
                viewModel.saveTransaction(
                    type = type,
                    amountPesewas = amountPesewas,
                    accountId = accountId,
                    categoryId = categoryId,
                    note = note,
                    dateMillis = dateMillis
                )
            },
            onDeleteTransaction = { id ->
                viewModel.deleteTransaction(id)
            },
            onSeeAllClick = {
                viewModel.selectTab(BemoTab.TRANSACTIONS)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
