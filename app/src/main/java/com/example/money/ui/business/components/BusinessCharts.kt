package com.example.money.ui.business.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.money.ui.util.Formatters

@Composable
fun BusinessKpiCard(
    title: String,
    amountCents: Long,
    accentColor: Color,
    changePercent: Int?,
    sparklineValues: List<Long>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    Formatters.currencyFromCents(amountCents),
                    style = MaterialTheme.typography.headlineSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                )
                changePercent?.let { change ->
                    val sign = if (change >= 0) "+" else ""
                    Text(
                        "$sign$change%",
                        color = if (change >= 0) accentColor else Color(0xFFB35D5D),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            if (sparklineValues.size >= 2) {
                Sparkline(
                    values = sparklineValues,
                    lineColor = accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                )
            }
        }
    }
}

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
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Cashflow", fontWeight = FontWeight.SemiBold)
            DualLineChart(
                primary = incomeValues,
                secondary = expenseValues,
                primaryColor = Color(0xFF4A7C59),
                secondaryColor = Color(0xFFB35D5D),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(top = 8.dp),
            )
            if (labels.isNotEmpty()) {
                Text(
                    labels.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
fun ProfitBarChart(
    values: List<Long>,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("P&L", fontWeight = FontWeight.SemiBold)
            BarChart(
                values = values,
                barColor = Color(0xFF4A6FA5),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(top = 8.dp),
            )
            if (labels.isNotEmpty()) {
                Text(
                    labels.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
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
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val max = values.max().coerceAtLeast(1).toFloat()
        val barWidth = size.width / values.size * 0.6f
        val gap = size.width / values.size
        values.forEachIndexed { index, value ->
            val barHeight = (value / max) * size.height
            val left = index * gap + (gap - barWidth) / 2
            val top = size.height - barHeight
            drawRect(
                color = if (value >= 0) barColor else Color(0xFFB35D5D),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight.coerceAtLeast(2f)),
            )
        }
    }
}
