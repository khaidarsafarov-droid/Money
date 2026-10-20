package com.example.money.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.Debtor
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.settings.AutopilotSettings
import com.example.money.settings.PrivacySettings
import com.example.money.ui.components.DebtorListItem
import com.example.money.ui.components.EmptyIllustration
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.debtors.DebtCalendarTab
import com.example.money.ui.debtors.DebtCheckInSheet
import com.example.money.ui.debtors.DebtContactActions
import com.example.money.ui.debtors.DebtSearchField
import com.example.money.ui.debtors.DebtTab
import com.example.money.ui.debtors.DebtTabRow
import com.example.money.ui.debtors.DebtorFilter
import com.example.money.ui.debtors.DebtorFilterRow
import com.example.money.ui.theme.MoneyMotion
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.util.systemMotionEnabled

@Suppress("UNUSED_PARAMETER")
fun LazyListScope.homeDebtsSection(
    debtTab: DebtTab,
    onDebtTabChange: (DebtTab) -> Unit,
    allDebtors: List<Debtor>,
    debtors: List<Debtor>,
    debtorFilter: DebtorFilter,
    onDebtorFilterChange: (DebtorFilter) -> Unit,
    debtSearchQuery: String,
    onDebtSearchQueryChange: (String) -> Unit,
    showCheckIn: Boolean,
    onDismissCheckIn: () -> Unit,
    privacySettings: PrivacySettings,
    autopilotSettings: AutopilotSettings,
    onApplyAutopilot: () -> Unit,
    calendarPermissionGranted: Boolean = true,
    onOpenCalendarSettings: () -> Unit = {},
    onRequestCalendarPermission: () -> Unit = {},
    highlightedDebtorId: Long?,
    contactContext: Context,
    onRemindDebtor: (Debtor) -> Unit,
    onAddDebtPayment: (Debtor) -> Unit,
    onOpenDebtor: (Debtor) -> Unit = {},
    onAccelerateDebtPayment: (Debtor, Long) -> Unit,
    onMarkDebtPaid: (Debtor) -> Unit = {},
    onEditDebtor: (Debtor) -> Unit,
    onDeleteDebtor: (Debtor) -> Unit,
    onViewContactHistory: (Debtor) -> Unit,
    onSyncCalendar: (Debtor) -> Unit,
    onRemoveCalendar: (Debtor) -> Unit,
    emptyDebtorsTitle: @Composable (DebtorFilter) -> String,
) {
    item {
        DebtTabRow(
            selectedTab = debtTab,
            onTabSelected = onDebtTabChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    if (debtTab != DebtTab.Calendar) {
        if (showCheckIn) {
            item {
                DebtCheckInSheet(
                    debtors = allDebtors,
                    hideAmounts = privacySettings.hideAmounts,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item {
                TextButton(
                    onClick = onDismissCheckIn,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(R.string.action_close))
                }
            }
        }
    }
    if (debtTab == DebtTab.Calendar) {
        item {
            DebtCalendarTab(
                debtors = allDebtors,
                hideAmounts = privacySettings.hideAmounts,
                calendarPermissionGranted = calendarPermissionGranted,
                onOpenCalendarSettings = onOpenCalendarSettings,
                onRequestCalendarPermission = onRequestCalendarPermission,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    } else {
        item {
            DebtFilterAndSearch(
                debtorFilter = debtorFilter,
                onDebtorFilterChange = onDebtorFilterChange,
                debtSearchQuery = debtSearchQuery,
                onDebtSearchQueryChange = onDebtSearchQueryChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (debtors.isEmpty()) {
            item {
                EmptyStateMessage(
                    title = emptyDebtorsTitle(debtorFilter),
                    illustration = if (debtSearchQuery.isNotBlank()) {
                        EmptyIllustration.Search
                    } else {
                        EmptyIllustration.Debts
                    },
                    modifier = Modifier.padding(
                        horizontal = MoneySpacing.lg,
                        vertical = MoneySpacing.sm,
                    ),
                )
            }
        } else {
            items(debtors, key = { "debtor-${it.syncId.ifBlank { "id-${it.id}" }}" }) { debtor ->
                val isHighlighted = debtor.id == highlightedDebtorId
                val accelerateCents = DebtFreedomEngine.suggestAcceleratePaymentCents(
                    remainingCents = debtor.remainingAmountCents,
                    targetCents = debtor.targetAmountCents,
                    currentCents = debtor.currentAmountCents,
                )
                val motion = systemMotionEnabled(LocalContext.current)
                DebtorListItem(
                    debtor = debtor,
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = MoneyMotion.spec(motion),
                            placementSpec = MoneyMotion.spec(motion),
                            fadeOutSpec = MoneyMotion.spec(motion),
                        )
                        .padding(horizontal = 8.dp),
                    highlighted = isHighlighted,
                    onRemind = { onRemindDebtor(debtor) },
                    onAddPayment = { onAddDebtPayment(debtor) },
                    onOpen = { onOpenDebtor(debtor) },
                    onAccelerate = { onAccelerateDebtPayment(debtor, accelerateCents) },
                    onMarkPaid = { onMarkDebtPaid(debtor) },
                    onEdit = { onEditDebtor(debtor) },
                    onDelete = { onDeleteDebtor(debtor) },
                    onViewContactHistory = debtor.contactLookupKey?.let { _ ->
                        { onViewContactHistory(debtor) }
                    },
                    onCalendar = {
                        if (debtor.calendarEventId.isNullOrBlank()) {
                            onSyncCalendar(debtor)
                        } else {
                            onRemoveCalendar(debtor)
                        }
                    },
                    calendarSynced = !debtor.calendarEventId.isNullOrBlank(),
                    hideAmounts = privacySettings.hideAmounts,
                    onCall = debtor.phone.takeIf { it.isNotBlank() }?.let {
                        { DebtContactActions.dialPhone(contactContext, debtor.phone) }
                    },
                    onWhatsApp = debtor.phone.takeIf { it.isNotBlank() }?.let {
                        {
                            DebtContactActions.openWhatsApp(
                                contactContext,
                                debtor.phone,
                                debtor.name,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DebtFilterAndSearch(
    debtorFilter: DebtorFilter,
    onDebtorFilterChange: (DebtorFilter) -> Unit,
    debtSearchQuery: String,
    onDebtSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchOpen by remember { mutableStateOf(debtSearchQuery.isNotBlank()) }
    val showSearch = searchOpen || debtSearchQuery.isNotBlank()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DebtorFilterRow(
                selectedFilter = debtorFilter,
                onFilterSelected = onDebtorFilterChange,
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
            )
        }
    }
}
