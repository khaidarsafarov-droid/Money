package com.example.money.ui.business.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.ui.business.components.charts.Sparkline
import com.example.money.ui.business.theme.BusinessColors
import com.example.money.ui.business.theme.BusinessKpiType
import com.example.money.ui.business.theme.BusinessSpacing
import com.example.money.ui.business.theme.accentColor
import com.example.money.ui.util.Formatters

@Composable
fun BusinessKpiCard(
    title: String,
    amountCents: Long,
    type: BusinessKpiType,
    changePercent: Int?,
    sparklineValues: List<Long>,
    modifier: Modifier = Modifier,
) {
    val accent = type.accentColor()
    BusinessCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = BusinessColors.OnSurfaceMuted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    Formatters.currencyFromCents(amountCents),
                    style = MaterialTheme.typography.headlineMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
                changePercent?.let { change ->
                    val sign = if (change >= 0) "+" else ""
                    val changeColor = if (change >= 0) BusinessColors.ProfitGreen else BusinessColors.LossRed
                    Text("$sign$change%", color = changeColor, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (sparklineValues.size >= 2) {
                Sparkline(
                    values = sparklineValues,
                    lineColor = accent,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                )
            }
        }
    }
}
