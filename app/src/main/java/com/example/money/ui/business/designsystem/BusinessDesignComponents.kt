package com.example.money.ui.business.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.money.ui.business.theme.BusinessSpacing

@Composable
fun BusinessScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BusinessDesignTokens.RadiusHero))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        BusinessDesignTokens.AccentBlue.copy(alpha = 0.12f),
                        BusinessDesignTokens.AccentViolet.copy(alpha = 0.10f),
                    ),
                ),
            )
            .padding(BusinessDesignTokens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(BusinessDesignTokens.SpaceXs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = BusinessDesignTokens.TextPrimary,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = BusinessDesignTokens.TextMuted,
        )
    }
}

@Composable
fun BusinessCategoryCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                if (selected) BusinessDesignTokens.ElevationRaised else BusinessDesignTokens.ElevationCard,
                RoundedCornerShape(BusinessDesignTokens.RadiusCard),
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(BusinessDesignTokens.RadiusCard),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accentColor.copy(alpha = 0.08f) else BusinessDesignTokens.SurfaceWhite,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(BusinessDesignTokens.SpaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BusinessDesignTokens.SpaceMd),
        ) {
            Box(
                modifier = Modifier
                    .size(BusinessDesignTokens.MinTouchTarget)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = BusinessDesignTokens.TextMuted)
            }
        }
    }
}

@Composable
fun BusinessListRow(
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(BusinessDesignTokens.ElevationCard, RoundedCornerShape(BusinessDesignTokens.RadiusCard))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(BusinessDesignTokens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = BusinessDesignTokens.SurfaceWhite),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(BusinessDesignTokens.SpaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = BusinessDesignTokens.AccentBlue,
                    modifier = Modifier.padding(end = BusinessDesignTokens.SpaceMd),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = BusinessDesignTokens.TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!trailing.isNullOrBlank()) {
                Text(
                    trailing,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BusinessDesignTokens.AccentBlue,
                )
            }
        }
    }
}

@Composable
fun BusinessFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        shape = RoundedCornerShape(BusinessDesignTokens.RadiusButton),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BusinessDesignTokens.AccentBlue,
            cursorColor = BusinessDesignTokens.AccentBlue,
        ),
    )
}

@Composable
fun BusinessPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BusinessDesignTokens.RadiusButton),
        colors = ButtonDefaults.buttonColors(
            containerColor = BusinessDesignTokens.AccentBlue,
            contentColor = Color.White,
        ),
    ) {
        Text(text)
    }
}

@Composable
fun BusinessSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BusinessDesignTokens.RadiusButton),
    ) {
        Text(text)
    }
}

@Composable
fun BusinessStatusChip(
    label: String,
    color: Color,
) {
    Surface(shape = RoundedCornerShape(BusinessDesignTokens.RadiusChip), color = color.copy(alpha = 0.15f)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun BusinessDebtTableRow(
    name: String,
    amount: String,
    statusLabel: String,
    statusColor: Color,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    markPaidLabel: String?,
    onMarkPaid: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BusinessDesignTokens.RadiusCard),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                BusinessDesignTokens.AccentBlue.copy(alpha = 0.08f)
            } else {
                BusinessDesignTokens.SurfaceWhite
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) BusinessDesignTokens.ElevationRaised else BusinessDesignTokens.ElevationCard,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onToggle() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = selected, onCheckedChange = { if (enabled) onToggle() }, enabled = enabled)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(amount, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = BusinessDesignTokens.AccentBlue)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BusinessDesignTokens.TextMuted)
                }
                BusinessStatusChip(label = statusLabel, color = statusColor)
            }
            if (markPaidLabel != null && onMarkPaid != null) {
                TextButton(onClick = onMarkPaid) { Text(markPaidLabel) }
            }
        }
    }
}

@Composable
fun BusinessMessageComposerPanel(
    message: String,
    onMessageChange: (String) -> Unit,
    messageLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BusinessDesignTokens.RadiusCard),
        elevation = CardDefaults.cardElevation(defaultElevation = BusinessDesignTokens.ElevationRaised),
        colors = CardDefaults.cardColors(containerColor = BusinessDesignTokens.SurfaceWhite),
    ) {
        Column(
            modifier = Modifier.padding(BusinessDesignTokens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(BusinessDesignTokens.SpaceSm),
        ) {
            BusinessFormField(
                value = message,
                onValueChange = onMessageChange,
                label = messageLabel,
                minLines = BusinessDesignTokens.ComposerMinLines,
            )
            content()
        }
    }
}

@Composable
fun BusinessChannelCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BusinessListRow(
        title = title,
        subtitle = subtitle,
        leadingIcon = icon,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun BusinessEmptyState(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(BusinessDesignTokens.SpaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BusinessDesignTokens.SpaceMd),
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = BusinessDesignTokens.TextMuted)
        if (actionLabel != null && onAction != null) {
            BusinessSecondaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun BusinessHistoryRow(
    channel: String,
    recipient: String,
    status: String,
    modifier: Modifier = Modifier,
) {
    BusinessListRow(
        title = "$channel → $recipient",
        subtitle = status,
        onClick = {},
        modifier = modifier,
    )
}

@Composable
fun BusinessOutlineActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(BusinessDesignTokens.RadiusButton),
    ) {
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
