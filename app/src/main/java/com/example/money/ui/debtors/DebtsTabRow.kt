package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.theme.StatusActiveBg

@Composable
fun DebtTabRow(
    selectedTab: DebtTab,
    onTabSelected: (DebtTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        items(DebtTab.entries) { tab ->
            val selected = selectedTab == tab
            FilterChip(
                selected = selected,
                onClick = { onTabSelected(tab) },
                label = {
                    Text(
                        text = stringResource(tab.titleRes),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusActiveBg,
                    selectedLabelColor = GoldPrimary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = GoldPrimary,
                ),
                shape = RoundedCornerShape(20.dp),
            )
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
        items(DebtorFilter.entries) { option ->
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
                    selectedContainerColor = StatusActiveBg,
                    selectedLabelColor = GoldPrimary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = GoldPrimary,
                ),
                shape = RoundedCornerShape(20.dp),
            )
        }
    }
}
