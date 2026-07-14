package com.example.money.ui.business.components.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.business.BusinessAttentionAlert
import com.example.money.domain.model.business.BusinessAttentionSeverity
import com.example.money.domain.model.business.BusinessAttentionType
import com.example.money.ui.business.components.cards.BusinessCard
import com.example.money.ui.business.theme.BusinessColors
import com.example.money.ui.business.theme.BusinessSpacing
import com.example.money.ui.util.Formatters

@Composable
fun AttentionCenterWidget(
    alerts: List<BusinessAttentionAlert>,
    modifier: Modifier = Modifier,
) {
    if (alerts.isEmpty()) return
    BusinessCard(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.NotificationsActive,
                contentDescription = null,
                tint = BusinessColors.WarningYellow,
            )
            Text(
                stringResource(R.string.business_attention_title),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
            modifier = Modifier.padding(top = BusinessSpacing.sm),
        ) {
            alerts.forEach { alert ->
                AttentionAlertRow(alert = alert)
            }
        }
    }
}

@Composable
private fun AttentionAlertRow(alert: BusinessAttentionAlert) {
    val (icon, tint, background) = when (alert.severity) {
        BusinessAttentionSeverity.INFO -> Triple(
            Icons.Outlined.TrendingDown,
            BusinessColors.PrimaryBlue,
            BusinessColors.Gray100,
        )
        BusinessAttentionSeverity.WARNING -> Triple(
            Icons.Outlined.ReceiptLong,
            BusinessColors.WarningYellow,
            BusinessColors.WarningYellow.copy(alpha = 0.15f),
        )
        BusinessAttentionSeverity.CRITICAL -> Triple(
            Icons.Outlined.AccountBalanceWallet,
            BusinessColors.LossRed,
            BusinessColors.LossRed.copy(alpha = 0.12f),
        )
    }
    val message = formatAlertMessage(alert)

    Surface(
        color = background,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(BusinessSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun formatAlertMessage(alert: BusinessAttentionAlert): String = when (alert.type) {
    BusinessAttentionType.OVERDUE_INVOICE -> stringResource(
        R.string.business_alert_overdue_invoice,
        alert.clientName ?: "—",
        alert.daysOverdue ?: 0L,
        Formatters.currencyFromCents(alert.amountCents ?: 0),
    )
    BusinessAttentionType.LOW_CASH -> stringResource(
        R.string.business_alert_low_cash,
        Formatters.currencyFromCents(alert.amountCents ?: 0),
    )
    BusinessAttentionType.LARGE_EXPENSE -> stringResource(
        R.string.business_alert_large_expense,
        alert.category ?: "—",
        Formatters.currencyFromCents(alert.amountCents ?: 0),
    )
}
