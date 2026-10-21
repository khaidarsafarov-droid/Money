package com.example.money.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.money.ui.util.Formatters
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.util.ClientIdentity
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.settings.AppMode
import com.example.money.settings.AutopilotSettings
import com.example.money.settings.PrivacySettings
import com.example.money.ui.components.DebtorListItem
import com.example.money.ui.components.EmptyIllustration
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.debtors.DebtCalendarTab
import com.example.money.ui.debtors.DebtCheckInSheet
import com.example.money.ui.debtors.DebtContactActions
import com.example.money.ui.debtors.DebtFilterAndSearch
import com.example.money.ui.debtors.DebtSectionTotal
import com.example.money.ui.debtors.DebtTab
import com.example.money.ui.debtors.DebtTabRow
import com.example.money.ui.debtors.DebtorFilter
import com.example.money.ui.debtors.DebtorSort
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
    onOpenCheckIn: () -> Unit = {},
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
    appMode: AppMode = AppMode.PERSONAL,
    showClients: Boolean = false,
    onToggleClients: () -> Unit = {},
    openSearch: Boolean = false,
    onSearchOpened: () -> Unit = {},
) {
    item {
        DebtTabRow(
            selectedTab = debtTab,
            onTabSelected = onDebtTabChange,
            onOpenCalendar = { onDebtTabChange(DebtTab.Calendar) },
            onOpenCheckIn = onOpenCheckIn,
            onApplyAutopilot = if (autopilotSettings.enabled) onApplyAutopilot else null,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    if (debtTab != DebtTab.Calendar) {
        item {
            DebtSectionTotal(
                debtors = allDebtors,
                tab = debtTab,
                hideAmounts = privacySettings.hideAmounts,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
    if (debtTab != DebtTab.Calendar) {
        if (showCheckIn) {
            item {
                DebtCheckInSheet(
                    debtors = allDebtors,
                    hideAmounts = privacySettings.hideAmounts,
                    onDismiss = onDismissCheckIn,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
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
                debtTab = debtTab,
                allDebtors = allDebtors,
                debtorFilter = debtorFilter,
                onDebtorFilterChange = onDebtorFilterChange,
                debtSearchQuery = debtSearchQuery,
                onDebtSearchQueryChange = onDebtSearchQueryChange,
                showClients = showClients && appMode == AppMode.PROFESSIONAL,
                onToggleClients = if (appMode == AppMode.PROFESSIONAL) onToggleClients else null,
                openSearch = openSearch,
                onSearchOpened = onSearchOpened,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        val clientGroups = if (showClients && appMode == AppMode.PROFESSIONAL) {
            val type = if (debtTab == DebtTab.OwesMe) DebtType.CREDITOR else DebtType.DEBTOR
            val groups = ClientIdentity.groups(allDebtors, type, debtSearchQuery)
            val order = DebtorSort.sort(groups.map { it.display }).map { it.id }
            groups.sortedBy { order.indexOf(it.display.id) }
        } else {
            emptyList()
        }
        if (showClients && appMode == AppMode.PROFESSIONAL) {
            item {
                val owed = clientGroups.sumOf { it.display.remainingAmountCents }
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(stringResource(R.string.clients_owed_total), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            Formatters.currencyFromCents(owed, hide = privacySettings.hideAmounts),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.clients_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${clientGroups.size}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (clientGroups.isEmpty()) {
                item {
                    EmptyStateMessage(
                        title = stringResource(R.string.clients_empty),
                        illustration = EmptyIllustration.Debts,
                        modifier = Modifier.padding(horizontal = MoneySpacing.lg, vertical = MoneySpacing.sm),
                    )
                }
            } else {
                items(clientGroups, key = { "client-${it.open.id}" }, contentType = { "client" }) { group ->
                    val motion = systemMotionEnabled(LocalContext.current)
                    DebtorListItem(
                        debtor = group.display,
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = MoneyMotion.spec(motion),
                                placementSpec = MoneyMotion.springSpec(motion),
                                fadeOutSpec = MoneyMotion.spec(motion),
                            )
                            .padding(horizontal = 8.dp),
                        onRemind = { onRemindDebtor(group.open) },
                        onAddPayment = { onAddDebtPayment(group.open) },
                        onOpen = { onOpenDebtor(group.open) },
                        onMarkPaid = { onMarkDebtPaid(group.open) },
                        onEdit = { onEditDebtor(group.open) },
                        onDelete = { onDeleteDebtor(group.open) },
                        hideAmounts = privacySettings.hideAmounts,
                    )
                }
            }
        } else if (debtors.isEmpty()) {
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
            items(
                debtors,
                key = { "debtor-${it.syncId.ifBlank { "id-${it.id}" }}" },
                contentType = { "debtor" },
            ) { debtor ->
                val isHighlighted = debtor.id == highlightedDebtorId
                val accelerateCents = remember(
                    debtor.remainingAmountCents,
                    debtor.targetAmountCents,
                    debtor.currentAmountCents,
                ) {
                    DebtFreedomEngine.suggestAcceleratePaymentCents(
                        remainingCents = debtor.remainingAmountCents,
                        targetCents = debtor.targetAmountCents,
                        currentCents = debtor.currentAmountCents,
                    )
                }
                val motion = systemMotionEnabled(LocalContext.current)
                DebtorListItem(
                    debtor = debtor,
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = MoneyMotion.spec(motion),
                            placementSpec = MoneyMotion.springSpec(motion),
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
