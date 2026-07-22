package com.example.money.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.settings.AutopilotSettings
import com.example.money.settings.GamificationState
import com.example.money.settings.PrivacySettings
import com.example.money.ui.components.DebtorListItem
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.debtors.DebtCalendarTab
import com.example.money.ui.debtors.DebtCheckInSheet
import com.example.money.ui.debtors.DebtContactActions
import com.example.money.ui.debtors.DebtFreedomTab
import com.example.money.ui.debtors.DebtOverviewCard
import com.example.money.ui.debtors.DebtSearchField
import com.example.money.ui.debtors.DebtTab
import com.example.money.ui.debtors.DebtTabRow
import com.example.money.ui.debtors.DebtorFilter
import com.example.money.ui.debtors.DebtorFilterRow
import com.example.money.ui.debtors.GamificationBanner
import com.example.money.ui.theme.MoneySpacing

fun LazyListScope.homeDebtsSection(
    debtTab: DebtTab,
    onDebtTabChange: (DebtTab) -> Unit,
    allDebtors: List<Debtor>,
    debtors: List<Debtor>,
    debtorFilter: DebtorFilter,
    onDebtorFilterChange: (DebtorFilter) -> Unit,
    debtSearchQuery: String,
    onDebtSearchQueryChange: (String) -> Unit,
    freedomProjection: DebtFreedomEngine.FreedomProjection,
    extraMonthlyCents: Long,
    onExtraMonthlyChange: (Long) -> Unit,
    showCheckIn: Boolean,
    onDismissCheckIn: () -> Unit,
    gamificationState: GamificationState,
    privacySettings: PrivacySettings,
    autopilotSettings: AutopilotSettings,
    onOpenAdvisor: () -> Unit,
    onApplyAutopilot: () -> Unit,
    highlightedDebtorId: Long?,
    contactContext: Context,
    onRemindDebtor: (Debtor) -> Unit,
    onAddDebtPayment: (Debtor) -> Unit,
    onAccelerateDebtPayment: (Debtor, Long) -> Unit,
    onEditDebtor: (Debtor) -> Unit,
    onDeleteDebtor: (Debtor) -> Unit,
    onViewContactHistory: (Debtor) -> Unit,
    onSyncCalendar: (Debtor) -> Unit,
    onRemoveCalendar: (Debtor) -> Unit,
    onAddDebt: () -> Unit,
    emptyDebtorsTitle: @Composable (DebtorFilter) -> String,
    emptyDebtorsHint: @Composable (DebtorFilter) -> String,
) {
    item {
        DebtTabRow(
            selectedTab = debtTab,
            onTabSelected = onDebtTabChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = onOpenAdvisor) {
                Text(stringResource(R.string.advisor_open))
            }
            if (autopilotSettings.enabled) {
                TextButton(onClick = onApplyAutopilot) {
                    Text(stringResource(R.string.autopilot_apply_now))
                }
            }
        }
    }
    if (debtTab != DebtTab.Freedom && debtTab != DebtTab.Calendar) {
        if (debtTab == DebtTab.OwesMe || debtTab == DebtTab.IOwe) {
            item {
                DebtOverviewCard(
                    debtors = allDebtors,
                    debtType = when (debtTab) {
                        DebtTab.OwesMe -> DebtType.CREDITOR
                        DebtTab.IOwe -> DebtType.DEBTOR
                        else -> DebtType.DEBTOR
                    },
                    hideAmounts = privacySettings.hideAmounts,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
        }
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
        item {
            GamificationBanner(
                state = gamificationState,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
        }
    }
    if (debtTab == DebtTab.Calendar) {
        item {
            DebtCalendarTab(
                debtors = allDebtors,
                hideAmounts = privacySettings.hideAmounts,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    } else if (debtTab == DebtTab.Freedom) {
        item {
            DebtFreedomTab(
                projection = freedomProjection,
                extraMonthlyCents = extraMonthlyCents,
                onExtraMonthlyChange = onExtraMonthlyChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    } else {
        item {
            DebtorFilterRow(
                selectedFilter = debtorFilter,
                onFilterSelected = onDebtorFilterChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item {
            DebtSearchField(
                query = debtSearchQuery,
                onQueryChange = onDebtSearchQueryChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
        }
        if (debtors.isEmpty()) {
            item {
                EmptyStateMessage(
                    title = emptyDebtorsTitle(debtorFilter),
                    hint = emptyDebtorsHint(debtorFilter),
                    actionLabel = stringResource(R.string.empty_debtors_action),
                    onAction = onAddDebt,
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
                DebtorListItem(
                    debtor = debtor,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    highlighted = isHighlighted,
                    onRemind = { onRemindDebtor(debtor) },
                    onAddPayment = { onAddDebtPayment(debtor) },
                    onAccelerate = { onAccelerateDebtPayment(debtor, accelerateCents) },
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
