package com.example.money.ui.business.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R

data class BusinessAssistantMessage(
    val role: BusinessAssistantRole,
    val text: String,
)

enum class BusinessAssistantRole {
    USER,
    ASSISTANT,
}

enum class BusinessAssistantPrompt(val id: String, val labelRes: Int) {
    PROFIT_SUMMARY("profit_summary", R.string.business_assistant_prompt_profit),
    CASHFLOW_FORECAST("cashflow_forecast", R.string.business_assistant_prompt_forecast),
    OVERDUE_INVOICES("overdue_invoices", R.string.business_assistant_prompt_overdue),
    TOP_CLIENTS("top_clients", R.string.business_assistant_prompt_clients),
    INSIGHTS("insights", R.string.business_assistant_prompt_insights),
    INVOICE_RISKS("invoice_risks", R.string.business_assistant_prompt_risks),
    CATEGORY_HINT("category_hint", R.string.business_assistant_prompt_category),
}

@Composable
fun BusinessAiAssistantScreen(
    messages: List<BusinessAssistantMessage>,
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.business_assistant_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.business_assistant_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BusinessAssistantPrompt.entries.forEach { prompt ->
                FilterChip(
                    selected = false,
                    onClick = { onPromptSelected(prompt.id) },
                    label = { Text(stringResource(prompt.labelRes)) },
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages) { message ->
                BusinessAssistantBubble(message = message)
            }
        }
    }
}

@Composable
private fun BusinessAssistantBubble(message: BusinessAssistantMessage) {
    val isAssistant = message.role == BusinessAssistantRole.ASSISTANT
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAssistant) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Text(
            text = message.text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
