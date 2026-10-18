package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.ui.theme.MoneySpacing

private val directionTabs = listOf(DebtTab.OwesMe, DebtTab.IOwe)

private val listFilters = listOf(DebtorFilter.ALL, DebtorFilter.ACTIVE, DebtorFilter.OVERDUE)

/**
 * Direction of the debt (owed to me / I owe) plus text links to forecast and calendar.
 * Forecast and calendar are screens of this section, not a second chip row.
 */
@Composable
fun DebtTabRow(
    selectedTab: DebtTab,
    onTabSelected: (DebtTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MoneySpacing.xs),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            directionTabs.forEachIndexed { index, tab ->
                SegmentedButton(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = directionTabs.size),
                    icon = {},
                    label = {
                        Text(
                            text = stringResource(tab.titleRes),
                            fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MoneySpacing.sm)) {
            TextButton(onClick = { onTabSelected(DebtTab.Calendar) }) {
                Text(
                    text = stringResource(DebtTab.Calendar.titleRes),
                    fontWeight = if (selectedTab == DebtTab.Calendar) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selectedTab == DebtTab.Calendar) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
fun DebtorFilterRow(
    selectedFilter: DebtorFilter,
    onFilterSelected: (DebtorFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        items(listFilters) { option ->
            val selected = selectedFilter == option
            FilterChip(
                selected = selected,
                onClick = { onFilterSelected(option) },
                label = {
                    Text(
                        text = stringResource(option.labelRes),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
                shape = RoundedCornerShape(20.dp),
            )
        }
    }
}
