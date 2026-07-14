package com.example.money.ui.business.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.money.R
import com.example.money.ui.business.BusinessQuickAction
import com.example.money.ui.business.theme.BusinessColors
import com.example.money.ui.business.theme.BusinessSpacing

@Composable
fun BusinessActionShortcuts(
    onAction: (BusinessQuickAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = listOf(
        BusinessQuickAction.CREATE_INVOICE to Pair(
            stringResource(R.string.business_action_create_invoice),
            Icons.Outlined.Receipt,
        ),
        BusinessQuickAction.ADD_TRANSACTION to Pair(
            stringResource(R.string.business_action_add_transaction),
            Icons.Outlined.AddCard,
        ),
        BusinessQuickAction.CREATE_PROJECT to Pair(
            stringResource(R.string.business_action_create_project),
            Icons.Outlined.FolderOpen,
        ),
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
    ) {
        items(actions) { (action, labelAndIcon) ->
            val (label, icon) = labelAndIcon
            if (action == BusinessQuickAction.CREATE_INVOICE) {
                Button(
                    onClick = { onAction(action) },
                    colors = ButtonDefaults.buttonColors(containerColor = BusinessColors.PrimaryBlue),
                ) {
                    Icon(icon, contentDescription = null)
                    Text(label, modifier = Modifier.padding(start = BusinessSpacing.xs))
                }
            } else {
                OutlinedButton(onClick = { onAction(action) }) {
                    Icon(icon, contentDescription = null)
                    Text(label, modifier = Modifier.padding(start = BusinessSpacing.xs))
                }
            }
        }
    }
}
