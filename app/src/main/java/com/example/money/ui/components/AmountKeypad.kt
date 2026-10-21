package com.example.money.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val keypadRows = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf(",", "0", "⌫"),
)

@Composable
fun AmountKeypad(
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        keypadRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 58.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onKey(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = key, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

fun applyAmountKey(current: String, key: String): String = when (key) {
    "⌫" -> current.dropLast(1)
    "," -> if (current.contains(',') || current.contains('.')) current else current + ","
    else -> if (current.length >= 12) current else current + key
}
