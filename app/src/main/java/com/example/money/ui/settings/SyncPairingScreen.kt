package com.example.money.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.money.MoneyApplication
import com.example.money.ui.util.withLightHaptic
import com.example.money.R
import com.example.money.data.sync.PairingOffer
import com.example.money.ui.theme.CreamBackground
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncPairingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val enabled by app.preferencesStore.peerSyncEnabled.collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    var offer by remember { mutableStateOf<PairingOffer?>(null) }
    var peerInput by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val pairedMessage = stringResource(R.string.sync_pair_saved)
    val rejectedMessage = stringResource(R.string.sync_pair_rejected)

    Scaffold(
        modifier = modifier,
        containerColor = CreamBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sync_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_up),
                        )
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.sync_settings_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
                checked = enabled,
                onCheckedChange = withLightHaptic(LocalContext.current) { checked ->
                    scope.launch { app.preferencesStore.setPeerSyncEnabled(checked) }
                },
            )
            Button(
                onClick = {
                    scope.launch {
                        app.preferencesStore.setPeerSyncEnabled(true)
                        offer = app.syncService.createPairingOffer()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.sync_show_qr))
            }
            offer?.let { current ->
                Text(
                    text = stringResource(R.string.sync_pin_label, current.pin),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Image(
                    bitmap = remember(current.qrPayload) { qrBitmap(current.qrPayload) },
                    contentDescription = stringResource(R.string.sync_qr_content_description),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = peerInput,
                onValueChange = { peerInput = it },
                label = { Text(stringResource(R.string.sync_pair_input_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val accepted = if (peerInput.trim().startsWith("money-pair:")) {
                        app.syncService.pairWithQr(peerInput.trim())
                    } else {
                        app.syncService.pairWithPin(peerInput.trim())
                    }
                    message = if (accepted) pairedMessage else rejectedMessage
                    if (accepted) {
                        scope.launch { app.preferencesStore.setPeerSyncEnabled(true) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.sync_pair_action))
            }
            message?.let { text ->
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun qrBitmap(payload: String): ImageBitmap {
    val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 256, 256)
    val image = ImageBitmap(matrix.width, matrix.height)
    val canvas = Canvas(image)
    val paint = Paint().apply {
        color = Color.Black
        style = PaintingStyle.Fill
    }
    for (x in 0 until matrix.width) {
        for (y in 0 until matrix.height) {
            if (matrix.get(x, y)) {
                canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
            }
        }
    }
    return image
}
