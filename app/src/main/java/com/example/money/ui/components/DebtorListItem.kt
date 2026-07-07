package com.example.money.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.ui.debtors.DebtDueStatus
import com.example.money.ui.debtors.dueStatus
import com.example.money.ui.debtors.initials
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.theme.OverdueRed
import com.example.money.ui.theme.UrgentYellowBg
import com.example.money.ui.theme.UrgentYellowText
import com.example.money.ui.util.Formatters

@Composable
fun DebtorListItem(
    debtor: Debtor,
    onRemind: () -> Unit,
    onAddPayment: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAccelerate: (() -> Unit)? = null,
    onCalendar: (() -> Unit)? = null,
    calendarSynced: Boolean = false,
    hideAmounts: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val dueStatus = debtor.dueStatus()
    val isOverdue = dueStatus == DebtDueStatus.OVERDUE
    val showRemind = debtor.debtType == DebtType.CREDITOR && !debtor.isPaid
    val milestones = com.example.money.domain.util.DebtFreedomEngine.computeMilestones(
        currentCents = debtor.currentAmountCents,
        targetCents = debtor.targetAmountCents,
    )
    val accelerateCents = com.example.money.domain.util.DebtFreedomEngine.suggestAcceleratePaymentCents(
        remainingCents = debtor.remainingAmountCents,
        targetCents = debtor.targetAmountCents,
        currentCents = debtor.currentAmountCents,
    )
    val paymentsToClose = com.example.money.domain.util.DebtFreedomEngine.estimatePaymentsToClose(
        remainingCents = debtor.remainingAmountCents,
        paymentCents = accelerateCents,
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isOverdue) {
                    Modifier.border(1.5.dp, OverdueRed.copy(alpha = 0.45f), MaterialTheme.shapes.large)
                } else {
                    Modifier
                },
            ),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DebtorAvatar(name = debtor.name)

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = debtor.name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        DebtDisplayStatusChip(dueStatus = dueStatus)
                        if (debtor.isHighPriorityDebt && !debtor.isPaid) {
                            Text(
                                text = stringResource(R.string.loan_priority_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = OverdueRed,
                                modifier = Modifier
                                    .background(OverdueRed.copy(alpha = 0.1f), MaterialTheme.shapes.small)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        if (!debtor.isPaid && onAccelerate != null) {
                            TextButton(
                                onClick = onAccelerate,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.debt_accelerate),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoldPrimary,
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(
                            R.string.debtor_target_amount,
                            Formatters.currencyFromCents(debtor.targetAmountCents, debtor.currency, hide = hideAmounts),
                        ),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(
                            if (debtor.debtType == DebtType.CREDITOR) {
                                R.string.debtor_given_date
                            } else {
                                R.string.debt_received_date
                            },
                            Formatters.shortDate(debtor.dateGiven),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DebtDueStatusLabel(status = dueStatus, dateDue = debtor.dateDue)
                }
            }

            if (!debtor.isPaid) {
                LinearProgressIndicator(
                    progress = { debtor.progressFraction },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isOverdue) OverdueRed else GoldPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.debt_journey_progress,
                        milestones.progressPercent,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                milestones.nextMilestone?.let { milestone ->
                    Text(
                        text = stringResource(
                            R.string.debt_next_milestone,
                            milestone.percent,
                            Formatters.currencyFromCents(milestone.centsToReach, debtor.currency, hide = hideAmounts),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (com.example.money.domain.util.DebtFreedomEngine.isSmallDebt(debtor.remainingAmountCents)) {
                    Text(
                        text = stringResource(
                            R.string.debt_small_win_hint,
                            paymentsToClose,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = FinancePositive,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.debtor_remaining,
                        Formatters.currencyFromCents(debtor.remainingAmountCents, debtor.currency, hide = hideAmounts),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(
                        R.string.debtor_paid_progress,
                        Formatters.currencyFromCents(debtor.currentAmountCents, debtor.currency, hide = hideAmounts),
                        Formatters.currencyFromCents(debtor.targetAmountCents, debtor.currency, hide = hideAmounts),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!debtor.isPaid) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onAddPayment,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(
                                if (debtor.debtType == DebtType.CREDITOR) {
                                    R.string.action_add_payment
                                } else {
                                    R.string.action_make_payment
                                },
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = onEdit,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 8.dp,
                            vertical = 4.dp,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    if (showRemind) {
                        OutlinedButton(
                            onClick = onRemind,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 8.dp,
                                vertical = 4.dp,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.action_remind),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    if (onCalendar != null) {
                        OutlinedButton(
                            onClick = onCalendar,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 8.dp,
                                vertical = 4.dp,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = stringResource(
                                    if (calendarSynced) {
                                        R.string.calendar_action_update
                                    } else {
                                        R.string.calendar_action_add
                                    },
                                ),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 8.dp,
                            vertical = 4.dp,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.action_edit),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtDisplayStatusChip(dueStatus: DebtDueStatus) {
    val (label, color) = when (dueStatus) {
        DebtDueStatus.PAID -> stringResource(R.string.debtor_paid) to GoldPrimary
        DebtDueStatus.OVERDUE -> stringResource(R.string.debtor_overdue) to OverdueRed
        else -> stringResource(R.string.debt_status_active) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = label,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun DebtorAvatar(name: String) {
    val avatarColor = avatarColorForName(name)
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(avatarColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.initials(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun DebtDueStatusLabel(status: DebtDueStatus, dateDue: Long) {
    when (status) {
        DebtDueStatus.OVERDUE -> {
            Text(
                text = stringResource(R.string.debtor_due_date, Formatters.shortDate(dateDue)),
                color = OverdueRed,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        DebtDueStatus.URGENT -> {
            Box(
                modifier = Modifier
                    .background(UrgentYellowBg, MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = stringResource(R.string.debtor_due_short, Formatters.shortDayMonth(dateDue)),
                    color = UrgentYellowText,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        DebtDueStatus.PAID -> {
            Text(
                text = stringResource(R.string.debtor_paid),
                color = GoldPrimary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
        DebtDueStatus.NORMAL -> {
            Text(
                text = stringResource(R.string.debtor_due_date, Formatters.shortDate(dateDue)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun avatarColorForName(name: String): Color {
    val palette = listOf(
        Color(0xFF8D6E63),
        Color(0xFF6D8B74),
        Color(0xFF7B6FA6),
        Color(0xFF5C7EAE),
        Color(0xFFB07D62),
        Color(0xFF9E7C5C),
    )
    val index = kotlin.math.abs(name.hashCode()) % palette.size
    return palette[index]
}
