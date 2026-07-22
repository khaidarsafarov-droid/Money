package com.example.money.ui.debtors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.theme.FinanceNegative
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.util.Formatters
import java.util.Calendar

@Composable
fun DebtCalendarTab(
    debtors: List<Debtor>,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
) {
    val activeDebts = debtors.filter { !it.isPaid }
    var monthAnchor by remember { mutableStateOf(startOfMonth(System.currentTimeMillis())) }

    val monthLabel = Formatters.monthYear(monthAnchor)
    val days = remember(monthAnchor) { buildMonthDays(monthAnchor) }
    var selectedDay by remember(monthAnchor, activeDebts) {
        mutableStateOf(days.firstOrNull { dayHasDebts(activeDebts, it) } ?: monthAnchor)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val prevMonthCd = stringResource(R.string.calendar_prev_month)
            val nextMonthCd = stringResource(R.string.calendar_next_month)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "◀",
                    modifier = Modifier
                        .semantics { contentDescription = prevMonthCd }
                        .clickable { monthAnchor = shiftMonth(monthAnchor, -1) },
                )
                Text(monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "▶",
                    modifier = Modifier
                        .semantics { contentDescription = nextMonthCd }
                        .clickable { monthAnchor = shiftMonth(monthAnchor, 1) },
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    R.string.calendar_weekday_mon,
                    R.string.calendar_weekday_tue,
                    R.string.calendar_weekday_wed,
                    R.string.calendar_weekday_thu,
                    R.string.calendar_weekday_fri,
                    R.string.calendar_weekday_sat,
                    R.string.calendar_weekday_sun,
                ).forEach { labelRes ->
                    Text(
                        stringResource(labelRes),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            days.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { dayMillis ->
                        val inMonth = isSameMonth(dayMillis, monthAnchor)
                        val hasDebts = dayHasDebts(activeDebts, dayMillis)
                        val dayNumber = Calendar.getInstance().apply { timeInMillis = dayMillis }
                            .get(Calendar.DAY_OF_MONTH)
                        val dayCd = stringResource(
                            if (hasDebts) R.string.calendar_day_with_debts else R.string.calendar_day_empty_cd,
                            dayNumber,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        dayMillis == selectedDay -> GoldPrimary.copy(alpha = 0.2f)
                                        hasDebts -> FinancePositive.copy(alpha = 0.15f)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                )
                                .semantics { contentDescription = dayCd }
                                .clickable(enabled = inMonth) {
                                    if (inMonth) selectedDay = dayMillis
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (inMonth) dayNumber.toString() else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (hasDebts) FontWeight.Bold else FontWeight.Normal,
                                )
                                if (hasDebts && inMonth) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(FinanceNegative)
                                            .padding(2.dp),
                                    )
                                }
                            }
                        }
                    }
                    repeat(7 - week.size) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }

            val dayDebts = activeDebts.filter { isSameDay(it.dateDue, selectedDay) }
            Text(stringResource(R.string.calendar_day_title, Formatters.shortDate(selectedDay)), fontWeight = FontWeight.SemiBold)
            if (dayDebts.isEmpty()) {
                Text(stringResource(R.string.calendar_day_empty), style = MaterialTheme.typography.bodySmall)
            } else {
                dayDebts.forEach { debtor ->
                    val color = if (debtor.debtType == DebtType.CREDITOR) FinancePositive else FinanceNegative
                    Text(
                        text = "• ${debtor.name} · ${Formatters.currencyFromCents(debtor.remainingAmountCents, debtor.currency, hide = hideAmounts)}",
                        color = color,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun startOfMonth(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun shiftMonth(anchor: Long, delta: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = anchor
        add(Calendar.MONTH, delta)
    }
    return startOfMonth(cal.timeInMillis)
}

private fun buildMonthDays(anchor: Long): List<Long> {
    val first = Calendar.getInstance().apply {
        timeInMillis = anchor
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val startOffset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val result = mutableListOf<Long>()
    repeat(startOffset) { result.add(0L) }
    for (day in 1..daysInMonth) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = anchor
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        result.add(cal.timeInMillis)
    }
    return result
}

private fun isSameMonth(a: Long, b: Long): Boolean {
    if (a == 0L) return false
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
        ca.get(Calendar.MONTH) == cb.get(Calendar.MONTH)
}

private fun isSameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
        ca.get(Calendar.MONTH) == cb.get(Calendar.MONTH) &&
        ca.get(Calendar.DAY_OF_MONTH) == cb.get(Calendar.DAY_OF_MONTH)
}

private fun dayHasDebts(debtors: List<Debtor>, dayMillis: Long): Boolean =
    dayMillis != 0L && debtors.any { isSameDay(it.dateDue, dayMillis) }
