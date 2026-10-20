package com.example.money.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.theme.moneyBodyStyle
import com.example.money.ui.theme.moneyHeadingStyle

enum class EmptyIllustration {
    General,
    Debts,
    Goals,
    Notes,
    Search,
}

@Composable
fun EmptyStateMessage(
    title: String,
    hint: String = "",
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    illustration: EmptyIllustration = EmptyIllustration.General,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val resolvedIcon = icon ?: when (illustration) {
        EmptyIllustration.Debts -> Icons.Rounded.People
        EmptyIllustration.Goals -> Icons.Rounded.Flag
        EmptyIllustration.Notes -> Icons.Rounded.EditNote
        EmptyIllustration.Search -> Icons.Outlined.Search
        EmptyIllustration.General -> Icons.Rounded.Home
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MoneySpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
        ) {
            Icon(
                imageVector = resolvedIcon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = title,
                style = moneyHeadingStyle(),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (hint.isNotBlank()) {
                Text(
                    text = hint,
                    style = moneyBodyStyle(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (actionLabel != null && onAction != null) {
                MoneyButton(
                    text = actionLabel,
                    onClick = onAction,
                    priority = MoneyButtonPriority.Primary,
                    modifier = Modifier.padding(top = MoneySpacing.sm),
                )
            }
        }
    }
}
