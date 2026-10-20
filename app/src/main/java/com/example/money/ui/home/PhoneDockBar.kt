package com.example.money.ui.home

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.money.R

internal enum class PhoneDockTab {
    Home,
    Debts,
    Goals,
    More,
}

@Composable
internal fun PhoneDockBar(
    selected: PhoneDockTab?,
    onHome: () -> Unit,
    onDebts: () -> Unit,
    onGoals: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        DockItem(PhoneDockTab.Home, selected, stringResource(R.string.nav_home), Icons.Rounded.Home, onHome)
        DockItem(PhoneDockTab.Debts, selected, stringResource(R.string.tab_debts), Icons.Rounded.People, onDebts)
        DockItem(PhoneDockTab.Goals, selected, stringResource(R.string.section_goals), Icons.Rounded.Flag, onGoals)
        DockItem(PhoneDockTab.More, selected, stringResource(R.string.nav_more), Icons.Rounded.MoreHoriz, onMore)
    }
}

@Composable
private fun RowScope.DockItem(
    tab: PhoneDockTab,
    selected: PhoneDockTab?,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected == tab,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
            )
        },
        label = {
            Text(text = label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        },
    )
}
