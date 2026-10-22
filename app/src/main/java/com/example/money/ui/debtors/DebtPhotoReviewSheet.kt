package com.example.money.ui.debtors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import com.example.money.ui.motion.MotionGlyph
import com.example.money.ui.motion.MotionGlyphIcon
import com.example.money.ui.motion.moneySheetMotion
import com.example.money.ui.util.MoneyHaptic
import com.example.money.ui.util.moneyHaptic
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.money.ui.components.DialogImeEffect
import com.example.money.ui.components.bringIntoViewOnFocus
import com.example.money.ui.util.Formatters
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.money.R
import com.example.money.domain.util.DebtPhotoParser
import com.example.money.domain.util.ImportedDebt
import com.example.money.domain.util.ParsedDebtLine
import com.example.money.ui.theme.MoneySpacing

private data class DraftRow(
    val id: Int,
    val selected: Boolean,
    val name: String,
    val amountText: String,
    val dateText: String,
    val phone: String,
    val nameUnsure: Boolean,
    val amountUnsure: Boolean,
    val dateUnsure: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtPhotoReviewSheet(
    lines: List<ParsedDebtLine>,
    onDismiss: () -> Unit,
    onSave: (List<ImportedDebt>) -> Unit,
) {
    val now = remember { System.currentTimeMillis() }
    val rows = remember(lines) {
        mutableStateListOf<DraftRow>().apply {
            addAll(
                lines.mapIndexed { index, line ->
                    DraftRow(
                        id = index,
                        selected = line.selectedByDefault,
                        name = line.name,
                        amountText = DebtPhotoParser.formatAmount(line.amountCents),
                        dateText = DebtPhotoParser.formatDue(line.dueAt),
                        phone = line.phone,
                        nameUnsure = line.nameUnsure,
                        amountUnsure = line.amountUnsure,
                        dateUnsure = line.dateUnsure,
                    )
                },
            )
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { BottomSheetDefaults.windowInsets },
    ) {
        DialogImeEffect()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .fillMaxHeight(0.96f)
                .moneySheetMotion(onDismiss)
                .padding(horizontal = 20.dp),
        ) {
            val selectedRows = rows.filter { it.selected }
            val selectedCents = selectedRows.sumOf { DebtPhotoParser.parseAmount(it.amountText) ?: 0L }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.photo_import_lines, rows.size),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = stringResource(
                            R.string.photo_import_selected,
                            selectedRows.size,
                            Formatters.currencyFromCents(selectedCents),
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
            if (rows.isEmpty()) {
                Text(
                    text = stringResource(R.string.debt_import_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = MoneySpacing.lg),
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MoneySpacing.md),
            ) {
                items(rows, key = { it.id }, contentType = { "photo-row" }) { row ->
                    DraftEditor(
                        row = row,
                        onChange = { updated ->
                            val index = rows.indexOfFirst { it.id == row.id }
                            if (index >= 0) rows[index] = updated
                        },
                    )
                }
            }
            val saveView = androidx.compose.ui.platform.LocalView.current
            var saved by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            Button(
                onClick = {
                    val imported = mutableListOf<ImportedDebt>()
                    var invalid = false
                    rows.forEachIndexed { index, row ->
                        if (!row.selected) return@forEachIndexed
                        val cents = DebtPhotoParser.parseAmount(row.amountText)
                        val nameBlank = row.name.isBlank()
                        if (cents == null || nameBlank) {
                            invalid = true
                            rows[index] = row.copy(nameUnsure = nameBlank, amountUnsure = cents == null)
                        } else {
                            imported += ImportedDebt(
                                name = row.name.trim(),
                                amountCents = cents,
                                dateDue = DebtPhotoParser.parseDue(row.dateText, now) ?: 0L,
                                phone = row.phone,
                            )
                        }
                    }
                    if (invalid || imported.isEmpty()) {
                        saveView.moneyHaptic(MoneyHaptic.Reject)
                    } else {
                        saved = true
                        saveView.moneyHaptic(MoneyHaptic.Confirm)
                        onSave(imported)
                    }
                },
                enabled = rows.any { it.selected } && !saved,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .heightIn(min = 56.dp),
            ) {
                if (saved) {
                    MotionGlyphIcon(MotionGlyph.Check, tint = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun DraftEditor(
    row: DraftRow,
    onChange: (DraftRow) -> Unit,
) {
    var editing by remember(row.id) { mutableStateOf(row.nameUnsure || row.amountUnsure) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { editing = !editing }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Checkbox(
                checked = row.selected,
                onCheckedChange = { onChange(row.copy(selected = it)) },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.name.ifBlank { stringResource(R.string.debt_import_name) },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (row.nameUnsure) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                if (row.dateText.isNotBlank()) {
                    Text(text = row.dateText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Text(
                text = row.amountText.ifBlank { stringResource(R.string.debt_import_amount) },
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (row.amountUnsure || !row.selected) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        if (editing) {
            OutlinedTextField(
                value = row.name,
                onValueChange = { onChange(row.copy(name = it, nameUnsure = false)) },
                placeholder = { Text(stringResource(R.string.debt_import_name)) },
                isError = row.nameUnsure,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(),
            )
            OutlinedTextField(
                value = row.amountText,
                onValueChange = { onChange(row.copy(amountText = it, amountUnsure = false)) },
                placeholder = { Text(stringResource(R.string.debt_import_amount)) },
                isError = row.amountUnsure,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(),
            )
            OutlinedTextField(
                value = row.dateText,
                onValueChange = { onChange(row.copy(dateText = it, dateUnsure = false)) },
                placeholder = { Text(stringResource(R.string.debt_import_date)) },
                isError = row.dateUnsure,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(),
            )
        }
    }
}
