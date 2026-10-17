package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.Debtor
import com.example.money.domain.util.LocalFinancialAdvisor
import com.example.money.settings.AutopilotStrategy
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtAdvisorBottomSheet(
    debts: List<Debtor>,
    messages: List<AdvisorChatMessage>,
    onPromptSelected: (String) -> Unit,
    onFreeText: (String) -> Unit,
    onClearHistory: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.advisor_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.advisor_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdvisorPrompt.entries.forEach { prompt ->
                    FilterChip(
                        selected = false,
                        onClick = { onPromptSelected(prompt.id) },
                        label = { Text(stringResource(prompt.labelRes)) },
                    )
                }
            }

            messages.forEach { message ->
                AdvisorBubble(message = message)
            }

            if (messages.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text(stringResource(R.string.advisor_clear_history))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.advisor_input_hint)) },
                )
                IconButton(
                    onClick = {
                        val text = draft.trim()
                        if (text.isNotEmpty()) {
                            onFreeText(text)
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = stringResource(R.string.advisor_send),
                    )
                }
            }
        }
    }
}

data class AdvisorChatMessage(
    val role: AdvisorRole,
    val text: String,
)

enum class AdvisorRole {
    USER,
    ADVISOR,
}

private enum class AdvisorPrompt(val id: String, val labelRes: Int) {
    OVERVIEW("overview", R.string.advisor_prompt_overview),
    WHICH_FIRST("which_debt_first", R.string.advisor_prompt_first),
    WHAT_IF("what_if_extra", R.string.advisor_prompt_extra),
    OVERDUE("overdue", R.string.advisor_prompt_overdue),
    GOALS("goals", R.string.advisor_prompt_goals),
    BUDGET("budget", R.string.advisor_prompt_budget),
    FREEDOM("freedom", R.string.advisor_prompt_freedom),
    MOTIVATION("motivation", R.string.advisor_prompt_motivation),
}

@Composable
private fun AdvisorBubble(message: AdvisorChatMessage) {
    val isAdvisor = message.role == AdvisorRole.ADVISOR
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdvisor) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Text(
            text = message.text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

fun buildAdvisorMessages(
    promptId: String,
    debts: List<Debtor>,
    previous: List<AdvisorChatMessage>,
    rawText: String = "",
    totalAccumulatedCents: Long = 0L,
    remainingToGoalCents: Long = 0L,
    payoffStrategy: AutopilotStrategy = AutopilotStrategy.AVALANCHE,
    locale: Locale = Locale.getDefault(),
): List<AdvisorChatMessage> {
    val en = locale.language.startsWith("en", ignoreCase = true)
    val context = LocalFinancialAdvisor.AdviceContext(
        debts = debts,
        totalAccumulatedCents = totalAccumulatedCents,
        remainingToGoalCents = remainingToGoalCents,
        payoffStrategy = payoffStrategy,
        locale = locale,
    )
    val request = LocalFinancialAdvisor.AdviceRequest(rawText = rawText, promptId = promptId)
    val resolvedId = LocalFinancialAdvisor.resolvePromptId(request)
    val promptLabel = LocalFinancialAdvisor.userFacingPromptLabel(resolvedId, rawText, en)
    val reply = LocalFinancialAdvisor.buildReply(request.copy(promptId = resolvedId), context)
    return previous + AdvisorChatMessage(AdvisorRole.USER, promptLabel) +
        AdvisorChatMessage(AdvisorRole.ADVISOR, reply)
}
