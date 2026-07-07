package com.example.money.ui.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.money.R

private data class PsychotypeQuestion(
    val textRes: Int,
    val options: List<Pair<Char, Int>>,
)

private val PSYCHOTYPE_QUESTIONS = listOf(
    PsychotypeQuestion(
        R.string.psychotype_q1,
        listOf('a' to R.string.psychotype_q1_a, 'b' to R.string.psychotype_q1_b, 'c' to R.string.psychotype_q1_c, 'd' to R.string.psychotype_q1_d),
    ),
    PsychotypeQuestion(
        R.string.psychotype_q2,
        listOf('a' to R.string.psychotype_q2_a, 'b' to R.string.psychotype_q2_b, 'c' to R.string.psychotype_q2_c, 'd' to R.string.psychotype_q2_d),
    ),
    PsychotypeQuestion(
        R.string.psychotype_q3,
        listOf('a' to R.string.psychotype_q3_a, 'b' to R.string.psychotype_q3_b, 'c' to R.string.psychotype_q3_c, 'd' to R.string.psychotype_q3_d),
    ),
    PsychotypeQuestion(
        R.string.psychotype_q4,
        listOf('a' to R.string.psychotype_q4_a, 'b' to R.string.psychotype_q4_b, 'c' to R.string.psychotype_q4_c, 'd' to R.string.psychotype_q4_d),
    ),
    PsychotypeQuestion(
        R.string.psychotype_q5,
        listOf('a' to R.string.psychotype_q5_a, 'b' to R.string.psychotype_q5_b, 'c' to R.string.psychotype_q5_c, 'd' to R.string.psychotype_q5_d),
    ),
)

@Composable
fun PsychotypeTestDialog(
    onDismiss: () -> Unit,
    onComplete: (List<Char>) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateOf(List(PSYCHOTYPE_QUESTIONS.size) { 'a' }) }
    val question = PSYCHOTYPE_QUESTIONS[step]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.psychotype_test_title, step + 1, PSYCHOTYPE_QUESTIONS.size)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(question.textRes), style = MaterialTheme.typography.bodyMedium)
                question.options.forEach { (key, labelRes) ->
                    FilterChip(
                        selected = answers.value[step] == key,
                        onClick = {
                            val updated = answers.value.toMutableList()
                            updated[step] = key
                            answers.value = updated
                        },
                        label = { Text(stringResource(labelRes)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (step < PSYCHOTYPE_QUESTIONS.lastIndex) {
                        step += 1
                    } else {
                        onComplete(answers.value)
                        onDismiss()
                    }
                },
            ) {
                Text(
                    if (step < PSYCHOTYPE_QUESTIONS.lastIndex) {
                        stringResource(R.string.action_next)
                    } else {
                        stringResource(R.string.action_done)
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
