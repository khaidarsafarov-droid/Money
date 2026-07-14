package com.example.money.ui.business.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.domain.model.business.CategoryDistributionRow
import com.example.money.ui.business.components.cards.BusinessCard
import com.example.money.ui.business.theme.BusinessColors
import com.example.money.ui.business.theme.BusinessSpacing

@Composable
fun Sparkline(
    values: List<Long>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val max = values.max().coerceAtLeast(1).toFloat()
        val min = values.min().toFloat()
        val range = (max - min).coerceAtLeast(1f)
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - min) / range) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 3f))
    }
}

@Composable
fun CashflowLineChart(
    incomeValues: List<Long>,
    expenseValues: List<Long>,
    labels: List<String>,
    title: String,
    modifier: Modifier = Modifier,
) {
    BusinessCard(modifier = modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
        DualLineChart(
            primary = incomeValues,
            secondary = expenseValues,
            primaryColor = BusinessColors.ProfitGreen,
            secondaryColor = BusinessColors.LossRed,
            modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = BusinessSpacing.sm),
        )
        if (labels.isNotEmpty()) {
            Text(labels.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = BusinessColors.Gray500)
        }
    }
}

@Composable
fun ProfitBarChart(
    values: List<Long>,
    labels: List<String>,
    title: String,
    modifier: Modifier = Modifier,
) {
    BusinessCard(modifier = modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
        BarChart(
            values = values,
            positiveColor = BusinessColors.PrimaryBlue,
            negativeColor = BusinessColors.LossRed,
            modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = BusinessSpacing.sm),
        )
        if (labels.isNotEmpty()) {
            Text(labels.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = BusinessColors.Gray500)
        }
    }
}

@Composable
fun CategoryDistributionChart(
    rows: List<CategoryDistributionRow>,
    title: String,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return
    BusinessCard(modifier = modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
        HorizontalStackedBarChart(
            rows = rows,
            incomeColor = BusinessColors.ProfitGreen,
            expenseColor = BusinessColors.LossRed,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = BusinessSpacing.sm),
        )
    }
}

@Composable
private fun HorizontalStackedBarChart(
    rows: List<CategoryDistributionRow>,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier,
) {
    val maxTotal = rows.maxOf { it.totalCents }.coerceAtLeast(1).toFloat()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
        rows.forEach { row ->
            Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.xs)) {
                Text(row.category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                ) {
                    val incomeWidth = (row.incomeCents / maxTotal) * size.width
                    val expenseWidth = (row.expenseCents / maxTotal) * size.width
                    if (incomeWidth > 0) {
                        drawRect(
                            color = incomeColor,
                            topLeft = Offset.Zero,
                            size = androidx.compose.ui.geometry.Size(incomeWidth, size.height),
                        )
                    }
                    if (expenseWidth > 0) {
                        drawRect(
                            color = expenseColor,
                            topLeft = Offset(incomeWidth, 0f),
                            size = androidx.compose.ui.geometry.Size(expenseWidth, size.height),
                        )
                    }
                }
            }
        }
        Text(
            "● Income  ● Expense",
            style = MaterialTheme.typography.labelSmall,
            color = BusinessColors.Gray500,
        )
    }
}

@Composable
private fun DualLineChart(
    primary: List<Long>,
    secondary: List<Long>,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val count = maxOf(primary.size, secondary.size)
        if (count < 2) return@Canvas
        val all = primary + secondary
        val max = all.max().coerceAtLeast(1).toFloat()
        val stepX = size.width / (count - 1)

        fun buildPath(values: List<Long>): Path {
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = index * stepX
                val y = size.height - (value / max) * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }

        drawPath(buildPath(primary), primaryColor, style = Stroke(width = 3f))
        drawPath(buildPath(secondary), secondaryColor, style = Stroke(width = 3f))
    }
}

@Composable
private fun BarChart(
    values: List<Long>,
    positiveColor: Color,
    negativeColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val max = values.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1).toFloat()
        val barWidth = size.width / values.size * 0.6f
        val gap = size.width / values.size
        values.forEachIndexed { index, value ->
            val barHeight = (kotlin.math.abs(value) / max) * size.height
            val left = index * gap + (gap - barWidth) / 2
            val top = size.height - barHeight
            drawRect(
                color = if (value >= 0) positiveColor else negativeColor,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight.coerceAtLeast(2f)),
            )
        }
    }
}
