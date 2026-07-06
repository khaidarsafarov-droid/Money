package com.example.money.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.sync.SyncConnectionState
import com.example.money.domain.sync.SyncStatus
import com.example.money.ui.theme.FinancePositive

@Composable
fun SyncStatusIndicator(
    state: SyncConnectionState,
    modifier: Modifier = Modifier,
) {
    val description = when (state.status) {
        SyncStatus.Offline -> stringResource(R.string.sync_status_offline)
        SyncStatus.Discovering -> stringResource(R.string.sync_status_discovering)
        SyncStatus.PeerAvailable -> stringResource(R.string.sync_status_peer_available)
        SyncStatus.Syncing -> stringResource(R.string.sync_status_syncing)
        SyncStatus.Synced -> stringResource(R.string.sync_status_synced)
    }
    val color = if (state.isIndicatorActive) {
        FinancePositive
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
            .semantics { contentDescription = description },
    )
}
