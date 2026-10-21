package com.example.money.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.money.R
import com.example.money.ui.motion.pressScale
import com.example.money.ui.theme.MoneyMotion
import com.example.money.ui.util.systemMotionEnabled

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
    val scheme = MaterialTheme.colorScheme
    Surface(modifier = modifier, color = scheme.surface) {
        Column {
            HorizontalDivider(color = scheme.outlineVariant, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockItem(PhoneDockTab.Home, selected, stringResource(R.string.nav_home), Icons.Outlined.Home, Icons.Filled.Home, onHome)
                DockItem(PhoneDockTab.Debts, selected, stringResource(R.string.tab_debts), Icons.Outlined.Handshake, Icons.Filled.Handshake, onDebts)
                DockItem(PhoneDockTab.Goals, selected, stringResource(R.string.section_goals), Icons.Outlined.Flag, Icons.Filled.Flag, onGoals)
                DockItem(PhoneDockTab.More, selected, stringResource(R.string.nav_more), Icons.Outlined.MoreHoriz, Icons.Filled.MoreHoriz, onMore)
            }
        }
    }
}

@Composable
private fun RowScope.DockItem(
    tab: PhoneDockTab,
    selected: PhoneDockTab?,
    label: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    onClick: () -> Unit,
) {
    val on = selected == tab
    val scheme = MaterialTheme.colorScheme
    val motion = systemMotionEnabled(LocalContext.current)
    val iconScale by animateFloatAsState(
        targetValue = if (on) 1f else 0.92f,
        animationSpec = MoneyMotion.pressSpec(motion),
        label = "dockIcon",
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .pressScale()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(if (on) scheme.primaryContainer else scheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = on,
                animationSpec = MoneyMotion.spec(motion, MoneyMotion.short),
                label = "dockGlyph",
            ) { selectedNow ->
                Icon(
                    imageVector = if (selectedNow) selectedIcon else icon,
                    contentDescription = label,
                    tint = if (on) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
            color = if (on) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
