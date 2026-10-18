package com.example.money.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.ui.debtors.DebtDueStatus
import com.example.money.ui.debtors.dueStatus
import com.example.money.ui.debtors.initials
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.theme.moneyCaptionStyle
import com.example.money.ui.util.Formatters

@Composable
fun DebtorListItem(
    debtor: Debtor,
    onRemind: () -> Unit,
    onAddPayment: () -> Unit,
    onOpen: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAccelerate: (() -> Unit)? = null,
    onCalendar: (() -> Unit)? = null,
    onViewContactHistory: (() -> Unit)? = null,
    onCall: (() -> Unit)? = null,
    onWhatsApp: (() -> Unit)? = null,
    onMarkPaid: (() -> Unit)? = null,
    calendarSynced: Boolean = false,
    hideAmounts: Boolean = false,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val dueStatus = debtor.dueStatus()
    val isOverdue = dueStatus == DebtDueStatus.OVERDUE
    val showRemind = debtor.debtType == DebtType.CREDITOR && !debtor.isPaid
    var showMenu by remember { mutableStateOf(false) }

    val rowBackground = when {
        highlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        isOverdue -> MaterialTheme.colorScheme.error.copy(alpha = 0.06f)
        else -> Color.Transparent
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(rowBackground)
                .clickable(onClick = onOpen)
                .padding(horizontal = MoneySpacing.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
        ) {
            DebtorAvatar(name = debtor.name, overdue = isOverdue)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debtor.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                DebtDueStatusLabel(status = dueStatus, dateDue = debtor.dateDue)
            }

            Text(
                text = Formatters.currencyFromCents(
                    if (debtor.isPaid) debtor.targetAmountCents else debtor.remainingAmountCents,
                    debtor.currency,
                    hide = hideAmounts,
                ),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )

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
                            text = {
                                Text(
                                    stringResource(
                                        if (debtor.debtType == DebtType.CREDITOR) {
                                            R.string.action_add_payment
                                        } else {
                                            R.string.action_make_payment
                                        },
                                    ),
                                )
                            },
                            onClick = {
                                showMenu = false
                                onAddPayment()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Payments, contentDescription = null)
                            },
                        )
                        if (onCall != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.debt_action_call)) },
                                onClick = {
                                    showMenu = false
                                    onCall()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Call, contentDescription = null)
                                },
                            )
                        }
                        if (onWhatsApp != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.debt_action_whatsapp)) },
                                onClick = {
                                    showMenu = false
                                    onWhatsApp()
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null)
                                },
                            )
                        }
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
                                    Icon(Icons.Outlined.Notifications, contentDescription = null)
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
                                                R.string.calendar_action_remove
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
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Outlined.TrendingUp, contentDescription = null)
                                },
                            )
                        }
                        if (onMarkPaid != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_mark_paid)) },
                                onClick = {
                                    showMenu = false
                                    onMarkPaid()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Payments, contentDescription = null)
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
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            thickness = 0.5.dp,
        )
    }
}

@Composable
private fun DebtorAvatar(name: String, overdue: Boolean) {
    val avatarColor = avatarColorForName(name)
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.initials(),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (overdue) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
            )
        }
    }
}

@Composable
private fun DebtDueStatusLabel(status: DebtDueStatus, dateDue: Long) {
    when (status) {
        DebtDueStatus.OVERDUE -> {
            Text(
                text = stringResource(R.string.debtor_due_date, Formatters.dueDateCompact(dateDue)),
                color = MaterialTheme.colorScheme.error,
                style = moneyCaptionStyle(),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DebtDueStatus.URGENT -> {
            Text(
                text = stringResource(R.string.debtor_due_short, Formatters.dueDateCompact(dateDue)),
                color = MaterialTheme.colorScheme.error,
                style = moneyCaptionStyle(),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DebtDueStatus.PAID -> {
            Text(
                text = stringResource(R.string.debtor_paid),
                style = moneyCaptionStyle(),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
        DebtDueStatus.NORMAL -> {
            Text(
                text = stringResource(R.string.debtor_due_date, Formatters.dueDateCompact(dateDue)),
                style = moneyCaptionStyle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
