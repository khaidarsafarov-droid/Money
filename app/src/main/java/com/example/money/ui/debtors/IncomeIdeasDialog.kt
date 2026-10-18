@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.money.ui.debtors

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.ui.util.Formatters

private data class IncomeResource(
    val title: String,
    val description: String,
    val url: String,
)

private val INCOME_RESOURCES = listOf(
    IncomeResource("Upwork", "Фриланс: дизайн, разработка, тексты", "https://www.upwork.com"),
    IncomeResource("Fiverr", "Небольшие заказы и микроуслуги", "https://www.fiverr.com"),
    IncomeResource("Prolific", "Оплачиваемые исследования и опросы", "https://www.prolific.com"),
    IncomeResource("Kwork", "Русскоязычная биржа микрозадач", "https://kwork.ru"),
)

@Composable
fun IncomeIdeasDialog(
    debtors: List<Debtor>,
    hideAmounts: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val totalRemaining = debtors
        .filter { it.debtType == DebtType.DEBTOR && !it.isPaid }
        .sumOf { it.remainingAmountCents }
    val monthsToAccelerate = 12
    val extraNeeded = if (totalRemaining > 0) {
        (totalRemaining + monthsToAccelerate - 1) / monthsToAccelerate
    } else {
        0L
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.income_ideas_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (extraNeeded > 0) {
                    Text(
                        text = stringResource(
                            R.string.income_calculator_hint,
                            Formatters.currencyFromCents(extraNeeded, hide = hideAmounts),
                            monthsToAccelerate,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.income_ideas_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
                INCOME_RESOURCES.forEach { resource ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resource.url)))
                        },
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = resource.title,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = resource.description,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}
