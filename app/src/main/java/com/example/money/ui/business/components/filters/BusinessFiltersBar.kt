package com.example.money.ui.business.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.money.R
import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessDashboardFilters
import com.example.money.domain.model.business.BusinessFilterPreset
import com.example.money.domain.model.business.BusinessProject
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.domain.model.business.SavedFilterPreset
import com.example.money.ui.business.theme.BusinessSpacing

@Composable
fun BusinessFiltersBar(
    clients: List<BusinessClient>,
    projects: List<BusinessProject>,
    filters: BusinessDashboardFilters,
    savedPresets: List<SavedFilterPreset>,
    onFiltersChange: (BusinessDashboardFilters) -> Unit,
    onApplyPreset: (BusinessFilterPreset) -> Unit,
    onApplySavedPreset: (SavedFilterPreset) -> Unit,
    onSavePreset: (String) -> Unit,
    onDeleteSavedPreset: (String) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
    ) {
        Text(
            stringResource(R.string.business_filters),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
            item {
                PresetChip(
                    label = stringResource(R.string.business_filter_today),
                    selected = filters.periodPreset == BusinessFilterPreset.TODAY,
                    onClick = { onApplyPreset(BusinessFilterPreset.TODAY) },
                )
            }
            item {
                PresetChip(
                    label = stringResource(R.string.business_filter_this_week),
                    selected = filters.periodPreset == BusinessFilterPreset.THIS_WEEK,
                    onClick = { onApplyPreset(BusinessFilterPreset.THIS_WEEK) },
                )
            }
            item {
                PresetChip(
                    label = stringResource(R.string.business_filter_this_month),
                    selected = filters.periodPreset == BusinessFilterPreset.THIS_MONTH &&
                        filters.clientId == null && filters.projectId == null,
                    onClick = { onApplyPreset(BusinessFilterPreset.THIS_MONTH) },
                )
            }
            item {
                PresetChip(
                    label = stringResource(R.string.business_filter_by_client),
                    selected = filters.periodPreset == BusinessFilterPreset.BY_CLIENT,
                    onClick = { onApplyPreset(BusinessFilterPreset.BY_CLIENT) },
                )
            }
            item {
                PresetChip(
                    label = stringResource(R.string.business_filter_by_project),
                    selected = filters.periodPreset == BusinessFilterPreset.BY_PROJECT,
                    onClick = { onApplyPreset(BusinessFilterPreset.BY_PROJECT) },
                )
            }
        }

        if (filters.periodPreset == BusinessFilterPreset.BY_CLIENT) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
                items(clients) { client ->
                    FilterChip(
                        selected = filters.clientId == client.id,
                        onClick = {
                            onFiltersChange(filters.copy(clientId = client.id, projectId = null))
                        },
                        label = { Text(client.name) },
                    )
                }
            }
        }

        if (filters.periodPreset == BusinessFilterPreset.BY_PROJECT) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
                items(projects) { project ->
                    val clientName = clients.find { it.id == project.clientId }?.name ?: "—"
                    FilterChip(
                        selected = filters.projectId == project.id,
                        onClick = {
                            onFiltersChange(filters.copy(projectId = project.id, clientId = null))
                        },
                        label = { Text("${project.name} ($clientName)") },
                    )
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
            item {
                FilterChip(
                    selected = filters.invoiceStatus == null,
                    onClick = { onFiltersChange(filters.copy(invoiceStatus = null)) },
                    label = { Text(stringResource(R.string.business_filter_all_invoices)) },
                )
            }
            items(InvoiceStatus.entries) { status ->
                FilterChip(
                    selected = filters.invoiceStatus == status,
                    onClick = { onFiltersChange(filters.copy(invoiceStatus = status)) },
                    label = { Text(status.name) },
                )
            }
        }

        if (savedPresets.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
                items(savedPresets, key = { it.name }) { preset ->
                    FilterChip(
                        selected = false,
                        onClick = { onApplySavedPreset(preset) },
                        label = { Text(preset.name) },
                    )
                }
            }
        }

        RowActions(
            hasActiveFilters = filters.clientId != null ||
                filters.projectId != null ||
                filters.invoiceStatus != null ||
                filters.periodPreset != BusinessFilterPreset.THIS_MONTH,
            onSavePreset = { showSaveDialog = true },
            onClearFilters = onClearFilters,
        )
    }

    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                onSavePreset(name)
                showSaveDialog = false
            },
        )
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun RowActions(
    hasActiveFilters: Boolean,
    onSavePreset: () -> Unit,
    onClearFilters: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
    ) {
        OutlinedButton(onClick = onSavePreset) {
            Text(stringResource(R.string.business_filter_save_preset))
        }
        if (hasActiveFilters) {
            Button(onClick = onClearFilters) {
                Text(stringResource(R.string.business_filter_clear))
            }
        }
    }
}

@Composable
private fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.business_filter_save_preset)) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.business_filter_preset_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
