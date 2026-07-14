package com.example.money.ui.business.components.ai

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.money.R
import com.example.money.domain.model.business.InvoiceRiskAssessment
import com.example.money.domain.model.business.InvoiceRiskLevel
import com.example.money.ui.business.theme.BusinessColors

@Composable
fun InvoiceRiskBadge(
    assessment: InvoiceRiskAssessment?,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    if (assessment == null) return
    val (label, color) = when (assessment.riskLevel) {
        InvoiceRiskLevel.LOW -> stringResource(R.string.business_risk_low) to BusinessColors.ProfitGreen
        InvoiceRiskLevel.MEDIUM -> stringResource(R.string.business_risk_medium) to BusinessColors.WarningYellow
        InvoiceRiskLevel.HIGH -> stringResource(R.string.business_risk_high) to BusinessColors.LossRed
    }
    AssistChip(
        onClick = {},
        modifier = modifier,
        label = { Text("$label ${assessment.probabilityPercent}%") },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = color,
        ),
    )
}
