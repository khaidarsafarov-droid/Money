package com.example.money.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.money.ui.theme.finoraAvatarColor
import com.example.money.ui.theme.finoraInitials
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.util.DebtDueDateUtils
import com.example.money.settings.AppMode
import com.example.money.ui.debtors.DebtDueStatus
import com.example.money.ui.debtors.dueStatus
import com.example.money.ui.debtors.initials
import com.example.money.ui.motion.PaidCheck
import com.example.money.ui.motion.pressScale
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.theme.moneyCaptionStyle
import com.example.money.ui.util.Formatters

@Suppress("UNUSED_PARAMETER")
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

    val dueToday = debtor.mode == AppMode.PROFESSIONAL &&
        !debtor.isPaid &&
        DebtDueDateUtils.isDueToday(debtor.dateDue)
    val rowBackground = when {
        highlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        dueToday -> MaterialTheme.colorScheme.primaryContainer
        isOverdue -> MaterialTheme.colorScheme.error.copy(alpha = 0.06f)
        else -> Color.Transparent
    }
    val amountLabel = Formatters.currencyFromCents(
        if (debtor.isPaid) debtor.targetAmountCents else debtor.remainingAmountCents,
        debtor.currency,
        hide = hideAmounts,
    )
    val dueSpoken = when (dueStatus) {
        DebtDueStatus.PAID -> stringResource(R.string.debtor_paid)
        DebtDueStatus.OVERDUE, DebtDueStatus.URGENT, DebtDueStatus.NORMAL ->
            if (debtor.dateDue > 0L) {
                stringResource(R.string.debtor_due_date, Formatters.dueDateCompact(debtor.dateDue))
            } else {
                ""
            }
    }
    val rowSpoken = listOf(debtor.name, amountLabel, dueSpoken)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    val openLabel = stringResource(R.string.a11y_open)
    val paidActionLabel = stringResource(R.string.a11y_mark_paid)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(rowBackground)
                .pressScale()
                .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen)
                .heightIn(min = MoneySpacing.listRow)
                .padding(horizontal = MoneySpacing.lg, vertical = MoneySpacing.md)
                .semantics(mergeDescendants = true) {
                    contentDescription = rowSpoken
                    if (!debtor.isPaid && onMarkPaid != null) {
                        customActions = listOf(
                            CustomAccessibilityAction(paidActionLabel) {
                                onMarkPaid()
                                true
                            },
                        )
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
        ) {
            DebtorAvatar(name = debtor.name, overdue = isOverdue, sharedKey = "debtor-${debtor.id}", paid = debtor.isPaid)

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
                text = amountLabel,
                modifier = Modifier.widthIn(max = 140.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                    if (!debtor.isPaid && onMarkPaid != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_mark_paid)) },
                            onClick = {
                                showMenu = false
                                onMarkPaid()
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Check, contentDescription = null)
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
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp,
        )
    }
}

@Composable
private fun DebtorAvatar(name: String, overdue: Boolean, sharedKey: Any, paid: Boolean) {
    val scheme = MaterialTheme.colorScheme
    var playCheck by remember(sharedKey) { mutableStateOf(false) }
    var ready by remember(sharedKey) { mutableStateOf(false) }
    LaunchedEffect(paid) {
        if (!ready) {
            ready = true
            return@LaunchedEffect
        }
        if (paid) playCheck = true
    }
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(finoraAvatarColor(name))
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = finoraInitials(name),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium,
            )
            if (paid) {
                PaidCheck(
                    play = playCheck,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (overdue) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(scheme.error),
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
            if (dateDue <= 0L) return
            Text(
                text = stringResource(R.string.debtor_due_date, Formatters.dueDateCompact(dateDue)),
                style = moneyCaptionStyle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

