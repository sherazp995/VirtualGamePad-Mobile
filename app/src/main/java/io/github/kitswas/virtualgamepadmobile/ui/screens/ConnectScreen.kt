package io.github.kitswas.virtualgamepadmobile.ui.screens

import android.content.Intent
import android.net.InetAddresses
import android.os.Build
import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.data.PreviewBase
import io.github.kitswas.virtualgamepadmobile.data.PreviewHeightDp
import io.github.kitswas.virtualgamepadmobile.data.PreviewWidthDp
import io.github.kitswas.virtualgamepadmobile.data.SavedConnection
import io.github.kitswas.virtualgamepadmobile.ui.components.QRScanResult
import io.github.kitswas.virtualgamepadmobile.ui.components.rememberQRCodeScanner
import io.github.kitswas.virtualgamepadmobile.ui.composables.SectionHeader
import io.github.kitswas.virtualgamepadmobile.ui.composables.SidebarItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

const val LOG_TAG = "ConnectMenu"

private fun getIP(qrCode: String): String {
    val splitTill = qrCode.lastIndexOf(":")
    if (splitTill == -1) return qrCode
    return qrCode.substring(0, splitTill)
}

private fun getPort(qrCode: String): String {
    val splitAt = qrCode.lastIndexOf(":")
    if (splitAt == -1) return qrCode
    return qrCode.substring(splitAt + 1)
}

@Suppress("DEPRECATION")
private fun validateIP(ipAddress: String): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        InetAddresses.isNumericAddress(ipAddress)
    } else {
        Patterns.IP_ADDRESS.matcher(ipAddress).matches()
    }
}

private fun validatePort(port: String): Boolean {
    val minPort = 1
    val maxPort = 65535
    return port.toIntOrNull().let { it != null && it in minPort..maxPort }
}

private fun processQRScanResult(
    result: QRScanResult,
    onNavigateToConnectingScreen: (String, String) -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    context: android.content.Context
) {
    when (result) {
        is QRScanResult.Success -> {
            try {
                val qrCode = result.content
                Log.d(LOG_TAG, qrCode)
                val ipAddress = getIP(qrCode)
                val port = getPort(qrCode)

                if (validateIP(ipAddress) && validatePort(port)) {
                    // Navigate to the connecting screen with the IP and port
                    onNavigateToConnectingScreen(ipAddress, port)
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.connect_qr_error_format),
                        )
                    }
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(
                            R.string.connect_qr_error_processing,
                            e.message ?: e.toString()
                        ),
                    )
                }
            }
        }

        is QRScanResult.Error -> {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.connect_qr_error_scanning, result.message),
                )
            }
        }

        is QRScanResult.PermissionDenied -> {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.connect_camera_permission_denied),
                )
            }
        }

        is QRScanResult.Cancelled -> { // User canceled the QR code scan
        }
    }
}

@Composable
fun ConnectMenu(
    onNavigateToConnectingScreen: (String, String) -> Unit,
    onSaveConnection: suspend (SavedConnection) -> Unit,
    initialIp: String,
    initialPort: String
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val downloadsUrl = "https://kitswas.github.io/VirtualGamePad/#installation"

    val qrCodeScanner = rememberQRCodeScanner { result ->
        processQRScanResult(
            result,
            onNavigateToConnectingScreen = onNavigateToConnectingScreen,
            snackbarHostState = snackbarHostState,
            scope = scope,
            context = context
        )
    }

    var ipAddress by rememberSaveable { mutableStateOf(initialIp) }
    var port by rememberSaveable { mutableStateOf(initialPort) }
    var name by rememberSaveable { mutableStateOf("") }
    val isIPValid = validateIP(ipAddress)
    val isPortValid = validatePort(port)
    val canConnect = isIPValid && isPortValid

    // Update state when parameters arrive asynchronously from DataStore
    LaunchedEffect(initialIp, initialPort) {
        if (ipAddress.isBlank() && initialIp.isNotBlank()) ipAddress = initialIp
        if (port.isBlank() && initialPort.isNotBlank()) port = initialPort
    }

    val focusManager = LocalFocusManager.current
    val savedMessage = stringResource(R.string.connect_saved, name.ifBlank { "$ipAddress:$port" })

    fun attemptToConnect() {
        if (canConnect) onNavigateToConnectingScreen(ipAddress, port)
    }

    fun save() {
        if (!canConnect) return
        scope.launch {
            onSaveConnection(SavedConnection(name.ifBlank { "$ipAddress:$port" }, ipAddress, port))
            snackbarHostState.showSnackbar(savedMessage, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Other ways to connect on the left, the screen's actions pinned below them.
            Column(
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    stringResource(R.string.menu_start),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                SidebarItem(
                    label = stringResource(R.string.connect_scan_qr),
                    onClick = { qrCodeScanner() },
                )
                SidebarItem(
                    label = stringResource(R.string.connect_download_server),
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, downloadsUrl.toUri()))
                    },
                )
                Box(modifier = Modifier.weight(1f))
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Button(
                        onClick = ::attemptToConnect,
                        enabled = canConnect,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.connect_button))
                    }
                    OutlinedButton(
                        onClick = ::save,
                        enabled = canConnect,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.connect_save))
                    }
                }
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // The connection's details, as flat rows.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                SectionHeader(stringResource(R.string.connect_details))
                ConnectField(
                    label = stringResource(R.string.connect_name_label),
                    value = name,
                    onValueChange = { name = it },
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    onIme = { focusManager.moveFocus(FocusDirection.Down) },
                )
                ConnectField(
                    label = stringResource(R.string.connect_ip_label),
                    value = ipAddress,
                    onValueChange = { ipAddress = it.trim() },
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                    onIme = { focusManager.moveFocus(FocusDirection.Down) },
                    error = stringResource(R.string.connect_error_ip).takeIf { ipAddress.isNotEmpty() && !isIPValid },
                )
                ConnectField(
                    label = stringResource(R.string.connect_port_label),
                    value = port,
                    onValueChange = { port = it.trim() },
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    onIme = {
                        focusManager.clearFocus()
                        attemptToConnect()
                    },
                    error = stringResource(R.string.connect_error_port).takeIf { port.isNotEmpty() && !isPortValid },
                )
                Text(
                    stringResource(R.string.connect_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

/** A full-width field row; an error shows under it only once something is typed. */
@Composable
private fun ConnectField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onIme: () -> Unit,
    error: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onIme() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Preview(
    widthDp = PreviewWidthDp,
    heightDp = PreviewHeightDp,
)
@Composable
fun ConnectMenuPreview() {
    PreviewBase {
        ConnectMenu(
            onNavigateToConnectingScreen = { _, _ -> },
            onSaveConnection = {},
            initialIp = "",
            initialPort = "12345"
        )
    }
}
