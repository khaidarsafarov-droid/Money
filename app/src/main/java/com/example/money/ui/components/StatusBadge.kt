package com.example.money.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.ui.debtors.DebtDueStatus

enum class DebtStatusVisual {
    Active,
    Overdue,
    Paid,
}

@Composable
fun DebtStatusBadge(
    label: String,
    status: DebtStatusVisual,
    modifier: Modifier = Modifier,
) {
    val (background, foreground) = when (status) {
        DebtStatusVisual.Active -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        DebtStatusVisual.Overdue -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        DebtStatusVisual.Paid -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Text(
        text = label,
        color = foreground,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(background, MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

fun DebtDueStatus.toVisual(): DebtStatusVisual = when (this) {
    DebtDueStatus.PAID -> DebtStatusVisual.Paid
    DebtDueStatus.OVERDUE -> DebtStatusVisual.Overdue
    DebtDueStatus.URGENT, DebtDueStatus.NORMAL -> DebtStatusVisual.Active
}

@Composable
fun GoalStatusBadge(
    label: String,
    isCompleted: Boolean,
    isOverdue: Boolean,
    modifier: Modifier = Modifier,
) {
    val (background, foreground) = when {
        isCompleted -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        isOverdue -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
    }
    Text(
        text = label,
        color = foreground,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(background, MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
