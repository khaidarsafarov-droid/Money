package com.example.money.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
    hint: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    illustration: EmptyIllustration = EmptyIllustration.General,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
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
            EmptyStateArtwork(kind = illustration, icon = icon)
            Text(
                text = title,
                style = moneyHeadingStyle(),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = hint,
                style = moneyBodyStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
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

@Composable
private fun EmptyStateArtwork(
    kind: EmptyIllustration,
    icon: ImageVector?,
) {
    Box(
        modifier = Modifier.size(88.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        )
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            when (kind) {
                EmptyIllustration.Debts -> DebtMark()
                EmptyIllustration.Goals -> GoalMark()
                EmptyIllustration.Notes -> NoteMark()
                EmptyIllustration.Search -> SearchMark()
                EmptyIllustration.General -> {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtMark() {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .offset(x = (-8).dp)
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        )
        Box(
            modifier = Modifier
                .offset(x = 10.dp, y = 8.dp)
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
        )
    }
}

@Composable
private fun GoalMark() {
    Box(contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
        Box(
            modifier = Modifier
                .offset(x = 4.dp, y = (-12).dp)
                .size(width = 22.dp, height = 14.dp)
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun NoteMark() {
    Column(
        modifier = Modifier
            .size(width = 34.dp, height = 42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            )
        }
    }
}

@Composable
private fun SearchMark() {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
        )
        Box(
            modifier = Modifier
                .offset(x = 14.dp, y = 14.dp)
                .width(14.dp)
                .height(3.dp)
                .rotate(45f)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
