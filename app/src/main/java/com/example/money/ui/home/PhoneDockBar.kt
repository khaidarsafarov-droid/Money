package com.example.money.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.ui.theme.MoneyRadius
import com.example.money.ui.theme.MoneySpacing

internal enum class PhoneDockTab {
    Home,
    Goals,
    Debts,
    Notes,
}

@Composable
internal fun PhoneDockBar(
    selected: PhoneDockTab?,
    onHome: () -> Unit,
    onGoals: () -> Unit,
    onDebts: () -> Unit,
    onNotes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(MoneyRadius.xl),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                DockItem(
                    label = stringResource(R.string.nav_home),
                    icon = Icons.Rounded.Home,
                    selected = selected == PhoneDockTab.Home,
                    onClick = onHome,
                )
                DockItem(
                    label = stringResource(R.string.section_goals),
                    icon = Icons.Rounded.Flag,
                    selected = selected == PhoneDockTab.Goals,
                    onClick = onGoals,
                )
                DockItem(
                    label = stringResource(R.string.tab_debts),
                    icon = Icons.Rounded.People,
                    selected = selected == PhoneDockTab.Debts,
                    onClick = onDebts,
                )
                DockItem(
                    label = stringResource(R.string.tab_notes),
                    icon = Icons.Rounded.EditNote,
                    selected = selected == PhoneDockTab.Notes,
                    onClick = onNotes,
                )
            }
        }
    }
}

@Composable
private fun DockItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = MoneySpacing.minTouchTarget, minHeight = MoneySpacing.minTouchTarget)
            .clip(RoundedCornerShape(MoneyRadius.md))
            .semantics { this.selected = selected }
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = label,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
}
