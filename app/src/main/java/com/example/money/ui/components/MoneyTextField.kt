package com.example.money.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import com.example.money.ui.util.vibrateLight

@Composable
fun MoneyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester? = null,
    textStyle: TextStyle? = null,
) {
    val fieldModifier = if (focusRequester == null) {
        modifier.fillMaxWidth()
    } else {
        modifier.fillMaxWidth().focusRequester(focusRequester)
    }.bringIntoViewOnFocus()
    val context = LocalContext.current
    val numericKeyboard = keyboardOptions.keyboardType == KeyboardType.Decimal ||
        keyboardOptions.keyboardType == KeyboardType.Number ||
        keyboardOptions.keyboardType == KeyboardType.NumberPassword
    OutlinedTextField(
        value = value,
        onValueChange = { next ->
            if (numericKeyboard && next != value) vibrateLight(context)
            onValueChange(next)
        },
        label = { Text(label) },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = textStyle ?: MaterialTheme.typography.bodyLarge,
        singleLine = true,
        modifier = fieldModifier,
    )
}
