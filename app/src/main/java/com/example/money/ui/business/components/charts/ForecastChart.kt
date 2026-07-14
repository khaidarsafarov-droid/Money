package com.example.money.ui.business.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.business.CashflowForecastPoint
import com.example.money.ui.business.components.cards.BusinessCard
import com.example.money.ui.business.theme.BusinessColors
import com.example.money.ui.business.theme.BusinessSpacing

@Composable
fun ForecastChart(
    points: List<CashflowForecastPoint>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return
    BusinessCard(modifier = modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.business_ai_forecast_title),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleLarge,
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(top = BusinessSpacing.sm),
        ) {
            val values = points.map { it.projectedBalanceCents }
            val max = values.max().coerceAtLeast(1).toFloat()
            val min = values.min().toFloat()
            val range = (max - min).coerceAtLeast(1f)
            val stepX = size.width / (values.size - 1).coerceAtLeast(1)
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = index * stepX
                val y = size.height - ((value - min) / range) * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, BusinessColors.PrimaryBlue, style = Stroke(width = 3f))
            drawLine(
                color = BusinessColors.Gray300,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1f,
            )
        }
        Text(
            points.joinToString(" · ") { it.label },
            style = MaterialTheme.typography.bodySmall,
            color = BusinessColors.Gray500,
            modifier = Modifier.padding(top = BusinessSpacing.xs),
        )
    }
}
