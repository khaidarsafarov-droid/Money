package com.example.money.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.money.R
import com.example.money.ui.theme.GoldPrimary

internal enum class PhoneDockTab {
    Home,
    Operations,
    Debts,
    Advisor,
}

@Composable
internal fun PhoneDockBar(
    selected: PhoneDockTab,
    onHome: () -> Unit,
    onOperations: () -> Unit,
    onDebts: () -> Unit,
    onAdvisor: () -> Unit,
    fab: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50),
            color = Color.White,
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
                    icon = Icons.Outlined.Home,
                    selected = selected == PhoneDockTab.Home,
                    onClick = onHome,
                )
                DockItem(
                    label = stringResource(R.string.tab_operations),
                    icon = Icons.Outlined.SwapHoriz,
                    selected = selected == PhoneDockTab.Operations,
                    onClick = onOperations,
                )
                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    fab()
                }
                DockItem(
                    label = stringResource(R.string.tab_debts),
                    icon = Icons.Outlined.People,
                    selected = selected == PhoneDockTab.Debts,
                    onClick = onDebts,
                )
                DockItem(
                    label = stringResource(R.string.advisor_open),
                    icon = Icons.Outlined.Psychology,
                    selected = selected == PhoneDockTab.Advisor,
                    onClick = onAdvisor,
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
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color(0xFFD9F6E6) else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) GoldPrimary else Color(0xFF5C7D72),
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = label,
                color = if (selected) GoldPrimary else Color(0xFF5C7D72),
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
}
