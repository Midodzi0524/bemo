package com.example.bemo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bemo.model.Formatters
import com.example.bemo.ui.BemoDashboardState
import com.example.ui.theme.LocalBemoColors

/**
 * 'Budget Overview' Dashboard Component.
 * Visualizes the remaining monthly budget in a progress circle,
 * displays income and expense limits, and allows users to set and adjust monthly limits.
 */
@Composable
fun BudgetOverviewDashboard(
    dashboardState: BemoDashboardState,
    hideAmounts: Boolean = false,
    onSetLimits: (incomeLimitPesewas: Long, expenseLimitPesewas: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalBemoColors.current
    var showLimitsDialog by remember { mutableStateOf(false) }

    val spentFraction = dashboardState.budgetSpentFraction.coerceAtLeast(0f)
    val animatedProgress by animateFloatAsState(
        targetValue = spentFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "budget_circle_progress"
    )

    val progressRingColor = when {
        dashboardState.isOverBudget -> extended.danger
        spentFraction >= 0.8f -> extended.warning
        else -> extended.success
    }

    val spentPercent = (spentFraction * 100).toInt()

    BemoBentoCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = extended.border,
        modifier = modifier.fillMaxWidth().testTag("budget_overview_dashboard")
    ) {
        // 1. Header: Title, Month Badge, and Edit Limits Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Budget Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Monthly Spending Cap & Progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textMuted
                )
            }

            // "Set Limits" Button
            OutlinedButton(
                onClick = { showLimitsDialog = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("budget_set_limits_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Set Limits", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Centerpiece: Progress Circle Ring & Remaining Budget
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Circular Progress Gauge
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .testTag("budget_progress_circle"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)

                    // Background circular track
                    drawArc(
                        color = extended.surfaceAlt,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    // Foreground progress arc
                    val sweep = animatedProgress * 360f
                    if (sweep > 0f) {
                        drawArc(
                            color = progressRingColor,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Inner circle content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = if (dashboardState.isOverBudget) "Over by" else "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (dashboardState.isOverBudget) extended.dangerText else extended.textMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val remainingText = if (dashboardState.isOverBudget) {
                        Formatters.formatPesewas(dashboardState.totalSpentPesewas - dashboardState.monthlyExpenseLimitPesewas, hideAmounts)
                    } else {
                        Formatters.formatPesewas(dashboardState.remainingBudgetPesewas, hideAmounts)
                    }
                    Text(
                        text = remainingText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (dashboardState.isOverBudget) extended.dangerText else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (dashboardState.isOverBudget) "$spentPercent% limit" else "$spentPercent% used",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = progressRingColor
                    )
                }
            }

            // Quick Breakdown Metrics
            Column(
                modifier = Modifier.padding(start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Income Target
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(extended.successTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TrendingUp,
                            contentDescription = null,
                            tint = extended.incomeLine,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Expected Income", style = MaterialTheme.typography.labelSmall, color = extended.textMuted)
                        Text(
                            text = Formatters.formatPesewas(dashboardState.monthlyIncomeLimitPesewas, hideAmounts),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Expense Limit Cap
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(extended.primaryTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TrendingDown,
                            contentDescription = null,
                            tint = extended.onPrimaryTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Expense Limit", style = MaterialTheme.typography.labelSmall, color = extended.textMuted)
                        Text(
                            text = Formatters.formatPesewas(dashboardState.monthlyExpenseLimitPesewas, hideAmounts),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Total Spent
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(extended.surfaceAlt),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛒", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Actual Spent", style = MaterialTheme.typography.labelSmall, color = extended.textMuted)
                        Text(
                            text = Formatters.formatPesewas(dashboardState.totalSpentPesewas, hideAmounts),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = extended.border.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(10.dp))

        // 3. Bottom Health Banner: Safe to spend today
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dailyAllowance = if (dashboardState.daysLeftInMonth > 0)
                dashboardState.remainingBudgetPesewas / dashboardState.daysLeftInMonth
            else 0L

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛡️", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Daily Allowance: ${Formatters.formatPesewas(dailyAllowance, hideAmounts)} / day",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "${dashboardState.daysLeftInMonth} days left",
                style = MaterialTheme.typography.bodySmall,
                color = extended.textMuted
            )
        }

        if (dashboardState.monthlyTrends.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = extended.border.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(14.dp))

            // 4. 6-Month Spending & Income Trend Line Chart (Recharts-style)
            SixMonthTrendLineChart(
                dataPoints = dashboardState.monthlyTrends,
                hideAmounts = hideAmounts
            )
        }
    }

    // Set Monthly Limits Dialog
    if (showLimitsDialog) {
        SetBudgetLimitsDialog(
            currentIncomeLimitPesewas = dashboardState.monthlyIncomeLimitPesewas,
            currentExpenseLimitPesewas = dashboardState.monthlyExpenseLimitPesewas,
            onDismiss = { showLimitsDialog = false },
            onConfirm = { newIncomePesewas, newExpensePesewas ->
                onSetLimits(newIncomePesewas, newExpensePesewas)
                showLimitsDialog = false
            }
        )
    }
}

/**
 * Modal dialog for setting monthly income and expense limits.
 */
@Composable
fun SetBudgetLimitsDialog(
    currentIncomeLimitPesewas: Long,
    currentExpenseLimitPesewas: Long,
    onDismiss: () -> Unit,
    onConfirm: (incomePesewas: Long, expensePesewas: Long) -> Unit
) {
    val extended = LocalBemoColors.current

    var incomeInput by remember {
        mutableStateOf((currentIncomeLimitPesewas / 100.0).let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() })
    }
    var expenseInput by remember {
        mutableStateOf((currentExpenseLimitPesewas / 100.0).let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() })
    }

    val evaluatedIncome = remember(incomeInput) {
        Formatters.evaluateExpressionToPesewas(incomeInput) ?: currentIncomeLimitPesewas
    }
    val evaluatedExpense = remember(expenseInput) {
        Formatters.evaluateExpressionToPesewas(expenseInput) ?: currentExpenseLimitPesewas
    }

    val projectedSavings = evaluatedIncome - evaluatedExpense

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Monthly Budget Limits",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Establish your expected income and the maximum spending cap for this month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textMuted
                )

                // Income Limit Input
                OutlinedTextField(
                    value = incomeInput,
                    onValueChange = { incomeInput = it },
                    label = { Text("Monthly Expected Income") },
                    prefix = { Text("₵ ") },
                    supportingText = {
                        Text("Formatted: ${Formatters.formatPesewas(evaluatedIncome)}")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_income_limit")
                )

                // Expense Limit Input
                OutlinedTextField(
                    value = expenseInput,
                    onValueChange = { expenseInput = it },
                    label = { Text("Monthly Expense Limit (Cap)") },
                    prefix = { Text("₵ ") },
                    supportingText = {
                        Text("Formatted: ${Formatters.formatPesewas(evaluatedExpense)}")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_limit")
                )

                // Projected Monthly Savings Banner
                Surface(
                    color = if (projectedSavings >= 0) extended.successTint else extended.dangerTint,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (projectedSavings >= 0) "💰" else "⚠️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (projectedSavings >= 0) "Projected Savings" else "Deficit Warning",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (projectedSavings >= 0) extended.successText else extended.dangerText
                            )
                            Text(
                                text = if (projectedSavings >= 0)
                                    "${Formatters.formatPesewas(projectedSavings)} unallocated for goals"
                                else
                                    "Expenses exceed expected income by ${Formatters.formatPesewas(-projectedSavings)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (projectedSavings >= 0) extended.successText else extended.dangerText
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (evaluatedIncome > 0 && evaluatedExpense > 0) {
                        onConfirm(evaluatedIncome, evaluatedExpense)
                    }
                },
                modifier = Modifier.testTag("confirm_set_limits_button")
            ) {
                Text("Save Limits")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
