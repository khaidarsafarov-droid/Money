package com.example.money.ui.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.util.MoneyMath
import com.example.money.ui.components.DatePickerField
import com.example.money.ui.components.MoneyDialog
import com.example.money.ui.components.MoneyTextField
import com.example.money.ui.util.InputValidators
import java.util.Calendar

private fun formatAmountForInput(amount: Double): String {
    if (amount <= 0) return ""
    return if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
}

private fun defaultDeadlineMillis(): Long {
    val cal = Calendar.getInstance().apply {
        add(Calendar.MONTH, 3)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

@Composable
fun GoalFormDialog(
    existingGoal: SavingsGoal? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        kind: GoalKind,
        targetAmountCents: Long,
        currentAmountCents: Long,
        deadlineMillis: Long?,
    ) -> Unit,
) {
    val isEdit = existingGoal != null
    val goal = existingGoal
    val context = LocalContext.current

    var kind by remember(existingGoal?.id) {
        mutableStateOf(existingGoal?.kind ?: GoalKind.ACTION)
    }
    var name by remember(existingGoal?.id) {
        mutableStateOf(existingGoal?.name.orEmpty())
    }
    var targetText by remember(existingGoal?.id) {
        mutableStateOf(
            existingGoal?.let { formatAmountForInput(MoneyMath.fromCents(it.targetAmountCents)) }
                .orEmpty(),
        )
    }
    var hasDeadline by remember(existingGoal?.id) {
        mutableStateOf(existingGoal?.deadlineMillis != null)
    }
    var deadlineMillis by remember(existingGoal?.id) {
        mutableLongStateOf(existingGoal?.deadlineMillis ?: defaultDeadlineMillis())
    }

    var nameError by remember { mutableStateOf<String?>(null) }
    var targetError by remember { mutableStateOf<String?>(null) }

    MoneyDialog(
        title = if (goal != null) {
            stringResource(R.string.goal_edit_title, goal.name)
        } else {
            stringResource(R.string.goal_add_title)
        },
        onDismiss = onDismiss,
        confirmLabel = if (isEdit) {
            stringResource(R.string.action_save)
        } else {
            stringResource(R.string.goal_add_confirm)
        },
        onConfirm = {
            nameError = InputValidators.validateGoalPurpose(name)?.let(context::getString)
            val isMoney = kind == GoalKind.MONEY
            targetError = if (isMoney) {
                InputValidators.validateTargetAmount(targetText)?.let(context::getString)
            } else {
                null
            }
            val target = if (isMoney) {
                InputValidators.parseAmount(targetText) ?: return@MoneyDialog
            } else {
                goal?.targetAmountCents ?: 0L
            }

            if (nameError != null || targetError != null) return@MoneyDialog

            onConfirm(
                name.trim(),
                kind,
                target,
                goal?.currentAmountCents ?: 0L,
                deadlineMillis.takeIf { hasDeadline },
            )
        },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.goal_kind_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = kind == GoalKind.ACTION,
                    onClick = { kind = GoalKind.ACTION },
                    label = { Text(stringResource(R.string.goal_kind_action)) },
                )
                FilterChip(
                    selected = kind == GoalKind.MONEY,
                    onClick = { kind = GoalKind.MONEY },
                    label = { Text(stringResource(R.string.goal_kind_money)) },
                )
            }
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = { Text(stringResource(R.string.goal_name_label)) },
                placeholder = { Text(stringResource(R.string.goal_name_hint)) },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (kind == GoalKind.MONEY) {
                MoneyTextField(
                    value = targetText,
                    onValueChange = {
                        targetText = it
                        targetError = null
                    },
                    label = stringResource(R.string.goal_target_label),
                    isError = targetError != null,
                    supportingText = targetError,
                )
            } else {
                Text(
                    text = stringResource(R.string.goal_action_kind_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (kind == GoalKind.MONEY) {
                Text(
                    text = stringResource(R.string.goal_kind_money_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (kind == GoalKind.MONEY && isEdit) {
                Text(
                    text = stringResource(
                        R.string.goal_current_readonly,
                        formatAmountForInput(MoneyMath.fromCents(goal?.currentAmountCents ?: 0L)),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (kind == GoalKind.MONEY) {
                Text(
                    text = stringResource(R.string.goal_starts_at_zero_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.goal_deadline_toggle))
                    Text(
                        text = stringResource(R.string.goal_deadline_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = hasDeadline,
                    onCheckedChange = { hasDeadline = it },
                )
            }
            if (hasDeadline) {
                DatePickerField(
                    label = stringResource(R.string.goal_deadline_label),
                    selectedDateMillis = deadlineMillis,
                    onDateSelected = { deadlineMillis = it },
                )
            }
        }
    }
}
