package com.example.money.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.ui.debtors.DebtDueStatus
import com.example.money.ui.debtors.dueStatus
import com.example.money.ui.debtors.initials
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.theme.OverdueRed
import com.example.money.ui.theme.moneyCaptionStyle
import com.example.money.ui.theme.moneyDisplayStyle
import com.example.money.ui.theme.moneySubheadingStyle
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
    onViewContactHistory: (() -> Unit)? = null,
    calendarSynced: Boolean = false,
    hideAmounts: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val dueStatus = debtor.dueStatus()
    val isOverdue = dueStatus == DebtDueStatus.OVERDUE
    val showRemind = debtor.debtType == DebtType.CREDITOR && !debtor.isPaid
    var showMenu by remember { mutableStateOf(false) }

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
                .padding(MoneySpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MoneySpacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MoneySpacing.md),
            ) {
                DebtorAvatar(name = debtor.name)

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = debtor.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        DebtStatusBadge(
                            label = when (dueStatus) {
                                DebtDueStatus.PAID -> stringResource(R.string.debtor_paid)
                                DebtDueStatus.OVERDUE -> stringResource(R.string.debtor_overdue)
                                else -> stringResource(R.string.debt_status_active)
                            },
                            status = dueStatus.toVisual(),
                        )
                    }

                    Text(
                        text = Formatters.currencyFromCents(
                            if (debtor.isPaid) debtor.targetAmountCents else debtor.remainingAmountCents,
                            debtor.currency,
                            hide = hideAmounts,
                        ),
                        style = moneyDisplayStyle(),
                    )

                    DebtDueStatusLabel(status = dueStatus, dateDue = debtor.dateDue)
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(MoneySpacing.minTouchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.action_more),
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        if (!debtor.isPaid) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_edit)) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Edit, contentDescription = null)
                                },
                            )
                            if (showRemind) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_remind)) },
                                    onClick = {
                                        showMenu = false
                                        onRemind()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Share, contentDescription = null)
                                    },
                                )
                            }
                            if (onViewContactHistory != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.contact_history_action)) },
                                    onClick = {
                                        showMenu = false
                                        onViewContactHistory()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.History, contentDescription = null)
                                    },
                                )
                            }
                            if (onCalendar != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                if (calendarSynced) {
                                                    R.string.calendar_action_update
                                                } else {
                                                    R.string.calendar_action_add
                                                },
                                            ),
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onCalendar()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                                    },
                                )
                            }
                            if (onAccelerate != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.debt_accelerate)) },
                                    onClick = {
                                        showMenu = false
                                        onAccelerate()
                                    },
                                )
                            }
                        } else {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_edit)) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Edit, contentDescription = null)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Delete, contentDescription = null)
                            },
                        )
                    }
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
                        R.string.debtor_paid_progress,
                        Formatters.currencyFromCents(debtor.currentAmountCents, debtor.currency, hide = hideAmounts),
                        Formatters.currencyFromCents(debtor.targetAmountCents, debtor.currency, hide = hideAmounts),
                    ),
                    style = moneyCaptionStyle(),
                )

                MoneyButton(
                    text = stringResource(
                        if (debtor.debtType == DebtType.CREDITOR) {
                            R.string.action_add_payment
                        } else {
                            R.string.action_make_payment
                        },
                    ),
                    onClick = onAddPayment,
                    priority = MoneyButtonPriority.Primary,
                    icon = Icons.Default.Payments,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DebtorAvatar(name: String) {
    val avatarColor = avatarColorForName(name)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(avatarColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.initials(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
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
                style = moneySubheadingStyle(),
                fontWeight = FontWeight.SemiBold,
            )
        }
        DebtDueStatus.URGENT -> {
            Text(
                text = stringResource(R.string.debtor_due_short, Formatters.shortDayMonth(dateDue)),
                color = OverdueRed,
                style = moneyCaptionStyle(),
                fontWeight = FontWeight.Medium,
            )
        }
        DebtDueStatus.PAID -> {
            Text(
                text = stringResource(R.string.debtor_paid),
                style = moneyCaptionStyle(),
                color = GoldPrimary,
            )
        }
        DebtDueStatus.NORMAL -> {
            Text(
                text = stringResource(R.string.debtor_due_date, Formatters.shortDate(dateDue)),
                style = moneyCaptionStyle(),
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
