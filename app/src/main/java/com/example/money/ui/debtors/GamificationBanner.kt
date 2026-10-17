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
import com.example.money.settings.GamificationBadges
import com.example.money.settings.GamificationState
import com.example.money.ui.theme.FinancePositive

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            val winMessage = state.lastWinMessage
            if (winMessage != null) {
                Text(
                    text = winMessageText(winMessage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinancePositive,
                )
            }
            if (state.unlockedBadges.isNotEmpty()) {
                Text(
                    text = badgeLine(state.unlockedBadges),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun winMessageText(code: String): String {
    val milestone = code.removePrefix("win.milestone.").toIntOrNull()
    val streak = code.removePrefix("win.streak.").toIntOrNull()
    return when {
        code == "win.closed" -> stringResource(R.string.gamification_win_closed)
        code.startsWith("win.milestone.") && milestone != null ->
            stringResource(R.string.gamification_win_milestone, milestone)
        code.startsWith("win.streak.") && streak != null ->
            stringResource(R.string.gamification_win_streak, streak)
        else -> code
    }
}

@Composable
private fun badgeLine(badgeIds: Set<String>): String {
    val firstStep = stringResource(R.string.gamification_badge_first_step)
    val halfWay = stringResource(R.string.gamification_badge_half_way)
    val freedom = stringResource(R.string.gamification_badge_freedom)
    val streak = stringResource(R.string.gamification_badge_streak_3)
    return badgeIds.joinToString(" · ") { badgeId ->
        when (badgeId) {
            GamificationBadges.FIRST_STEP -> firstStep
            GamificationBadges.HALF_WAY -> halfWay
            GamificationBadges.FREEDOM -> freedom
            GamificationBadges.STREAK_3 -> streak
            else -> badgeId
        }
    }
}
