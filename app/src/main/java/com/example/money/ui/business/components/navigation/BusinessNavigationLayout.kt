package com.example.money.ui.business.components.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.ui.business.BusinessTab

private val TabletBreakpoint = 600.dp

@Composable
fun BusinessNavigationLayout(
    selectedTab: BusinessTab,
    onTabSelected: (BusinessTab) -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= TabletBreakpoint) {
            Row(modifier = Modifier.fillMaxSize()) {
                BusinessSidebar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                )
                Column(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    edgePadding = 8.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BusinessTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab.ordinal == index,
                            onClick = { onTabSelected(tab) },
                            text = { Text(tabLabel(tab)) },
                        )
                    }
                }
                content()
            }
        }
    }
}

@Composable
private fun tabLabel(tab: BusinessTab): String = stringResource(
    when (tab) {
        BusinessTab.Dashboard -> R.string.business_tab_dashboard
        BusinessTab.Clients -> R.string.business_tab_clients
        BusinessTab.Projects -> R.string.business_tab_projects
        BusinessTab.Invoices -> R.string.business_tab_invoices
        BusinessTab.Cashflow -> R.string.business_tab_cashflow
        BusinessTab.Reports -> R.string.business_tab_reports
        BusinessTab.Assistant -> R.string.business_tab_assistant
        BusinessTab.Sms -> R.string.business_tab_sms
        BusinessTab.Settings -> R.string.business_tab_settings
    },
)
