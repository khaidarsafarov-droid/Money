package com.example.money.ui.debtors

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.Debtor

enum class CelebrationReward {
    WALK,
    MOVIE,
    MEAL,
}

@Composable
fun DebtCelebrationDialog(
    debtor: Debtor,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val shareText = stringResource(R.string.celebration_share_text)
    var selectedReward by remember { mutableStateOf<CelebrationReward?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.celebration_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.celebration_body, debtor.name),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.celebration_reward_prompt),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CelebrationReward.entries.forEach { reward ->
                        FilterChip(
                            selected = selectedReward == reward,
                            onClick = { selectedReward = reward },
                            label = {
                                Text(
                                    when (reward) {
                                        CelebrationReward.WALK -> stringResource(R.string.celebration_reward_walk)
                                        CelebrationReward.MOVIE -> stringResource(R.string.celebration_reward_movie)
                                        CelebrationReward.MEAL -> stringResource(R.string.celebration_reward_meal)
                                    },
                                )
                            },
                        )
                    }
                }
                if (selectedReward != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "🎉",
                                style = MaterialTheme.typography.displaySmall,
                            )
                            Text(
                                text = stringResource(R.string.celebration_postcard_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedReward != null,
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.celebration_share))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}
