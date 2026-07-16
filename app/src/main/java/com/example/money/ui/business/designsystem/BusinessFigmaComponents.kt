package com.example.money.ui.business.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Figma frame `13_Components` — canonical names for the Business module UI library.
 *
 * @see docs/business/FIGMA_STRUCTURE.md
 */

// --- Headers (12_DesignSystem / 13_Components) ---

@Composable
fun HeaderPrimary(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    BusinessScreenHeader(title = title, subtitle = subtitle, modifier = modifier)
}

@Composable
fun HeaderSecondary(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BusinessDesignTokens.SpaceXs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = BusinessDesignTokens.TextPrimary,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = BusinessDesignTokens.TextMuted,
            )
        }
    }
}

// --- Buttons ---

@Composable
fun ButtonPrimary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BusinessPrimaryButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
}

@Composable
fun ButtonSecondary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BusinessSecondaryButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
}

@Composable
fun ButtonAi(
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
        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text, modifier = Modifier.padding(start = BusinessDesignTokens.SpaceSm))
    }
}

// --- Inputs ---

@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
) {
    BusinessFormField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        minLines = minLines,
    )
}

@Composable
fun InputNumber(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    BusinessFormField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        minLines = 1,
    )
}

@Composable
fun TextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = BusinessDesignTokens.InputMinLines,
) {
    BusinessFormField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        minLines = minLines,
    )
}

@Composable
fun TextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    BusinessFormField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        minLines = BusinessDesignTokens.ComposerMinLines,
    )
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(BusinessDesignTokens.RadiusButton),
    )
}

// --- Cards & list items ---

@Composable
fun CategoryCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    BusinessCategoryCard(
        title = title,
        description = description,
        icon = icon,
        accentColor = accentColor,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun ProcessListItem(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    BusinessListRow(
        title = title,
        subtitle = subtitle,
        leadingIcon = leadingIcon,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun ContactListItem(
    name: String,
    phone: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    showWhatsApp: Boolean = phone.isNotBlank(),
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BusinessDesignTokens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = BusinessDesignTokens.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = BusinessDesignTokens.ShadowSoft),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(BusinessDesignTokens.SpaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BusinessDesignTokens.SpaceSm),
        ) {
            androidx.compose.material3.Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (phone.isNotBlank()) {
                    Text(phone, style = MaterialTheme.typography.bodySmall, color = BusinessDesignTokens.TextMuted)
                }
            }
            if (showWhatsApp) {
                IconWhatsApp(modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun DebtRow(
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
    BusinessDebtTableRow(
        name = name,
        amount = amount,
        statusLabel = statusLabel,
        statusColor = statusColor,
        selected = selected,
        enabled = enabled,
        onToggle = onToggle,
        markPaidLabel = markPaidLabel,
        onMarkPaid = onMarkPaid,
        modifier = modifier,
        subtitle = subtitle,
    )
}

@Composable
fun ChannelCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BusinessChannelCard(title = title, subtitle = subtitle, icon = icon, onClick = onClick, modifier = modifier)
}

@Composable
fun ShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ChannelCard(title = title, subtitle = subtitle, icon = icon, onClick = onClick, modifier = modifier)
}

@Composable
fun HistoryRow(
    channel: String,
    recipient: String,
    body: String,
    status: String,
    dateLabel: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BusinessDesignTokens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = BusinessDesignTokens.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = BusinessDesignTokens.ShadowSoft),
    ) {
        Column(
            modifier = Modifier.padding(BusinessDesignTokens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(BusinessDesignTokens.SpaceXs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(recipient, fontWeight = FontWeight.SemiBold)
                StatusTag(label = status)
            }
            Text(
                text = "$channel · $dateLabel",
                style = MaterialTheme.typography.labelSmall,
                color = BusinessDesignTokens.TextMuted,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TemplateItem(
    title: String,
    preview: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = BusinessDesignTokens.SpaceXs)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(BusinessDesignTokens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = BusinessDesignTokens.SurfaceWhite),
    ) {
        Column(modifier = Modifier.padding(BusinessDesignTokens.SpaceMd)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                preview,
                style = MaterialTheme.typography.bodySmall,
                color = BusinessDesignTokens.TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// --- Automation (09_Automation) ---

@Composable
fun AutomationItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Toggle(label = label, checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
}

@Composable
fun Toggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun StatusTag(
    label: String,
    color: Color = BusinessDesignTokens.AccentBlue,
) {
    BusinessStatusChip(label = label, color = color)
}

@Composable
fun FilterTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BusinessDesignTokens.SpaceSm),
    ) {
        labels.forEachIndexed { index, label ->
            FilterChip(
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
fun ChannelSelector(
    channels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterTabs(
        labels = channels,
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        modifier = modifier,
    )
}

// --- Icons (13_Components) ---

@Composable
fun IconWhatsApp(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Outlined.Store,
        contentDescription = null,
        tint = Color(0xFF25D366),
        modifier = modifier,
    )
}

@Composable
fun IconSms(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Outlined.Sms,
        contentDescription = null,
        tint = BusinessDesignTokens.AccentBlue,
        modifier = modifier,
    )
}

@Composable
fun IconPush(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Outlined.Notifications,
        contentDescription = null,
        tint = BusinessDesignTokens.AccentViolet,
        modifier = modifier,
    )
}
