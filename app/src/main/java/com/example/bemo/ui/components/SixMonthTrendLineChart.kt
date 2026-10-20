package com.example.bemo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bemo.model.Formatters
import com.example.ui.theme.LocalBemoColors

data class MonthTrendPoint(
    val monthLabel: String,
    val fullMonthName: String,
    val incomePesewas: Long,
    val spentPesewas: Long
)

/**
 * Recharts-style interactive 6-Month Income & Spending Trend Line Chart.
 * Features smooth Bézier curves, soft gradient fill washes, milestone data points,
 * grid guide lines, and interactive month selection with detailed tooltip inspection.
 */
@Composable
fun SixMonthTrendLineChart(
    dataPoints: List<MonthTrendPoint>,
    hideAmounts: Boolean = false,
    modifier: Modifier = Modifier
) {
    val extended = LocalBemoColors.current
    var selectedMonthIndex by remember { mutableStateOf(dataPoints.size - 1) }

    val incomeColor = extended.incomeLine
    val expenseColor = extended.expenseLine

    val selectedPoint = dataPoints.getOrNull(selectedMonthIndex)

    val maxVal = remember(dataPoints) {
        val highest = dataPoints.maxOfOrNull { maxOf(it.incomePesewas, it.spentPesewas) } ?: 500000L
        // Round up to nearest 100,000 pesewas (1,000 Cedis)
        (((highest / 100000L) + 1) * 100000L).coerceAtLeast(400000L).toFloat()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("six_month_trend_chart")
    ) {
        // Chart Header & Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "6-Month Cash Flow Trend",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Legend Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Income Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(incomeColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = extended.textMuted
                    )
                }

                // Expense Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(expenseColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Spending",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = extended.textMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Tooltip Card for Selected Month
        selectedPoint?.let { pt ->
            val netPesewas = pt.incomePesewas - pt.spentPesewas
            val isNetPositive = netPesewas >= 0

            Surface(
                color = extended.surfaceAlt,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = pt.fullMonthName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap any month column to inspect",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textDisabled,
                            fontSize = 10.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Income", style = MaterialTheme.typography.labelSmall, color = incomeColor, fontSize = 10.sp)
                            Text(
                                text = Formatters.formatPesewas(pt.incomePesewas, hideAmounts),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = incomeColor
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Spent", style = MaterialTheme.typography.labelSmall, color = expenseColor, fontSize = 10.sp)
                            Text(
                                text = Formatters.formatPesewas(pt.spentPesewas, hideAmounts),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = expenseColor
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Net Saved", style = MaterialTheme.typography.labelSmall, color = extended.textMuted, fontSize = 10.sp)
                            Text(
                                text = (if (isNetPositive) "+" else "") + Formatters.formatPesewas(netPesewas, hideAmounts),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isNetPositive) extended.successText else extended.dangerText
                            )
                        }
                    }
                }
            }
        }

        // Line Chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(dataPoints) {
                        detectTapGestures { offset ->
                            val sectionWidth = size.width / dataPoints.size
                            val index = (offset.x / sectionWidth).toInt().coerceIn(0, dataPoints.size - 1)
                            selectedMonthIndex = index
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val paddingBottom = 20.dp.toPx()
                val usableHeight = height - paddingBottom
                val count = dataPoints.size

                val stepX = width / (count - 1).coerceAtLeast(1)

                // 1. Draw horizontal reference grid lines (0%, 50%, 100%)
                val gridAlpha = 0.2f
                val gridColor = Color.Gray.copy(alpha = gridAlpha)

                drawLine(
                    color = gridColor,
                    start = Offset(0f, usableHeight * 0.1f),
                    end = Offset(width, usableHeight * 0.1f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, usableHeight * 0.55f),
                    end = Offset(width, usableHeight * 0.55f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, usableHeight),
                    end = Offset(width, usableHeight),
                    strokeWidth = 1.dp.toPx()
                )

                // 2. Prepare Point coordinates
                val incomePoints = dataPoints.mapIndexed { i, pt ->
                    val x = i * stepX
                    val y = usableHeight - (pt.incomePesewas.toFloat() / maxVal * usableHeight)
                    Offset(x, y.coerceIn(8.dp.toPx(), usableHeight))
                }

                val expensePoints = dataPoints.mapIndexed { i, pt ->
                    val x = i * stepX
                    val y = usableHeight - (pt.spentPesewas.toFloat() / maxVal * usableHeight)
                    Offset(x, y.coerceIn(8.dp.toPx(), usableHeight))
                }

                // 3. Draw Gradient Area Fill for Income
                if (incomePoints.isNotEmpty()) {
                    val incomeAreaPath = Path().apply {
                        moveTo(incomePoints.first().x, usableHeight)
                        lineTo(incomePoints.first().x, incomePoints.first().y)
                        for (i in 0 until incomePoints.size - 1) {
                            val p0 = incomePoints[i]
                            val p1 = incomePoints[i + 1]
                            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2, p0.y)
                            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2, p1.y)
                            cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                        }
                        lineTo(incomePoints.last().x, usableHeight)
                        close()
                    }

                    drawPath(
                        path = incomeAreaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(incomeColor.copy(alpha = 0.20f), Color.Transparent),
                            startY = 0f,
                            endY = usableHeight
                        )
                    )
                }

                // 4. Draw Gradient Area Fill for Expense
                if (expensePoints.isNotEmpty()) {
                    val expenseAreaPath = Path().apply {
                        moveTo(expensePoints.first().x, usableHeight)
                        lineTo(expensePoints.first().x, expensePoints.first().y)
                        for (i in 0 until expensePoints.size - 1) {
                            val p0 = expensePoints[i]
                            val p1 = expensePoints[i + 1]
                            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2, p0.y)
                            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2, p1.y)
                            cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                        }
                        lineTo(expensePoints.last().x, usableHeight)
                        close()
                    }

                    drawPath(
                        path = expenseAreaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(expenseColor.copy(alpha = 0.16f), Color.Transparent),
                            startY = 0f,
                            endY = usableHeight
                        )
                    )
                }

                // 5. Draw Smooth Bézier Curve Lines
                fun drawSmoothCurve(points: List<Offset>, color: Color) {
                    if (points.size < 2) return
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2, p0.y)
                            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2, p1.y)
                            cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                drawSmoothCurve(incomePoints, incomeColor)
                drawSmoothCurve(expensePoints, expenseColor)

                // 6. Draw Selected Column Highlight Bar
                if (selectedMonthIndex in 0 until count) {
                    val selX = selectedMonthIndex * stepX
                    drawLine(
                        color = extended.border.copy(alpha = 0.8f),
                        start = Offset(selX, 0f),
                        end = Offset(selX, usableHeight),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                // 7. Draw Circular Node Markers
                incomePoints.forEachIndexed { idx, pt ->
                    val isSelected = idx == selectedMonthIndex
                    val nodeRadius = if (isSelected) 6.dp.toPx() else 4.dp.toPx()
                    drawCircle(color = Color.White, radius = nodeRadius, center = pt)
                    drawCircle(
                        color = incomeColor,
                        radius = nodeRadius,
                        center = pt,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                expensePoints.forEachIndexed { idx, pt ->
                    val isSelected = idx == selectedMonthIndex
                    val nodeRadius = if (isSelected) 6.dp.toPx() else 4.dp.toPx()
                    drawCircle(color = Color.White, radius = nodeRadius, center = pt)
                    drawCircle(
                        color = expenseColor,
                        radius = nodeRadius,
                        center = pt,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // X-Axis Month Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dataPoints.forEachIndexed { index, pt ->
                val isSelected = index == selectedMonthIndex
                Text(
                    text = pt.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else extended.textMuted,
                    modifier = Modifier
                        .clickable { selectedMonthIndex = index }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                )
            }
        }
    }
}
