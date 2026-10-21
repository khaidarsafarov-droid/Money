package com.example.money.ui.clients

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money.MoneyApplication
import com.example.money.R
import com.example.money.contacts.ContactResolver
import com.example.money.domain.model.Client
import com.example.money.messaging.ContactInfoResolver
import com.example.money.messaging.SmsPermissions
import com.example.money.settings.CurrencyRegion
import com.example.money.ui.components.EmptyIllustration
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.components.MoneyButton
import com.example.money.ui.components.MoneyTextField
import com.example.money.ui.components.QuickPaymentSheet
import com.example.money.ui.home.FinoraAvatar
import com.example.money.ui.debtors.DebtorsViewModel
import com.example.money.ui.util.HapticStrength
import com.example.money.ui.util.vibrate
import com.example.money.ui.util.vibrateShort
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.InputValidators
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
    onBack: () -> Unit,
    onOpenClient: (Long) -> Unit,
    onCreateClient: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val viewModel: ClientsViewModel = viewModel(
        factory = ClientsViewModel.factory(app.clientRepository, app.debtorRepository),
    )
    val debtorsViewModel: DebtorsViewModel = viewModel(
        factory = DebtorsViewModel.Factory(
            app.debtorRepository,
            app.debtReminderScheduler,
            app.preferencesStore,
            app.debtCalendarSyncService,
            app.personalDebtMessageScheduler,
        ),
    )
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val sort by viewModel.clientSort.collectAsStateWithLifecycle()
    val currencyRegion by app.preferencesStore.currencyRegion.collectAsStateWithLifecycle(CurrencyRegion.US)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var quickClientId by remember { mutableStateOf<Long?>(null) }
    val clientDebts by (
        quickClientId?.let(viewModel::observeDebts) ?: flowOf(emptyList())
        ).collectAsStateWithLifecycle(emptyList())
    val quickDebt = clientDebts
        .filter { !it.isPaid && it.remainingAmountCents > 0L }
        .maxByOrNull { it.remainingAmountCents }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClient) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.clients_add))
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.clients_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setSearch,
                label = { Text(stringResource(R.string.clients_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ClientSort.entries, key = { it.name }) { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { viewModel.setSort(option) },
                        label = {
                            Text(
                                text = stringResource(
                                    when (option) {
                                        ClientSort.NAME -> R.string.clients_sort_name
                                        ClientSort.DEBT -> R.string.clients_sort_debt
                                        ClientSort.LAST_MEETING -> R.string.clients_sort_meeting
                                    },
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
            Text(
                text = pluralStringResource(R.plurals.clients_count, clients.size, clients.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            if (clients.isEmpty()) {
                EmptyStateMessage(
                    title = stringResource(R.string.clients_empty),
                    illustration = EmptyIllustration.Debts,
                    actionLabel = stringResource(R.string.clients_add),
                    onAction = onCreateClient,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(clients, key = { it.id }, contentType = { "client" }) { client ->
                        ClientRow(
                            client = client,
                            currency = currencyRegion,
                            onClick = { onOpenClient(client.id) },
                            onQuickPayment = if (client.debtCents > 0L) {
                                { quickClientId = client.id }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
    val paymentSavedMessage = stringResource(R.string.msg_payment_saved)
    val undoLabel = stringResource(R.string.action_undo)
    if (quickDebt != null) {
        QuickPaymentSheet(
            remainingCents = quickDebt.remainingAmountCents,
            currencyRegion = currencyRegion,
            onDismiss = { quickClientId = null },
            onSave = { cents ->
                val debtorId = quickDebt.id
                scope.launch {
                    val paymentId = debtorsViewModel.recordPaymentSync(debtorId, cents, false)
                    quickClientId = null
                    if (paymentId != null) {
                        vibrateShort(context)
                        val result = snackbarHostState.showSnackbar(
                            message = paymentSavedMessage,
                            actionLabel = undoLabel,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            vibrate(context, HapticStrength.MEDIUM)
                            debtorsViewModel.undoPaymentSync(debtorId, paymentId)
                        }
                    }
                }
            },
        )
    }
}

@Composable
internal fun ClientRow(
    client: Client,
    currency: CurrencyRegion,
    onClick: () -> Unit,
    onQuickPayment: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick, role = Role.Button),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FinoraAvatar(client.name)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = client.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (client.company.isNotBlank()) {
                Text(
                    text = client.company,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = Formatters.currencyFromCents(client.debtCents, currency),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        if (onQuickPayment != null) {
            IconButton(onClick = onQuickPayment, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Outlined.Payments,
                    contentDescription = stringResource(R.string.quick_payment_add),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEditorScreen(
    clientId: Long?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val viewModel: ClientsViewModel = viewModel(
        factory = ClientsViewModel.factory(app.clientRepository, app.debtorRepository),
    )
    val existingFlow = if (clientId == null) flowOf(null) else viewModel.observeClient(clientId)
    val existing by existingFlow.collectAsStateWithLifecycle(initialValue = null)

    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var phone by remember(existing?.id) { mutableStateOf(existing?.phone.orEmpty()) }
    var email by remember(existing?.id) { mutableStateOf(existing?.email.orEmpty()) }
    var company by remember(existing?.id) { mutableStateOf(existing?.company.orEmpty()) }
    var note by remember(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var nameError by remember { mutableStateOf(false) }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            ContactResolver.resolve(context, uri)?.let { contact ->
                if (name.isBlank()) name = contact.displayName
            }
            ContactInfoResolver.resolve(context, uri)?.let { info ->
                if (info.name.isNotBlank() && name.isBlank()) name = info.name
                if (info.phone.isNotBlank()) phone = info.phone
                if (info.email.isNotBlank()) email = info.email
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) contactPicker.launch(null)
    }

    val saveClient = {
        if (name.isBlank()) {
            nameError = true
        } else {
            viewModel.save(
                Client(
                    id = existing?.id ?: 0L,
                    syncId = existing?.syncId.orEmpty(),
                    name = name,
                    phone = phone,
                    company = company,
                    email = email.ifBlank { null },
                    lastMeetingAt = existing?.lastMeetingAt,
                    note = note,
                    createdAt = existing?.createdAt ?: 0L,
                ),
            ) { onBack() }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (clientId == null) R.string.clients_add else R.string.clients_edit,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            MoneyButton(
                text = stringResource(R.string.action_save),
                onClick = saveClient,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp),
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
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text(stringResource(R.string.clients_name)) },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text(stringResource(R.string.clients_name_required)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.clients_phone)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.clients_email)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                label = { Text(stringResource(R.string.clients_company)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.clients_note)) },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = {
                    if (SmsPermissions.canReadContacts(context)) {
                        contactPicker.launch(null)
                    } else {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                },
            ) {
                Icon(Icons.Outlined.Contacts, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.clients_pick_contact))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDebtWithDueDateSaved: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val currency by app.preferencesStore.currencyRegion.collectAsStateWithLifecycle(
        initialValue = CurrencyRegion.US,
    )
    val viewModel: ClientsViewModel = viewModel(
        factory = ClientsViewModel.factory(app.clientRepository, app.debtorRepository),
    )
    val clientFlow = remember(clientId) { viewModel.observeClient(clientId) }
    val client by clientFlow.collectAsStateWithLifecycle(initialValue = null)
    val ready by produceState(initialValue = false, clientId) {
        clientFlow.first()
        value = true
    }
    val debts by viewModel.observeDebts(clientId).collectAsStateWithLifecycle(initialValue = emptyList())
    var amountText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showAddDebt by remember { mutableStateOf(false) }
    val invalidAmount = stringResource(R.string.clients_debt_amount_invalid)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(client?.name ?: stringResource(R.string.clients_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = { onEdit(clientId) }) {
                        Text(stringResource(R.string.clients_edit))
                    }
                },
            )
        },
    ) { padding ->
        val current = client
        when {
            !ready -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            current == null -> {
                Text(
                    text = stringResource(R.string.clients_missing),
                    modifier = Modifier.padding(padding).padding(16.dp),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (current.company.isNotBlank()) {
                                Text(
                                    current.company,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                            Text(
                                text = stringResource(
                                    R.string.clients_debt_total,
                                    Formatters.currencyFromCents(current.debtCents, currency),
                                ),
                            )
                            Text(pluralStringResource(R.plurals.client_debts_count, debts.size, debts.size))
                        }
                    }
                    item {
                        MoneyButton(
                            text = stringResource(R.string.clients_mark_meeting),
                            onClick = { viewModel.markMeeting(current.id) },
                        )
                    }
                    if (current.lastMeetingAt != null) {
                        item {
                            Text(
                                stringResource(
                                    R.string.clients_last_meeting,
                                    Formatters.date(current.lastMeetingAt),
                                ),
                            )
                        }
                    }
                    items(debts, key = { it.id }) { debt ->
                        Text(
                            "${debt.name} · ${Formatters.currencyFromCents(debt.remainingAmountCents, currency)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    item {
                        MoneyButton(
                            text = stringResource(R.string.clients_add_debt),
                            onClick = { showAddDebt = true },
                        )
                    }
                }
            }
        }
    }
    val currentForDebt = client
    if (showAddDebt && currentForDebt != null) {
        ModalBottomSheet(onDismissRequest = { showAddDebt = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MoneyTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = null
                    },
                    label = stringResource(R.string.clients_new_debt_amount),
                    isError = amountError != null,
                    supportingText = amountError,
                )
                MoneyButton(
                    text = stringResource(R.string.clients_add_debt),
                    onClick = {
                        val cents = InputValidators.parseAmount(amountText)
                        if (cents == null || cents <= 0L) {
                            amountError = invalidAmount
                        } else {
                            viewModel.addDebt(currentForDebt, cents, currency.currencyCode) {
                                amountText = ""
                                showAddDebt = false
                                onDebtWithDueDateSaved()
                            }
                        }
                    },
                )
            }
        }
    }
}
