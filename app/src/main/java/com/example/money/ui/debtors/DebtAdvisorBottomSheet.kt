package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.Debtor
import com.example.money.domain.util.DebtAdvisorTemplates
import com.example.money.ui.theme.CreamSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtAdvisorBottomSheet(
    debts: List<Debtor>,
    messages: List<AdvisorChatMessage>,
    onPromptSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
    WHICH_FIRST("which_debt_first", R.string.advisor_prompt_first),
    WHAT_IF("what_if_extra", R.string.advisor_prompt_extra),
    MOTIVATION("motivation", R.string.advisor_prompt_motivation),
}

@Composable
private fun AdvisorBubble(message: AdvisorChatMessage) {
    val isAdvisor = message.role == AdvisorRole.ADVISOR
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdvisor) CreamSurface else MaterialTheme.colorScheme.surfaceVariant,
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
): List<AdvisorChatMessage> {
    val promptLabel = when (promptId) {
        "which_debt_first" -> "Какой долг закрыть первым?"
        "what_if_extra" -> "Что если платить на $50 больше?"
        "motivation" -> "Дай мотивацию"
        else -> promptId
    }
    val reply = DebtAdvisorTemplates.buildReply(promptId, debts)
    return previous + AdvisorChatMessage(AdvisorRole.USER, promptLabel) +
        AdvisorChatMessage(AdvisorRole.ADVISOR, reply)
}
