package com.example.bemo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bemo.model.Formatters
import com.example.bemo.ui.CategorySpendProgress
import com.example.ui.theme.LocalBemoColors

@Composable
fun CategoryDonutChart(
    items: List<CategorySpendProgress>,
    totalSpentPesewas: Long,
    hideAmounts: Boolean = false,
    modifier: Modifier = Modifier
) {
    val extended = LocalBemoColors.current
    val palette = listOf(
        Color(0xFFC84E26), // Primary Terracotta
        Color(0xFF5FCB8F), // Success Mint
        Color(0xFF1565C0), // Transport Blue
        Color(0xFFF2C94C), // Utilities Gold
        Color(0xFF9C27B0), // Data Purple
        Color(0xFFD85A30), // Dining Coral
        Color(0xFFE91E63), // Family Pink
        Color(0xFF009688)  // Personal Teal
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Donut Canvas
        Box(
            modifier = Modifier
                .size(140.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 24.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2)
                val arcSize = Size(radius * 2, radius * 2)

                // Background track
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.25f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                if (totalSpentPesewas > 0) {
                    var currentStartAngle = -90f
                    items.forEachIndexed { index, item ->
                        val sweep = (item.spentPesewas.toFloat() / totalSpentPesewas.toFloat()) * 360f
                        if (sweep > 0f) {
                            val sliceColor = palette[index % palette.size]
                            drawArc(
                                color = sliceColor,
                                startAngle = currentStartAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            currentStartAngle += sweep
                        }
                    }
                }
            }

            // Center total
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.textMuted
                )
                Text(
                    text = Formatters.formatPesewasShort(totalSpentPesewas, hideAmounts),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Legend list
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val topFour = items.take(4)
            topFour.forEachIndexed { index, item ->
                val sliceColor = palette[index % palette.size]
                val percent = if (totalSpentPesewas > 0) (item.spentPesewas * 100 / totalSpentPesewas) else 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(sliceColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.category.emoji} ${item.category.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = extended.textMuted
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyComparisonBarChart(
    incomePesewas: Long,
    spentPesewas: Long,
    savedPesewas: Long,
    hideAmounts: Boolean = false,
    modifier: Modifier = Modifier
) {
    val extended = LocalBemoColors.current
    val maxVal = maxOf(incomePesewas, spentPesewas, savedPesewas, 1L).toFloat()

    val incomeFraction = (incomePesewas.toFloat() / maxVal).coerceIn(0.05f, 1f)
    val spentFraction = (spentPesewas.toFloat() / maxVal).coerceIn(0.05f, 1f)
    val savedFraction = (savedPesewas.toFloat() / maxVal).coerceIn(0.05f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // Income bar
            ComparisonBarItem(
                label = "Income",
                amountText = Formatters.formatPesewasShort(incomePesewas, hideAmounts),
                fraction = incomeFraction,
                color = extended.incomeLine
            )

            // Spent bar
            ComparisonBarItem(
                label = "Spent",
                amountText = Formatters.formatPesewasShort(spentPesewas, hideAmounts),
                fraction = spentFraction,
                color = extended.expenseLine
            )

            // Saved bar
            ComparisonBarItem(
                label = "Saved",
                amountText = Formatters.formatPesewasShort(savedPesewas, hideAmounts),
                fraction = savedFraction,
                color = extended.success
            )
        }
    }
}

@Composable
private fun ComparisonBarItem(
    label: String,
    amountText: String,
    fraction: Float,
    color: Color
) {
    val extended = LocalBemoColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(76.dp)
    ) {
        Text(
            text = amountText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height((80 * fraction).dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = extended.textMuted
        )
    }
}
