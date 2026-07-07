package com.example.money.ui.goal

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.domain.util.GoalPsychologyEngines
import com.example.money.settings.AutopilotStrategy
import com.example.money.settings.FinancialPsychotype
import com.example.money.ui.theme.CreamBackground
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val viewModel: GoalHubViewModel = viewModel(
        factory = GoalHubViewModel.Factory(
            app.debtorRepository,
            app.preferencesStore,
            app.database.luckDiaryDao(),
        ),
    )

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val freedom by viewModel.freedomProjection.collectAsStateWithLifecycle()
    val payoffPlan by viewModel.payoffPlan.collectAsStateWithLifecycle()
    val luckEntries by viewModel.luckEntries.collectAsStateWithLifecycle()
    val extraMonthly by viewModel.extraMonthlyCents.collectAsStateWithLifecycle()

    var showPsychotypeTest by remember { mutableStateOf(!profile.psychotypeTestCompleted) }
    var showLuckForm by remember { mutableStateOf(false) }
    var showFears by remember { mutableStateOf(false) }

    val psychotype = profile.psychotype ?: FinancialPsychotype.BALANCED
    val progress = viewModel.progressPercent()
    val monthlyLuck = viewModel.monthlyLuckSumCents()

    if (showPsychotypeTest) {
        PsychotypeTestDialog(
            onDismiss = { showPsychotypeTest = false },
            onComplete = viewModel::submitPsychotypeTest,
        )
    }

    if (showLuckForm) {
        LuckDiaryEntryDialog(
            onDismiss = { showLuckForm = false },
            onSave = { note, amount, category ->
                viewModel.addLuckEntry(note, amount, category)
                showLuckForm = false
            },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = CreamBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.goal_hub_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = { showPsychotypeTest = true }) {
                        Text(stringResource(R.string.psychotype_retake))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreamBackground),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HubCard {
                Text(stringResource(R.string.goal_hub_psychotype, GoalPsychologyEngines.psychotypeLabel(psychotype)))
                Text(GoalPsychologyEngines.psychotypeTagline(psychotype), color = GoldPrimary)
            }

            FreedomHeroCard(freedom = freedom, progress = progress, wordPower = profile.wordPowerEnabled)

            if (psychotype == FinancialPsychotype.SPENDER || psychotype == FinancialPsychotype.BALANCED) {
                val days = GoalPsychologyEngines.daysUntilPayday(profile.paydayDayOfMonth)
                HubCard {
                    Text(stringResource(R.string.goal_hub_payday, days), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.goal_hub_autosave_hint))
                }
            }

            if (psychotype == FinancialPsychotype.SAVER || psychotype == FinancialPsychotype.BALANCED) {
                HubCard {
                    Text(stringResource(R.string.goal_hub_treat_title), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.goal_hub_treat_body))
                }
            }

            ExtraMonthlySlider(
                extraMonthly = extraMonthly,
                freedom = freedom,
                onChange = viewModel::setExtraMonthlyCents,
            )

            PayoffPlanCard(
                plan = payoffPlan,
                strategy = AutopilotStrategy.fromStored(profile.payoffStrategy),
                wordPower = profile.wordPowerEnabled,
                onStrategyChange = viewModel::setPayoffStrategy,
            )

            HubCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.goal_hub_fears_title), fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { showFears = !showFears }) {
                        Text(if (showFears) stringResource(R.string.action_close) else stringResource(R.string.action_edit))
                    }
                }
                if (showFears) {
                    FearSelector(
                        selected = profile.selectedFearIds,
                        onChange = viewModel::updateFears,
                    )
                }
                profile.selectedFearIds.forEach { fearId ->
                    Text(
                        text = "• ${GoalPsychologyEngines.affirmationForFear(fearId, progress)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FinancePositive,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            MotivationCard(
                motivation = profile.personalMotivation,
                onMotivationChange = viewModel::updatePersonalMotivation,
            )

            HubCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.goal_hub_luck_title), fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { showLuckForm = true }) {
                        Text(stringResource(R.string.goal_hub_luck_add))
                    }
                }
                Text(
                    stringResource(
                        R.string.goal_hub_luck_monthly,
                        Formatters.currencyFromCents(monthlyLuck),
                        luckEntries.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            WeeklyChecklistCard(viewModel = viewModel, profile = profile)

            HubCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.goal_hub_word_power), fontWeight = FontWeight.SemiBold)
                    Switch(checked = profile.wordPowerEnabled, onCheckedChange = viewModel::setWordPowerEnabled)
                }
                Text(stringResource(R.string.goal_hub_word_power_hint), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun HubCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun FreedomHeroCard(
    freedom: DebtFreedomEngine.FreedomProjection,
    progress: Int,
    wordPower: Boolean,
) {
    HubCard {
        Text(
            if (wordPower) stringResource(R.string.goal_hub_freedom_project) else stringResource(R.string.debt_freedom_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        val dateLabel = freedom.freedomDateMillis?.let { Formatters.shortDate(it) }
            ?: stringResource(R.string.debt_freedom_unknown)
        Text(dateLabel, style = MaterialTheme.typography.headlineSmall, color = GoldPrimary, fontWeight = FontWeight.Bold)
        freedom.monthsRemaining?.let {
            Text(stringResource(R.string.goal_hub_days_left, it * 30))
        }
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = FinancePositive,
        )
        Text(stringResource(R.string.weekly_checkin_progress, progress))
        Text(
            stringResource(
                if (wordPower) R.string.goal_hub_remaining_positive else R.string.debt_freedom_remaining_total,
                Formatters.currencyFromCents(freedom.totalRemainingCents),
            ),
        )
    }
}

@Composable
private fun ExtraMonthlySlider(
    extraMonthly: Long,
    freedom: DebtFreedomEngine.FreedomProjection,
    onChange: (Long) -> Unit,
) {
    HubCard {
        Text(stringResource(R.string.debt_freedom_extra_slider_label), fontWeight = FontWeight.SemiBold)
        Slider(
            value = extraMonthly.toFloat(),
            onValueChange = { onChange((it / 500f).toLong() * 500L) },
            valueRange = 0f..50_000f,
            steps = 20,
        )
        Text(stringResource(R.string.debt_freedom_extra_amount, Formatters.currencyFromCents(extraMonthly)))
        freedom.freedomDateMillis?.let { date ->
            Text(
                stringResource(R.string.goal_hub_new_date, Formatters.shortDate(date)),
                color = FinancePositive,
            )
        }
    }
}

@Composable
private fun PayoffPlanCard(
    plan: List<GoalPsychologyEngines.PayoffPlanItem>,
    strategy: AutopilotStrategy,
    wordPower: Boolean,
    onStrategyChange: (AutopilotStrategy) -> Unit,
) {
    HubCard {
        Text(stringResource(R.string.goal_hub_plan_title), fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = strategy == AutopilotStrategy.AVALANCHE,
                onClick = { onStrategyChange(AutopilotStrategy.AVALANCHE) },
                label = { Text(stringResource(R.string.autopilot_strategy_avalanche)) },
            )
            FilterChip(
                selected = strategy == AutopilotStrategy.SNOWBALL,
                onClick = { onStrategyChange(AutopilotStrategy.SNOWBALL) },
                label = { Text(stringResource(R.string.autopilot_strategy_snowball)) },
            )
        }
        if (plan.isEmpty()) {
            Text(stringResource(R.string.goal_hub_no_debts))
        } else {
            plan.forEach { item ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(item.name, fontWeight = FontWeight.Medium)
                    Text(
                        if (wordPower) {
                            stringResource(
                                R.string.goal_hub_remaining_positive,
                                Formatters.currencyFromCents(item.remainingCents),
                            )
                        } else {
                            Formatters.currencyFromCents(item.remainingCents)
                        },
                    )
                    val closeLabel = when {
                        !item.hasPaymentData -> stringResource(R.string.goal_hub_no_payment_data)
                        item.projectedCloseDateMs != null -> Formatters.shortDate(item.projectedCloseDateMs)
                        else -> stringResource(R.string.debt_freedom_unknown)
                    }
                    Text("→ $closeLabel", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = { item.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FearSelector(selected: Set<String>, onChange: (Set<String>) -> Unit) {
    val options = listOf(
        "wont_escape" to R.string.fear_wont_escape,
        "earn_more_than_parents" to R.string.fear_earn_more_parents,
        "ashamed_of_debt" to R.string.fear_ashamed_debt,
        "money_corrupts" to R.string.fear_money_corrupts,
        "rich_are_bad" to R.string.fear_rich_bad,
        "afraid_of_numbers" to R.string.fear_afraid_numbers,
    )
    options.forEach { (id, labelRes) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = id in selected,
                onCheckedChange = { checked ->
                    val next = selected.toMutableSet()
                    if (checked) next.add(id) else next.remove(id)
                    onChange(next)
                },
            )
            Text(stringResource(labelRes), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MotivationCard(motivation: String, onMotivationChange: (String) -> Unit) {
    var text by remember(motivation) { mutableStateOf(motivation) }
    HubCard {
        Text(stringResource(R.string.goal_hub_motivation_title), fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.goal_hub_motivation_hint), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onMotivationChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.goal_hub_motivation_placeholder)) },
        )
    }
}

@Composable
private fun WeeklyChecklistCard(viewModel: GoalHubViewModel, profile: com.example.money.settings.GoalPsychologyProfile) {
    val checklist = remember(profile.weeklyChecklistJson) {
        try {
            val obj = org.json.JSONObject(profile.weeklyChecklistJson)
            WeeklyChecklistUiState(
                checkedDebts = obj.optBoolean("checkedDebts"),
                madePayment = obj.optBoolean("madePayment"),
                savedEmergency = obj.optBoolean("savedEmergency"),
                pleasureOrRestraint = obj.optBoolean("pleasureOrRestraint"),
                luckDiaryEntry = obj.optBoolean("luckDiaryEntry"),
            )
        } catch (_: Exception) {
            WeeklyChecklistUiState()
        }
    }

    HubCard {
        Text(stringResource(R.string.goal_hub_checklist_title), fontWeight = FontWeight.SemiBold)
        ChecklistRow(R.string.checklist_checked_debts, checklist.checkedDebts) {
            viewModel.updateChecklist { s -> s.copy(checkedDebts = it) }
        }
        ChecklistRow(R.string.checklist_made_payment, checklist.madePayment) {
            viewModel.updateChecklist { s -> s.copy(madePayment = it) }
        }
        ChecklistRow(R.string.checklist_saved_emergency, checklist.savedEmergency) {
            viewModel.updateChecklist { s -> s.copy(savedEmergency = it) }
        }
        ChecklistRow(R.string.checklist_pleasure, checklist.pleasureOrRestraint) {
            viewModel.updateChecklist { s -> s.copy(pleasureOrRestraint = it) }
        }
        ChecklistRow(R.string.checklist_luck_diary, checklist.luckDiaryEntry) {
            viewModel.updateChecklist { s -> s.copy(luckDiaryEntry = it) }
        }
    }
}

@Composable
private fun ChecklistRow(labelRes: Int, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onToggle)
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LuckDiaryEntryDialog(
    onDismiss: () -> Unit,
    onSave: (note: String, amountCents: Long?, category: String) -> Unit,
) {
    var note by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("OTHER") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goal_hub_luck_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.goal_hub_luck_what)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.goal_hub_luck_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull()?.let {
                        com.example.money.domain.util.MoneyMath.toCents(it)
                    }
                    onSave(note.trim(), amount, category)
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
