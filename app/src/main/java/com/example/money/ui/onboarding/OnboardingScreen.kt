package com.example.money.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money.MoneyApplication
import com.example.money.R
import com.example.money.domain.util.GoalPsychologyEngines
import com.example.money.settings.UserEmotion
import com.example.money.settings.UserGender
import com.example.money.ui.components.FinOrb
import com.example.money.ui.home.WelcomePreferences
import com.example.money.ui.theme.CreamBackground
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.GoldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.Factory(app.preferencesStore, app.debtorRepository),
    )

    val step by viewModel.step.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val debtCount by viewModel.activeDebtCount.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = CreamBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                stringResource(R.string.onboarding_step, step + 1, OnboardingViewModel.TOTAL_STEPS),
                style = MaterialTheme.typography.labelLarge,
                color = GoldPrimary,
            )
            LinearProgressIndicator(
                progress = { (step + 1).toFloat() / OnboardingViewModel.TOTAL_STEPS },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                color = FinancePositive,
            )

            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                },
                label = "onboarding_step",
            ) { currentStep ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (currentStep) {
                        0 -> WelcomeStep(onNext = viewModel::nextStep)
                        1 -> NameStep(viewModel = viewModel, draft = draft)
                        2 -> PsychotypeStep(viewModel = viewModel, draft = draft, onNext = viewModel::nextStep)
                        3 -> FearsStep(viewModel = viewModel, draft = draft)
                        4 -> GoalStep(viewModel = viewModel, draft = draft)
                        5 -> SummaryStep(
                            draft = draft,
                            debtCount = debtCount,
                            onFinish = {
                                WelcomePreferences.markWelcomeSeen(context)
                                viewModel.completeOnboarding(onComplete)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    OnboardingCard {
        FinOrb(modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(stringResource(R.string.onboarding_welcome_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.onboarding_welcome_body), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_start))
        }
    }
}

@Composable
private fun NameStep(viewModel: OnboardingViewModel, draft: OnboardingDraft) {
    OnboardingCard {
        Text(stringResource(R.string.onboarding_name_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = draft.displayName,
            onValueChange = viewModel::updateName,
            label = { Text(stringResource(R.string.onboarding_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Text(stringResource(R.string.onboarding_gender_label), fontWeight = FontWeight.Medium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = draft.gender == UserGender.MALE,
                onClick = { viewModel.updateGender(UserGender.MALE) },
                label = { Text(stringResource(R.string.onboarding_gender_male)) },
            )
            FilterChip(
                selected = draft.gender == UserGender.FEMALE,
                onClick = { viewModel.updateGender(UserGender.FEMALE) },
                label = { Text(stringResource(R.string.onboarding_gender_female)) },
            )
            FilterChip(
                selected = draft.gender == UserGender.UNSPECIFIED,
                onClick = { viewModel.updateGender(UserGender.UNSPECIFIED) },
                label = { Text(stringResource(R.string.onboarding_gender_skip)) },
            )
        }
        OutlinedTextField(
            value = draft.ageText,
            onValueChange = viewModel::updateAgeText,
            label = { Text(stringResource(R.string.onboarding_age_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (viewModel.isValidName()) {
            Text(
                stringResource(R.string.onboarding_name_preview, draft.displayName.trim()),
                color = FinancePositive,
                fontWeight = FontWeight.Medium,
            )
        }
        Button(
            onClick = viewModel::nextStep,
            enabled = viewModel.isValidName() && viewModel.isValidAge(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_next))
        }
    }
}

@Composable
private fun PsychotypeStep(
    viewModel: OnboardingViewModel,
    draft: OnboardingDraft,
    onNext: () -> Unit,
) {
    val questions = listOf(
        R.string.psychotype_q1 to listOf(
            'a' to R.string.psychotype_q1_a,
            'b' to R.string.psychotype_q1_b,
            'c' to R.string.psychotype_q1_c,
            'd' to R.string.psychotype_q1_d,
        ),
        R.string.psychotype_q2 to listOf(
            'a' to R.string.psychotype_q2_a,
            'b' to R.string.psychotype_q2_b,
            'c' to R.string.psychotype_q2_c,
            'd' to R.string.psychotype_q2_d,
        ),
        R.string.psychotype_q3 to listOf(
            'a' to R.string.psychotype_q3_a,
            'b' to R.string.psychotype_q3_b,
            'c' to R.string.psychotype_q3_c,
            'd' to R.string.psychotype_q3_d,
        ),
        R.string.psychotype_q4 to listOf(
            'a' to R.string.psychotype_q4_a,
            'b' to R.string.psychotype_q4_b,
            'c' to R.string.psychotype_q4_c,
            'd' to R.string.psychotype_q4_d,
        ),
        R.string.psychotype_q5 to listOf(
            'a' to R.string.psychotype_q5_a,
            'b' to R.string.psychotype_q5_b,
            'c' to R.string.psychotype_q5_c,
            'd' to R.string.psychotype_q5_d,
        ),
    )

    OnboardingCard {
        val psychotype = draft.psychotypeResult
        if (draft.showPsychotypeResult && psychotype != null) {
            Text(stringResource(R.string.onboarding_psychotype_result), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(
                    R.string.goal_hub_psychotype,
                    GoalPsychologyEngines.psychotypeLabel(psychotype),
                ),
                color = GoldPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(GoalPsychologyEngines.psychotypeOnboardingDescription(psychotype))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_next))
            }
        } else {
            val index = draft.psychotypeQuestionIndex
            val (questionRes, options) = questions[index]
            Text(
                stringResource(R.string.psychotype_test_title, index + 1, questions.size),
                style = MaterialTheme.typography.labelLarge,
                color = GoldPrimary,
            )
            Text(stringResource(questionRes), style = MaterialTheme.typography.titleMedium)
            options.forEach { (key, labelRes) ->
                FilterChip(
                    selected = draft.psychotypeAnswers[index] == key,
                    onClick = { viewModel.selectPsychotypeAnswer(index, key) },
                    label = { Text(stringResource(labelRes)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(
                onClick = viewModel::nextPsychotypeQuestion,
                enabled = draft.psychotypeAnswers[index] != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (index < questions.lastIndex) {
                        stringResource(R.string.action_next)
                    } else {
                        stringResource(R.string.onboarding_show_result)
                    },
                )
            }
        }
    }
}

@Composable
private fun FearsStep(viewModel: OnboardingViewModel, draft: OnboardingDraft) {
    val options = listOf(
        "wont_escape" to R.string.fear_wont_escape,
        "earn_more_than_parents" to R.string.fear_earn_more_parents,
        "ashamed_of_debt" to R.string.fear_ashamed_debt,
        "money_corrupts" to R.string.fear_money_corrupts,
        "rich_are_bad" to R.string.fear_rich_bad,
        "afraid_of_numbers" to R.string.fear_afraid_numbers,
    )

    OnboardingCard {
        Text(stringResource(R.string.onboarding_fears_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        options.forEach { (id, labelRes) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = id in draft.selectedFearIds,
                    onCheckedChange = { checked ->
                        val next = draft.selectedFearIds.toMutableSet()
                        if (checked) next.add(id) else next.remove(id)
                        viewModel.updateFears(next)
                    },
                )
                Text(stringResource(labelRes), style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedTextField(
            value = draft.customFear,
            onValueChange = viewModel::updateCustomFear,
            label = { Text(stringResource(R.string.onboarding_fears_other)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.onboarding_fears_reassurance), color = FinancePositive)
        Button(onClick = viewModel::nextStep, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_next))
        }
    }
}

@Composable
private fun GoalStep(viewModel: OnboardingViewModel, draft: OnboardingDraft) {
    val emotions = listOf(
        UserEmotion.HAPPY to "😊",
        UserEmotion.NEUTRAL to "😐",
        UserEmotion.SAD to "😔",
        UserEmotion.ANGRY to "😡",
        UserEmotion.THINKING to "🤔",
    )

    OnboardingCard {
        Text(stringResource(R.string.onboarding_goal_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = draft.goalText,
            onValueChange = viewModel::updateGoalText,
            label = { Text(stringResource(R.string.onboarding_goal_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.onboarding_emotion_label), fontWeight = FontWeight.Medium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            emotions.forEach { (emotion, emoji) ->
                FilterChip(
                    selected = draft.emotion == emotion,
                    onClick = { viewModel.updateEmotion(emotion) },
                    label = { Text(emoji) },
                )
            }
        }
        OutlinedTextField(
            value = draft.priority,
            onValueChange = viewModel::updatePriority,
            label = { Text(stringResource(R.string.onboarding_priority_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        val name = draft.displayName.trim().ifBlank { stringResource(R.string.onboarding_default_name) }
        Text(stringResource(R.string.onboarding_goal_thanks, name), color = FinancePositive, fontWeight = FontWeight.Medium)
        Button(onClick = viewModel::nextStep, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_next))
        }
    }
}

@Composable
private fun SummaryStep(
    draft: OnboardingDraft,
    debtCount: Int,
    onFinish: () -> Unit,
) {
    val name = draft.displayName.trim().ifBlank { stringResource(R.string.onboarding_default_name) }
    val psychotype = draft.psychotypeResult
    val goal = draft.goalText.trim().ifBlank { stringResource(R.string.onboarding_default_goal) }
    val fearLabels = mapOf(
        "wont_escape" to stringResource(R.string.fear_wont_escape_short),
        "earn_more_than_parents" to stringResource(R.string.fear_earn_more_parents_short),
        "ashamed_of_debt" to stringResource(R.string.fear_ashamed_debt_short),
        "money_corrupts" to stringResource(R.string.fear_money_corrupts_short),
        "rich_are_bad" to stringResource(R.string.fear_rich_bad_short),
    )
    val defaultFearLabel = stringResource(R.string.fear_afraid_numbers_short)
    val fearPreview = draft.selectedFearIds.take(2).joinToString(", ") { fearId ->
        fearLabels[fearId] ?: defaultFearLabel
    }

    OnboardingCard {
        FinOrb(modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(stringResource(R.string.onboarding_summary_greeting, name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.onboarding_summary_intro))
        SummaryRow(
            label = stringResource(R.string.onboarding_summary_psychotype),
            value = psychotype?.let { GoalPsychologyEngines.psychotypeLabel(it) } ?: "—",
        )
        if (fearPreview.isNotBlank()) {
            SummaryRow(
                label = stringResource(R.string.onboarding_summary_fears),
                value = "$fearPreview · ${stringResource(R.string.onboarding_summary_fears_note)}",
            )
        }
        SummaryRow(
            label = stringResource(R.string.onboarding_summary_goal),
            value = "${emotionEmoji(draft.emotion)} $goal",
        )
        SummaryRow(
            label = stringResource(R.string.onboarding_summary_debts),
            value = if (debtCount > 0) {
                stringResource(R.string.onboarding_summary_debts_count, debtCount)
            } else {
                stringResource(R.string.onboarding_summary_no_debts)
            },
        )
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_finish))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun emotionEmoji(emotion: UserEmotion?): String = when (emotion) {
    UserEmotion.HAPPY -> "😊"
    UserEmotion.NEUTRAL -> "😐"
    UserEmotion.SAD -> "😔"
    UserEmotion.ANGRY -> "😡"
    UserEmotion.THINKING -> "🤔"
    null -> "🎯"
}
