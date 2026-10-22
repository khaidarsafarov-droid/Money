package com.example.money.ui.goal

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Psychology
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money.MoneyApplication
import com.example.money.R
import com.example.money.ui.components.DialogImeEffect
import com.example.money.ui.components.bringIntoViewOnFocus
import com.example.money.ui.components.scrollAboveIme
import com.example.money.domain.model.LuckDiaryEntry
import com.example.money.domain.util.GoalChallengeEngines
import com.example.money.domain.util.GoalPsychologyEngines
import com.example.money.settings.AutopilotStrategy
import com.example.money.settings.FinancialPsychotype
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.InputValidators
import com.example.money.ui.util.withLightHaptic
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(
    onBack: () -> Unit,
    onOpenDebts: () -> Unit = onBack,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val viewModel: GoalHubViewModel = viewModel(
        factory = GoalHubViewModel.Factory(
            app.debtorRepository,
            app.preferencesStore,
            app.luckDiaryRepository,
        ),
    )

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val psychotypeTestPrompt by viewModel.psychotypeTestPrompt.collectAsStateWithLifecycle()
    val userProfile by app.preferencesStore.userProfile.collectAsStateWithLifecycle(
        initialValue = com.example.money.settings.UserProfile(),
    )
    val privacySettings by app.preferencesStore.privacySettings.collectAsStateWithLifecycle(
        initialValue = com.example.money.settings.PrivacySettings(),
    )
    val hideAmounts = privacySettings.hideAmounts
    val payoffPlan by viewModel.payoffPlan.collectAsStateWithLifecycle()
    val luckEntries by viewModel.luckEntries.collectAsStateWithLifecycle()
    val activeChallenges by viewModel.activeChallenges.collectAsStateWithLifecycle()
    val debtors by viewModel.debtors.collectAsStateWithLifecycle()

    val displayName = userProfile.resolvedDisplayName(stringResource(R.string.onboarding_default_name))
    val goalText = userProfile.resolvedGoalText(stringResource(R.string.onboarding_default_goal))

    var showPsychotypeTest by remember { mutableStateOf(false) }
    LaunchedEffect(psychotypeTestPrompt) {
        if (psychotypeTestPrompt == true) {
            showPsychotypeTest = true
        }
    }
    var showLuckForm by remember { mutableStateOf(false) }
    var editingLuck by remember { mutableStateOf<LuckDiaryEntry?>(null) }
    var showFears by remember { mutableStateOf(false) }
    var showChallengePicker by remember { mutableStateOf(false) }
    var pendingChallengeTemplate by remember {
        mutableStateOf<GoalChallengeEngines.ChallengeTemplateId?>(null)
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { picked ->
            persistVisionImage(context, picked)?.let(viewModel::updateVisionPhotoPath)
        }
    }

    val psychotype = profile.psychotype ?: FinancialPsychotype.BALANCED
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

    editingLuck?.let { entry ->
        LuckDiaryEntryDialog(
            initial = entry,
            onDismiss = { editingLuck = null },
            onSave = { note, amount, category ->
                viewModel.updateLuckEntry(
                    entry.copy(note = note, amountCents = amount, category = category),
                )
                editingLuck = null
            },
        )
    }

    if (showChallengePicker) {
        ChallengePickerDialog(
            templates = GoalChallengeEngines.templates,
            onDismiss = { showChallengePicker = false },
            onPick = { templateId ->
                showChallengePicker = false
                val template = GoalChallengeEngines.templateFor(templateId)
                if (template.requiresDebtPick) {
                    pendingChallengeTemplate = templateId
                } else {
                    viewModel.startChallenge(templateId)
                }
            },
        )
    }

    pendingChallengeTemplate?.let { templateId ->
        DebtPickDialog(
            debtors = debtors.filter {
                it.debtType == com.example.money.domain.model.DebtType.DEBTOR &&
                    !it.isPaid &&
                    it.remainingAmountCents > 0
            },
            onDismiss = { pendingChallengeTemplate = null },
            onPick = { debtor ->
                viewModel.startChallenge(templateId, debtor.id, debtor.name)
                pendingChallengeTemplate = null
            },
            hideAmounts = hideAmounts,
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.goal_hub_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_up))
                    }
                },
                actions = {
                    IconButton(onClick = { showPsychotypeTest = true }) {
                        Icon(
                            Icons.Outlined.Psychology,
                            contentDescription = stringResource(R.string.psychotype_retake),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HubCard {
                Text(stringResource(R.string.goal_hub_greeting, displayName, GoalPsychologyEngines.psychotypeLabel(psychotype)))
                Text(GoalPsychologyEngines.psychotypeTagline(psychotype), color = MaterialTheme.colorScheme.primary)
            }

            if (profile.goalBadges.isNotEmpty()) {
                BadgesCard(badgeIds = profile.goalBadges)
            }

            if (psychotype == FinancialPsychotype.PLAYER) {
                PlayerPointsCard(challengeCount = activeChallenges.count { !it.completed })
            }

            VisionBoardCard(
                photoPath = profile.visionPhotoPath,
                motivation = profile.personalMotivation,
                onPickPhoto = { photoPicker.launch("image/*") },
                onClearPhoto = { viewModel.updateVisionPhotoPath(null) },
            )

            if (psychotype == FinancialPsychotype.SPENDER || psychotype == FinancialPsychotype.BALANCED) {
                val days = GoalPsychologyEngines.daysUntilPayday(profile.paydayDayOfMonth)
                HubCard {
                    Text(stringResource(R.string.goal_hub_payday, days), fontWeight = FontWeight.SemiBold)
                }
            }

            PayoffPlanCard(
                plan = payoffPlan,
                strategy = AutopilotStrategy.fromStored(profile.payoffStrategy),
                wordPower = profile.wordPowerEnabled,
                onStrategyChange = viewModel::setPayoffStrategy,
                hideAmounts = hideAmounts,
            )

            HubCard {
                HubHeader(
                    title = stringResource(R.string.goal_hub_fears_title),
                    expanded = showFears,
                    onToggle = { showFears = !showFears },
                )
                if (showFears) {
                    FearSelector(
                        selected = profile.selectedFearIds,
                        onChange = viewModel::updateFears,
                    )
                    profile.selectedFearIds.forEach { fearId ->
                        Text(
                            text = "• ${GoalPsychologyEngines.affirmationForFear(fearId, 0, displayName, goalText)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            MotivationCard(
                motivation = profile.personalMotivation,
                onMotivationChange = viewModel::updatePersonalMotivation,
            )

            HubCard {
                var showLuckList by remember { mutableStateOf(false) }
                HubHeader(
                    title = stringResource(R.string.goal_hub_luck_title_personal, displayName),
                    expanded = showLuckList,
                    onToggle = { showLuckList = !showLuckList },
                    trailing = {
                        TextButton(onClick = { showLuckForm = true }) {
                            Text(stringResource(R.string.goal_hub_luck_add))
                        }
                    },
                )
                Text(
                    stringResource(
                        R.string.goal_hub_luck_monthly,
                        Formatters.currencyFromCents(monthlyLuck, hide = hideAmounts),
                        luckEntries.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (showLuckList) {
                    luckEntries.take(5).forEach { entry ->
                        LuckEntryRow(
                            entry = entry,
                            hideAmounts = hideAmounts,
                            onEdit = { editingLuck = entry },
                            onDelete = { viewModel.deleteLuckEntry(entry) },
                        )
                    }
                }
            }

            ChallengesCard(
                challenges = activeChallenges,
                displayName = displayName,
                onAdd = { showChallengePicker = true },
                onIncrement = viewModel::incrementChallengeDay,
                onRemove = viewModel::removeChallenge,
            )

            WeeklyChecklistCard(viewModel = viewModel, profile = profile)

            HubCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.goal_hub_word_power),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Switch(
                        checked = profile.wordPowerEnabled,
                        onCheckedChange = withLightHaptic(context, onCheckedChange = viewModel::setWordPowerEnabled),
                    )
                }
            }
        }
    }
}

@Composable
private fun HubHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing?.invoke()
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
        )
    }
}

@Composable
private fun HubCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun PayoffPlanCard(
    plan: List<GoalPsychologyEngines.PayoffPlanItem>,
    strategy: AutopilotStrategy,
    wordPower: Boolean,
    onStrategyChange: (AutopilotStrategy) -> Unit,
    hideAmounts: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    HubCard {
        HubHeader(
            title = stringResource(R.string.goal_hub_plan_title),
            expanded = expanded,
            onToggle = { expanded = !expanded },
        )
        if (!expanded) return@HubCard
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
                                Formatters.currencyFromCents(item.remainingCents, hide = hideAmounts),
                            )
                        } else {
                            Formatters.currencyFromCents(item.remainingCents, hide = hideAmounts)
                        },
                    )
                    val closeLabel = when {
                        !item.hasPaymentData -> stringResource(R.string.goal_hub_no_payment_data)
                        item.projectedCloseDateMs != null -> Formatters.shortDate(item.projectedCloseDateMs)
                        else -> stringResource(R.string.goal_hub_close_soon)
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
    var expanded by remember { mutableStateOf(false) }
    HubCard {
        HubHeader(
            title = stringResource(R.string.goal_hub_motivation_title),
            expanded = expanded,
            onToggle = { expanded = !expanded },
        )
        if (!expanded) return@HubCard
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onMotivationChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewOnFocus(),
            placeholder = { Text(stringResource(R.string.goal_hub_motivation_placeholder)) },
        )
    }
}

@Composable
private fun WeeklyChecklistCard(viewModel: GoalHubViewModel, profile: com.example.money.settings.GoalPsychologyProfile) {
    val checklist = remember(profile.weeklyChecklistJson) {
        GoalHubViewModel.decodeChecklistForUi(profile.weeklyChecklistJson)
    }

    var expanded by remember { mutableStateOf(false) }
    HubCard {
        HubHeader(
            title = stringResource(R.string.goal_hub_checklist_title),
            expanded = expanded,
            onToggle = { expanded = !expanded },
        )
        if (!expanded) return@HubCard
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
private fun BadgesCard(badgeIds: Set<String>) {
    HubCard {
        Text(stringResource(R.string.goal_hub_badges_title), fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            badgeIds.forEach { badgeId ->
                FilterChip(
                    selected = true,
                    onClick = {},
                    enabled = false,
                    label = { Text(GoalChallengeEngines.badgeLabel(badgeId)) },
                )
            }
        }
    }
}

@Composable
private fun PlayerPointsCard(challengeCount: Int) {
    HubCard {
        Text(stringResource(R.string.goal_hub_player_title), fontWeight = FontWeight.SemiBold)
        Text(
            stringResource(R.string.goal_hub_player_challenges, challengeCount),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun VisionBoardCard(
    photoPath: String?,
    motivation: String,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
) {
    HubCard {
        Text(stringResource(R.string.goal_hub_vision_title), fontWeight = FontWeight.SemiBold)
        photoPath?.let { path ->
            val bitmap = remember(path) {
                try {
                    BitmapFactory.decodeFile(path)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = stringResource(R.string.goal_hub_vision_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        if (motivation.isNotBlank()) {
            Text("«$motivation»", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onPickPhoto) {
                Text(stringResource(R.string.goal_hub_vision_pick))
            }
            if (photoPath != null) {
                TextButton(onClick = onClearPhoto) {
                    Text(stringResource(R.string.goal_hub_vision_clear))
                }
            }
        }
    }
}

@Composable
private fun ChallengesCard(
    challenges: List<GoalChallengeEngines.ActiveChallenge>,
    displayName: String,
    onAdd: () -> Unit,
    onIncrement: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    HubCard {
        HubHeader(
            title = stringResource(R.string.goal_hub_challenges_title_personal, displayName),
            expanded = expanded,
            onToggle = { expanded = !expanded },
            trailing = {
                TextButton(onClick = onAdd) {
                    Text(stringResource(R.string.goal_hub_challenges_add))
                }
            },
        )
        if (expanded && challenges.isNotEmpty()) {
            challenges.forEach { challenge ->
                val template = GoalChallengeEngines.templateFor(challenge.templateId)
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(template.title, fontWeight = FontWeight.Medium)
                    challenge.debtName?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    val target = when (challenge.templateId) {
                        GoalChallengeEngines.ChallengeTemplateId.THREE_EARLY_PAYMENTS -> 3
                        else -> challenge.targetDays
                    }
                    if (target != null && target > 0) {
                        LinearProgressIndicator(
                            progress = { challenge.progressDays.toFloat() / target },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        )
                        Text(
                            stringResource(
                                R.string.goal_hub_challenge_progress,
                                challenge.progressDays,
                                target,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (challenge.completed) {
                        Text(stringResource(R.string.goal_hub_challenge_done), color = MaterialTheme.colorScheme.primary)
                    } else if (challenge.templateId != GoalChallengeEngines.ChallengeTemplateId.THREE_EARLY_PAYMENTS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onIncrement(challenge.id) }) {
                                Text(stringResource(R.string.goal_hub_challenge_mark_day))
                            }
                            TextButton(onClick = { onRemove(challenge.id) }) {
                                Text(stringResource(R.string.action_delete))
                            }
                        }
                    } else {
                        TextButton(onClick = { onRemove(challenge.id) }) {
                            Text(stringResource(R.string.action_delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengePickerDialog(
    templates: List<GoalChallengeEngines.ChallengeTemplate>,
    onDismiss: () -> Unit,
    onPick: (GoalChallengeEngines.ChallengeTemplateId) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goal_hub_challenges_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                templates.forEach { template ->
                    TextButton(
                        onClick = { onPick(template.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            Text(template.title, fontWeight = FontWeight.Medium)
                            Text(template.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun DebtPickDialog(
    debtors: List<com.example.money.domain.model.Debtor>,
    onDismiss: () -> Unit,
    onPick: (com.example.money.domain.model.Debtor) -> Unit,
    hideAmounts: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goal_hub_challenge_pick_debt)) },
        text = {
            Column {
                if (debtors.isEmpty()) {
                    Text(stringResource(R.string.goal_hub_no_debts))
                } else {
                    debtors.forEach { debtor ->
                        TextButton(
                            onClick = { onPick(debtor) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${debtor.name} · ${Formatters.currencyFromCents(debtor.remainingAmountCents, hide = hideAmounts)}")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun LuckEntryRow(
    entry: LuckDiaryEntry,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    hideAmounts: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.note, style = MaterialTheme.typography.bodySmall)
            Text(
                "${GoalChallengeEngines.luckCategoryLabel(entry.category)} · ${Formatters.shortDate(entry.entryDate)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            entry.amountCents?.let {
                Text(Formatters.currencyFromCents(it, hide = hideAmounts), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row {
            TextButton(onClick = onEdit) {
                Text(stringResource(R.string.action_edit))
            }
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.action_delete))
            }
        }
    }
}

private fun persistVisionImage(context: android.content.Context, uri: Uri): String? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.filesDir, "goal_vision_${System.currentTimeMillis()}.jpg")
        input.use { stream ->
            file.outputStream().use { output -> stream.copyTo(output) }
        }
        if (!file.exists() || file.length() == 0L) {
            file.delete()
            return null
        }
        file.absolutePath
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun LuckDiaryEntryDialog(
    onDismiss: () -> Unit,
    onSave: (note: String, amountCents: Long?, category: String) -> Unit,
    initial: LuckDiaryEntry? = null,
) {
    var note by remember(initial?.id) { mutableStateOf(initial?.note.orEmpty()) }
    var amountText by remember(initial?.id) {
        mutableStateOf(
            initial?.amountCents?.let { (it / 100.0).let { amount ->
                if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
            } }.orEmpty(),
        )
    }
    var category by remember(initial?.id) { mutableStateOf(initial?.category ?: "OTHER") }

    val categories = listOf(
        "UNEXPECTED_INCOME" to R.string.luck_cat_income,
        "SAVINGS" to R.string.luck_cat_savings,
        "LUCK" to R.string.luck_cat_luck,
        "GIFT" to R.string.luck_cat_gift,
        "OTHER" to R.string.luck_cat_other,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goal_hub_luck_add)) },
        text = {
            DialogImeEffect()
            Column(
                modifier = Modifier.scrollAboveIme(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.goal_hub_luck_what)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.goal_hub_luck_amount)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { (id, labelRes) ->
                        FilterChip(
                            selected = category == id,
                            onClick = { category = id },
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = note.trim()
                    if (trimmed.isEmpty()) return@TextButton
                    val amount = amountText.trim().takeIf { it.isNotEmpty() }?.let(InputValidators::parseAmount)
                    onSave(trimmed, amount, category)
                },
                enabled = note.trim().isNotEmpty(),
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
