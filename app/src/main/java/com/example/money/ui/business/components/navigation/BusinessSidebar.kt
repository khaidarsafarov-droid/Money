package com.example.money.ui.business.components.navigation

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.money.R
import com.example.money.ui.business.BusinessTab

@Composable
fun BusinessSidebar(
    selectedTab: BusinessTab,
    onTabSelected: (BusinessTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        BusinessTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(tab.titleRes())) },
            )
        }
    }
}

private fun BusinessTab.icon(): ImageVector = when (this) {
    BusinessTab.Dashboard -> Icons.Outlined.Dashboard
    BusinessTab.Clients -> Icons.Outlined.People
    BusinessTab.Projects -> Icons.Outlined.Folder
    BusinessTab.Invoices -> Icons.Outlined.Receipt
    BusinessTab.Cashflow -> Icons.AutoMirrored.Outlined.TrendingUp
    BusinessTab.Reports -> Icons.Outlined.Assessment
    BusinessTab.Assistant -> Icons.Outlined.SmartToy
    BusinessTab.Settings -> Icons.Outlined.Settings
}

private fun BusinessTab.titleRes(): Int = when (this) {
    BusinessTab.Dashboard -> R.string.business_tab_dashboard
    BusinessTab.Clients -> R.string.business_tab_clients
    BusinessTab.Projects -> R.string.business_tab_projects
    BusinessTab.Invoices -> R.string.business_tab_invoices
    BusinessTab.Cashflow -> R.string.business_tab_cashflow
    BusinessTab.Reports -> R.string.business_tab_reports
    BusinessTab.Assistant -> R.string.business_tab_assistant
    BusinessTab.Settings -> R.string.business_tab_settings
}
