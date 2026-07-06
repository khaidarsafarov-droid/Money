package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.util.DebtGamificationEngine
import com.example.money.settings.GamificationState
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.GoldPrimary

@Composable
fun GamificationBanner(
    state: GamificationState,
    modifier: Modifier = Modifier,
) {
    if (state.streakCount <= 0 && state.unlockedBadges.isEmpty() && state.lastWinMessage.isNullOrBlank()) {
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (state.streakCount > 0) {
                Text(
                    text = stringResource(R.string.gamification_streak, state.streakCount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary,
                )
            }
            state.lastWinMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinancePositive,
                )
            }
            if (state.unlockedBadges.isNotEmpty()) {
                Text(
                    text = state.unlockedBadges.joinToString(" · ") { DebtGamificationEngine.badgeLabel(it) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
