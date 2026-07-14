package com.example.money.ui.business.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
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
    icon: ImageVector? = null,
    subtitle: String? = null,
) {
    val accent = type.accentColor()
    BusinessCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = BusinessColors.OnSurfaceMuted)
                icon?.let { Icon(imageVector = it, contentDescription = null, tint = accent) }
            }
            Text(
                Formatters.currencyFromCents(amountCents),
                style = MaterialTheme.typography.headlineMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            when {
                subtitle != null -> Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BusinessColors.OnSurfaceMuted)
                changePercent != null -> {
                    val sign = if (changePercent >= 0) "+" else ""
                    Text(
                        stringResource(R.string.business_kpi_vs_last_month, "$sign$changePercent%"),
                        style = MaterialTheme.typography.bodySmall,
                        color = BusinessColors.OnSurfaceMuted,
                    )
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
