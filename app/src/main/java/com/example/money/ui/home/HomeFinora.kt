package com.example.money.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.money.R
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.util.DebtDueDateUtils
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.components.EmptyIllustration
import com.example.money.ui.debtors.DebtorSort
import com.example.money.ui.motion.AnimatedAmount
import com.example.money.ui.motion.MotionGlyph
import com.example.money.ui.motion.MotionGlyphIcon
import com.example.money.ui.motion.moneySharedBounds
import com.example.money.ui.motion.pressScale
import com.example.money.ui.motion.rememberDrawFraction
import com.example.money.ui.theme.LocalMoneyExtraColors
import com.example.money.ui.theme.finoraAvatarColor
import com.example.money.ui.theme.finoraBalanceBrush
import com.example.money.ui.theme.finoraGoalBrush
import com.example.money.ui.theme.finoraInitials
import com.example.money.ui.util.Formatters
import kotlin.math.abs

fun LazyListScope.homeFinora(
    goals: List<SavingsGoal>,
    debtors: List<Debtor>,
    owedToMeCents: Long,
    iOweCents: Long,
    hideAmounts: Boolean,
    onOwesMe: () -> Unit,
    onIOwe: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenGoal: (SavingsGoal) -> Unit,
    onOpenDebts: () -> Unit,
    onOpenDebtor: (Debtor) -> Unit,
    onTook: () -> Unit,
    onReturned: () -> Unit,
    onAddDebt: () -> Unit,
    onPhotoCamera: (() -> Unit)?,
    onPhotoGallery: (() -> Unit)?,
) {
    item {
        FinoraBalanceCard(
            owedToMeCents = owedToMeCents,
            iOweCents = iOweCents,
            hideAmounts = hideAmounts,
            onOwesMe = onOwesMe,
            onIOwe = onIOwe,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    item {
        FinoraQuickActions(
            onTook = onTook,
            onReturned = onReturned,
            onAddDebt = onAddDebt,
            onPhotoCamera = onPhotoCamera,
            onPhotoGallery = onPhotoGallery,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
    val previewGoals = goals.filter { !it.isCompleted }.take(2)
    item {
        FinoraSectionTitle(
            title = stringResource(R.string.section_goals),
            action = stringResource(R.string.action_see_all),
            onAction = onOpenGoals,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    if (previewGoals.isNotEmpty()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                previewGoals.forEach { goal ->
                    FinoraGoalPreview(
                        goal = goal,
                        hideAmounts = hideAmounts,
                        onClick = { onOpenGoal(goal) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
    val nearest = DebtorSort.sort(debtors.filter { !it.isPaid }).take(3)
    item {
        FinoraSectionTitle(
            title = stringResource(R.string.home_upcoming),
            action = stringResource(R.string.action_see_all),
            onAction = onOpenDebts,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    if (nearest.isEmpty()) {
        item {
            EmptyStateMessage(
                title = stringResource(R.string.empty_debtors_active),
                illustration = EmptyIllustration.Debts,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    } else {
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp),
            ) {
                nearest.forEachIndexed { index, debtor ->
                    FinoraDebtRow(
                        debtor = debtor,
                        hideAmounts = hideAmounts,
                        onClick = { onOpenDebtor(debtor) },
                    )
                    if (index < nearest.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
            }
        }
    }
    item { Box(modifier = Modifier.height(12.dp)) }
}

@Composable
fun FinoraBalanceCard(
    owedToMeCents: Long,
    iOweCents: Long,
    hideAmounts: Boolean,
    onOwesMe: () -> Unit,
    onIOwe: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.home_balance_label),
) {
    val net = owedToMeCents - iOweCents
    val netText = if (hideAmounts) {
        Formatters.currencyFromCents(net, hide = true)
    } else {
        val sign = if (net > 0L) "+ " else if (net < 0L) "− " else ""
        sign + Formatters.currencyFromCents(abs(net), hide = false)
    }
    Column(
        modifier = modifier
            .pressScale()
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(finoraBalanceBrush())
            .padding(18.dp),
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        AnimatedAmount(
            text = netText,
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BalancePill(
                label = stringResource(R.string.debt_dashboard_owes_me),
                value = Formatters.currencyFromCents(owedToMeCents, hide = hideAmounts),
                onClick = onOwesMe,
                modifier = Modifier.weight(1f),
            )
            BalancePill(
                label = stringResource(R.string.debt_dashboard_i_owe),
                value = Formatters.currencyFromCents(iOweCents, hide = hideAmounts),
                onClick = onIOwe,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BalancePill(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .pressScale()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 1)
        Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun FinoraQuickActions(
    onTook: () -> Unit,
    onReturned: () -> Unit,
    onAddDebt: () -> Unit,
    onPhotoCamera: (() -> Unit)?,
    onPhotoGallery: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val extra = LocalMoneyExtraColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        QuickAction(stringResource(R.string.debt_history_took), Icons.Rounded.SouthWest, extra.negativeContainer, extra.negative, onTook, MotionGlyph.Took)
        QuickAction(stringResource(R.string.debt_history_returned), Icons.Rounded.NorthEast, extra.positiveContainer, extra.positive, onReturned, MotionGlyph.Returned)
        PhotoAction(onPhotoCamera, onPhotoGallery)
        QuickAction(stringResource(R.string.qa_debt), Icons.Rounded.Add, extra.positiveContainer, extra.positive, onAddDebt)
    }
}

@Composable
private fun PhotoAction(
    onCamera: (() -> Unit)?,
    onGallery: (() -> Unit)?,
) {
    var open by remember { mutableStateOf(false) }
    val extra = LocalMoneyExtraColors.current
    Box {
        QuickAction(
            label = stringResource(R.string.qa_photo),
            icon = Icons.Rounded.PhotoCamera,
            container = extra.positiveContainer,
            tint = extra.positive,
            onClick = {
                when {
                    onCamera != null && onGallery != null -> open = true
                    onCamera != null -> onCamera()
                    onGallery != null -> onGallery()
                }
            },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (onCamera != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.debt_import_camera)) },
                    onClick = {
                        open = false
                        onCamera()
                    },
                )
            }
            if (onGallery != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.debt_import_gallery)) },
                    onClick = {
                        open = false
                        onGallery()
                    },
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    container: Color,
    tint: Color,
    onClick: () -> Unit,
    glyph: MotionGlyph? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .pressScale()
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center,
        ) {
            if (glyph != null) {
                MotionGlyphIcon(glyph = glyph, tint = tint, play = false, modifier = Modifier.size(22.dp))
            } else {
                Icon(imageVector = icon, contentDescription = label, tint = tint)
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun FinoraSectionTitle(
    title: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = action,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onAction),
        )
    }
}

@Composable
private fun FinoraGoalPreview(
    goal: SavingsGoal,
    hideAmounts: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawFraction = rememberDrawFraction(goal.progressFraction)
    Column(
        modifier = modifier
            .moneySharedBounds("goal-detail-${goal.id}")
            .pressScale()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(finoraGoalBrush(goal.name.hashCode())),
        )
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(text = goal.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = Formatters.currencyFromCents(goal.currentAmountCents, hide = hideAmounts) +
                    " / " +
                    Formatters.currencyFromCents(goal.targetAmountCents, hide = hideAmounts),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress = drawFraction,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
fun FinoraDebtRow(
    debtor: Debtor,
    hideAmounts: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    val owedToMe = debtor.debtType == com.example.money.domain.model.DebtType.CREDITOR
    val amountColor = if (owedToMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val amount = Formatters.currencyFromCents(debtor.remainingAmountCents, hide = hideAmounts)
    val signed = if (hideAmounts || owedToMe) amount else "− $amount"
    val due = subtitle ?: when {
        debtor.isPaid -> stringResource(R.string.debtor_paid)
        debtor.dateDue <= 0L -> ""
        DebtDueDateUtils.isDueToday(debtor.dateDue) -> stringResource(R.string.debt_chip_today)
        DebtDueDateUtils.isOverdue(debtor.dateDue) -> stringResource(R.string.debt_filter_overdue)
        else -> Formatters.dueDateCompact(debtor.dateDue)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FinoraAvatar(debtor.name)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = debtor.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (due.isNotBlank()) {
                Text(text = due, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
            }
        }
        Text(text = signed, color = amountColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
fun FinoraAvatar(name: String, sharedKey: Any? = null) {
    Box(
        modifier = Modifier
            .then(if (sharedKey != null) Modifier.moneySharedBounds(sharedKey) else Modifier)
            .size(40.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(finoraAvatarColor(name)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = finoraInitials(name), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}
