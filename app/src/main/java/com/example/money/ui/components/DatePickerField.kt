package com.example.money.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.example.money.ui.util.Formatters
import java.util.Calendar

@Composable
fun DatePickerField(
    label: String,
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    fun openDatePicker() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onDateSelected(picked.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    OutlinedTextField(
        value = Formatters.date(selectedDateMillis),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
        interactionSource = interactionSource,
        trailingIcon = {
            IconButton(onClick = { openDatePicker() }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = label,
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(selectedDateMillis) {
                detectTapGestures(onTap = { openDatePicker() })
            },
    )
}
