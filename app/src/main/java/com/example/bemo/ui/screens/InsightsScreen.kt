package com.example.bemo.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bemo.model.Formatters
import com.example.bemo.ui.BemoViewModel
import com.example.bemo.ui.components.BemoBentoCard
import com.example.bemo.ui.components.CategoryDonutChart
import com.example.bemo.ui.components.MonthlyComparisonBarChart
import com.example.bemo.ui.components.PaceProgressBar
import com.example.bemo.ui.components.SixMonthTrendLineChart
import com.example.ui.theme.LocalBemoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: BemoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dashboardState by viewModel.dashboardState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val wishlistItems by viewModel.wishlistItems.collectAsState()
    val extended = LocalBemoColors.current
    val scrollState = rememberScrollState()

    val savingsRatePercent = if (dashboardState.totalIncomePesewas > 0) {
        val saved = dashboardState.totalIncomePesewas - dashboardState.totalSpentPesewas
        ((saved.toFloat() / dashboardState.totalIncomePesewas.toFloat()) * 100).toInt().coerceAtLeast(0)
    } else 0

    val avgDailySpendPesewas = if (dashboardState.daysLeftInMonth > 0) {
        dashboardState.totalSpentPesewas / maxOf(1, 30 - dashboardState.daysLeftInMonth)
    } else 0L

    // Compute Financial Health Score (0 - 100)
    // 1. Budget discipline (under monthly cap): max 40 pts
    val budgetScore = if (dashboardState.monthlyExpenseLimitPesewas > 0) {
        val ratio = dashboardState.totalSpentPesewas.toFloat() / dashboardState.monthlyExpenseLimitPesewas.toFloat()
        when {
            ratio <= 0.8f -> 40
            ratio <= 1.0f -> 30
            ratio <= 1.15f -> 15
            else -> 0
        }
    } else 30

    // 2. Savings rate: max 30 pts (>= 20% -> 30 pts)
    val savingsScore = when {
        savingsRatePercent >= 25 -> 30
        savingsRatePercent >= 15 -> 22
        savingsRatePercent >= 5 -> 12
        else -> 5
    }

    // 3. Emergency cushion: max 20 pts
    val emergencyItem = wishlistItems.firstOrNull { it.id == "wsh_emergency" || it.title.lowercase().contains("emergency") }
    val emergencyFundPesewas = emergencyItem?.savedAmountPesewas ?: 0L
    val monthlyBurnPesewas = maxOf(dashboardState.totalSpentPesewas, 200000L)
    val runwayMonths = if (monthlyBurnPesewas > 0) {
        (emergencyFundPesewas.toFloat() / monthlyBurnPesewas.toFloat())
    } else 0f

    val runwayScore = when {
        runwayMonths >= 3.0f -> 20
        runwayMonths >= 1.0f -> 14
        runwayMonths >= 0.5f -> 8
        else -> 4
    }

    // 4. Overbudget categories penalty
    val overBudgetCount = dashboardState.topCategories.count { it.isOver }
    val categoryScore = maxOf(0, 10 - (overBudgetCount * 4))

    val totalHealthScore = (budgetScore + savingsScore + runwayScore + categoryScore).coerceIn(0, 100)

    val (gradeLetter, gradeColor, gradeTitle) = when {
        totalHealthScore >= 85 -> Triple("A", extended.success, "Excellent Financial Health")
        totalHealthScore >= 70 -> Triple("B", extended.primaryTint, "Solid Financial Habits")
        totalHealthScore >= 55 -> Triple("C", extended.warning, "Fair · Room to Optimize")
        else -> Triple("D", extended.danger, "Needs Attention")
    }

    fun shareSummary() {
        val shareText = buildString {
            appendLine("📊 Bemo Financial Summary · October 2026")
            appendLine("---------------------------------------")
            appendLine("• Health Score: $totalHealthScore/100 (Grade $gradeLetter)")
            appendLine("• Total Income: ${Formatters.formatPesewas(dashboardState.totalIncomePesewas)}")
            appendLine("• Total Spent: ${Formatters.formatPesewas(dashboardState.totalSpentPesewas)}")
            appendLine("• Net Savings: ${Formatters.formatPesewas(maxOf(0L, dashboardState.totalIncomePesewas - dashboardState.totalSpentPesewas))}")
            appendLine("• Savings Rate: $savingsRatePercent%")
            appendLine("• Remaining Cap: ${Formatters.formatPesewas(dashboardState.remainingBudgetPesewas)}")
            appendLine("---------------------------------------")
            appendLine("Managed with Bemo — Give every cedi a job.")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Bemo Financial Summary")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Financial Summary"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Insights", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("insights_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { shareSummary() }, modifier = Modifier.testTag("insights_share_button")) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share Report")
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
            // 1. Financial Health Scorecard
            BemoBentoCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = extended.border
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FINANCIAL HEALTH SCORE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = extended.textMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = gradeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Score Circular badge
                    Surface(
                        shape = CircleShape,
                        color = extended.surfaceAlt,
                        border = androidx.compose.foundation.BorderStroke(2.dp, gradeColor),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$totalHealthScore",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                PaceProgressBar(progressFraction = totalHealthScore / 100f)

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = extended.border.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // Breakdown pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Budget Cap", style = MaterialTheme.typography.labelSmall, color = extended.textMuted)
                        Text(
                            text = if (dashboardState.isOverBudget) "Over Limit" else "${(dashboardState.budgetSpentFraction * 100).toInt()}% used",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (dashboardState.isOverBudget) extended.dangerText else extended.successText
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Savings Rate", style = MaterialTheme.typography.labelSmall, color = extended.textMuted)
                        Text(
                            text = "$savingsRatePercent%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = extended.successText
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Runway", style = MaterialTheme.typography.labelSmall, color = extended.textMuted)
                        Text(
                            text = "${String.format("%.1f", runwayMonths)} mos",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 2. Metrics row: Savings Rate & Avg Daily Spend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BemoBentoCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = extended.surfaceAlt
                ) {
                    Text("Savings Rate", style = MaterialTheme.typography.labelMedium, color = extended.textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$savingsRatePercent%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = extended.successText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("of monthly income", style = MaterialTheme.typography.bodySmall, color = extended.textMuted)
                }

                BemoBentoCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = extended.surfaceAlt
                ) {
                    Text("Avg Daily Spend", style = MaterialTheme.typography.labelMedium, color = extended.textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Formatters.formatPesewas(avgDailySpendPesewas, settings.hideAmounts),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("so far this month", style = MaterialTheme.typography.bodySmall, color = extended.textMuted)
                }
            }

            // 3. Smart Recommendations & Tips
            BemoBentoCard(
                backgroundColor = extended.surfaceAlt,
                borderColor = extended.border
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Smart Financial Tips",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Tip 1
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (dashboardState.remainingBudgetPesewas > 0)
                            "You have ${Formatters.formatPesewas(dashboardState.remainingBudgetPesewas, settings.hideAmounts)} buffer left before reaching your monthly cap."
                        else
                            "You are currently ${Formatters.formatPesewas(dashboardState.totalSpentPesewas - dashboardState.monthlyExpenseLimitPesewas, settings.hideAmounts)} above your spending cap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Tip 2
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your emergency cushion (${Formatters.formatPesewas(emergencyFundPesewas, settings.hideAmounts)}) covers approximately ${String.format("%.1f", runwayMonths)} months of essential living expenses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 4. Spend breakdown Donut
            BemoBentoCard {
                Text(
                    text = "Spending by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                CategoryDonutChart(
                    items = dashboardState.topCategories,
                    totalSpentPesewas = dashboardState.totalSpentPesewas,
                    hideAmounts = settings.hideAmounts
                )
            }

            // 5. Monthly Comparison
            BemoBentoCard {
                Text(
                    text = "Monthly Flow Comparison",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                MonthlyComparisonBarChart(
                    incomePesewas = dashboardState.totalIncomePesewas,
                    spentPesewas = dashboardState.totalSpentPesewas,
                    savedPesewas = dashboardState.totalSavedPesewas,
                    hideAmounts = settings.hideAmounts
                )
            }

            // 6. 6-Month Income & Spending Trend
            if (dashboardState.monthlyTrends.isNotEmpty()) {
                BemoBentoCard {
                    SixMonthTrendLineChart(
                        dataPoints = dashboardState.monthlyTrends,
                        hideAmounts = settings.hideAmounts
                    )
                }
            }

            // 7. Top categories breakdown list
            Text(
                text = "Detailed Category Spend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            dashboardState.topCategories.forEach { item ->
                BemoBentoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.category.emoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = Formatters.formatPesewas(item.spentPesewas, settings.hideAmounts),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Share Report Button at bottom
            OutlinedButton(
                onClick = { shareSummary() },
                modifier = Modifier.fillMaxWidth().testTag("insights_share_report_btn")
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Monthly Summary Report")
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

