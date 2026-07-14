package com.example.money.ui.business.components.tables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.money.ui.business.components.cards.BusinessCard
import com.example.money.ui.business.theme.BusinessColors
import com.example.money.ui.business.theme.BusinessSpacing

data class BusinessTableColumn(
    val header: String,
    val weight: Float = 1f,
)

@Composable
fun BusinessTable(
    title: String,
    columns: List<BusinessTableColumn>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return
    BusinessCard(modifier = modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = BusinessSpacing.md, bottom = BusinessSpacing.sm),
        ) {
            columns.forEach { col ->
                Text(
                    col.header,
                    modifier = Modifier.weight(col.weight),
                    style = MaterialTheme.typography.labelMedium,
                    color = BusinessColors.Gray500,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        HorizontalDivider(color = BusinessColors.Gray300)
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = BusinessSpacing.sm),
            ) {
                row.forEachIndexed { colIndex, cell ->
                    val weight = columns.getOrNull(colIndex)?.weight ?: 1f
                    Text(
                        cell,
                        modifier = Modifier.weight(weight),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (index < rows.lastIndex) {
                HorizontalDivider(color = BusinessColors.Gray100)
            }
        }
    }
}
