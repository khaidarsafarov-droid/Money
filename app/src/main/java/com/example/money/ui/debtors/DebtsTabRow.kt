package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.money.ui.motion.AnimatedAmount
import com.example.money.ui.motion.pressScale
import com.example.money.ui.theme.finoraBalanceBrush
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.util.DebtDueDateUtils
import com.example.money.ui.theme.MoneyRadius
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.util.Formatters

private val directionTabs = listOf(DebtTab.OwesMe, DebtTab.IOwe)

private val listFilters = listOf(
    DebtorFilter.ACTIVE,
    DebtorFilter.DUE_SOON,
    DebtorFilter.OVERDUE,
    DebtorFilter.PAID,
)

@Composable
fun DebtTabRow(
    selectedTab: DebtTab,
    onTabSelected: (DebtTab) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenCheckIn: () -> Unit,
    onApplyAutopilot: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                        )
                    },
                )
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.action_more),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.debt_tab_calendar)) },
                    onClick = {
                        menuOpen = false
                        onOpenCalendar()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.weekly_checkin_title)) },
                    onClick = {
                        menuOpen = false
                        onOpenCheckIn()
                    },
                )
                if (onApplyAutopilot != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.autopilot_apply_now)) },
                        onClick = {
                            menuOpen = false
                            onApplyAutopilot()
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun DebtSectionTotal(
    debtors: List<Debtor>,
    tab: DebtTab,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
) {
    if (tab == DebtTab.Calendar) return
    val type = if (tab == DebtTab.OwesMe) DebtType.CREDITOR else DebtType.DEBTOR
    val amount = androidx.compose.runtime.remember(debtors, tab) {
        debtors.filter { it.debtType == type && !it.isPaid }.sumOf { it.remainingAmountCents }
    }
    val late = overdueCount(debtors, tab)
    Row(
        modifier = modifier
            .pressScale()
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoneyRadius.lg))
            .background(finoraBalanceBrush())
            .padding(horizontal = MoneySpacing.lg, vertical = MoneySpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MoneySpacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(tab.titleRes),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AnimatedAmount(
                text = Formatters.currencyFromCents(amount, hide = hideAmounts),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
        if (late > 0) {
            Text(
                text = stringResource(R.string.debt_filter_overdue_count, late),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun overdueCount(debtors: List<Debtor>, tab: DebtTab, now: Long = System.currentTimeMillis()): Int {
    if (tab == DebtTab.Calendar) return 0
    val type = if (tab == DebtTab.OwesMe) DebtType.CREDITOR else DebtType.DEBTOR
    return debtors.count { debtor ->
        debtor.debtType == type &&
            !debtor.isPaid &&
            debtor.dateDue > 0L &&
            DebtDueDateUtils.isOverdue(debtor.dateDue, now)
    }
}

@Composable
fun DebtorFilterRow(
    selectedFilter: DebtorFilter,
    onFilterSelected: (DebtorFilter) -> Unit,
    overdueCount: Int,
    modifier: Modifier = Modifier,
    hideFilters: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        if (leading != null) {
            item(key = "leading", contentType = "filter") { leading() }
        }
        if (hideFilters) return@LazyRow
        items(listFilters, key = { it.name }, contentType = { "filter" }) { option ->
            val selected = selectedFilter == option
            val label = if (option == DebtorFilter.OVERDUE) {
                stringResource(R.string.debt_filter_overdue_count, overdueCount)
            } else {
                stringResource(option.labelRes)
            }
            FilterChip(
                selected = selected,
                onClick = { onFilterSelected(option) },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = if (option == DebtorFilter.OVERDUE) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
                shape = RoundedCornerShape(MoneyRadius.lg),
            )
        }
    }
}

@Composable
internal fun DebtFilterAndSearch(
    debtTab: DebtTab,
    allDebtors: List<Debtor>,
    debtorFilter: DebtorFilter,
    onDebtorFilterChange: (DebtorFilter) -> Unit,
    debtSearchQuery: String,
    onDebtSearchQueryChange: (String) -> Unit,
    showClients: Boolean = false,
    onToggleClients: (() -> Unit)? = null,
    openSearch: Boolean = false,
    onSearchOpened: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var searchOpen by remember { mutableStateOf(debtSearchQuery.isNotBlank()) }
    val showSearch = searchOpen || debtSearchQuery.isNotBlank()
    androidx.compose.runtime.LaunchedEffect(openSearch) {
        if (openSearch) {
            searchOpen = true
            onSearchOpened()
        }
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MoneySpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
        ) {
            DebtorFilterRow(
                selectedFilter = debtorFilter,
                onFilterSelected = onDebtorFilterChange,
                overdueCount = overdueCount(allDebtors, debtTab),
                hideFilters = showClients,
                leading = onToggleClients?.let { toggle ->
                    {
                        FilterChip(
                            selected = showClients,
                            onClick = toggle,
                            label = {
                                Text(
                                    text = stringResource(R.string.clients_title),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                },
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    if (showSearch) {
                        searchOpen = false
                        if (debtSearchQuery.isNotBlank()) onDebtSearchQueryChange("")
                    } else {
                        searchOpen = true
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(R.string.debt_search_hint),
                )
            }
        }
        if (showSearch) {
            DebtSearchField(
                query = debtSearchQuery,
                onQueryChange = onDebtSearchQueryChange,
                requestFocus = openSearch,
            )
        }
    }
}
