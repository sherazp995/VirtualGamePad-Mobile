package io.github.kitswas.virtualgamepadmobile.ui.screens

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.data.ButtonAnchor
import io.github.kitswas.virtualgamepadmobile.data.ButtonComponent
import io.github.kitswas.virtualgamepadmobile.data.ButtonConfig
import io.github.kitswas.virtualgamepadmobile.data.CustomButton
import io.github.kitswas.virtualgamepadmobile.data.OFFSET_VALUE_RANGE
import io.github.kitswas.virtualgamepadmobile.data.PreviewBase
import io.github.kitswas.virtualgamepadmobile.data.SCALE_VALUE_RANGE
import io.github.kitswas.virtualgamepadmobile.data.SettingsRepository
import io.github.kitswas.virtualgamepadmobile.data.defaultButtonConfigs
import io.github.kitswas.virtualgamepadmobile.ui.composables.ChoiceSetting
import io.github.kitswas.virtualgamepadmobile.ui.composables.DrawGamepad
import io.github.kitswas.virtualgamepadmobile.ui.composables.RowDivider
import io.github.kitswas.virtualgamepadmobile.ui.composables.SidebarItem
import io.github.kitswas.virtualgamepadmobile.ui.composables.SliderSetting
import io.github.kitswas.virtualgamepadmobile.ui.composables.SwitchSetting
import io.github.kitswas.virtualgamepadmobile.ui.utils.jsonSaver
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

private const val logTag = "GamepadCustomization"

/**
 * Sanitizes button configurations to ensure all components have a valid config.
 */
private fun sanitizeButtonConfigs(configs: Map<ButtonComponent, ButtonConfig>): Map<ButtonComponent, ButtonConfig> {
    return ButtonComponent.entries.associateWith { component ->
        configs[component] ?: ButtonConfig.default(component)
    }
}

/**
 * Full-screen gamepad preview overlay
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun GamepadPreview(
    buttonConfigs: Map<ButtonComponent, ButtonConfig>,
    customButtons: List<CustomButton>,
) {
    val configuration = LocalConfiguration.current
    val gamepadState = remember { GamepadReading() }

    DrawGamepad(
        widthDp = configuration.screenWidthDp,
        heightDp = configuration.screenHeightDp,
        gamepadState = gamepadState,
        buttonConfigs = buttonConfigs,
        customButtons = customButtons,
    )
}

@Composable
fun GamepadCustomizationScreen(
    onNavigateBack: () -> Unit,
    settingsRepository: SettingsRepository
) {
    val buttonConfigs by settingsRepository.buttonConfigs.collectAsState(initial = defaultButtonConfigs)
    val customButtons by settingsRepository.customButtons.collectAsState(initial = emptyList())
    var modifiedConfigs by rememberSaveable(stateSaver = jsonSaver<Map<ButtonComponent, ButtonConfig>>()) {
        mutableStateOf(null)
    }
    var selected by rememberSaveable { mutableStateOf(ButtonComponent.entries.first()) }
    var showPreview by rememberSaveable { mutableStateOf(false) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var importJsonText by rememberSaveable { mutableStateOf("") }

    // Get the current configs to preview (modified or saved)
    val currentConfigs = modifiedConfigs ?: buttonConfigs
    val config = currentConfigs[selected] ?: ButtonConfig.default(selected)

    fun update(change: (ButtonConfig) -> ButtonConfig) {
        modifiedConfigs = currentConfigs + (selected to change(config))
    }

    // Handle back button to close preview
    BackHandler(enabled = showPreview) {
        showPreview = false
    }

    if (showPreview) {
        // Full-screen preview overlay
        GamepadPreview(buttonConfigs = currentConfigs, customButtons = customButtons)
        return
    }

    if (showExportDialog) {
        ExportConfigDialog(
            buttonConfigs = currentConfigs,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showImportDialog) {
        ImportConfigDialog(
            importedJsonText = importJsonText,
            onJsonTextChange = { importJsonText = it },
            onImport = { configs ->
                modifiedConfigs = configs
                showImportDialog = false
                importJsonText = ""
                Log.i(logTag, "Button configs imported")
            },
            onDismiss = {
                showImportDialog = false
                importJsonText = ""
            }
        )
    }

    Scaffold { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Every control on the left, the screen's actions pinned below them.
            Column(
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    stringResource(R.string.customization_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    ButtonComponent.entries.forEach { component ->
                        SidebarItem(
                            label = stringResource(component.nameRes),
                            selected = component == selected,
                            onClick = { selected = component },
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Button(
                        onClick = {
                            modifiedConfigs?.let { configs ->
                                runBlocking {
                                    try {
                                        settingsRepository.setAllButtonConfigs(configs)
                                        Log.i(logTag, "Button configs saved")
                                    } catch (e: Exception) {
                                        Log.e(logTag, "Error saving button configs", e)
                                    }
                                }
                            }
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.save))
                    }
                    OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // The selected control's settings.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(selected.nameRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showPreview = true }) {
                        Text(stringResource(R.string.customization_preview))
                    }
                    TextButton(onClick = { showImportDialog = true }) {
                        Text(stringResource(R.string.customization_import))
                    }
                    TextButton(onClick = { showExportDialog = true }) {
                        Text(stringResource(R.string.customization_export))
                    }
                    // Unsaved until Save, like every other change here, so Cancel undoes it.
                    TextButton(onClick = { modifiedConfigs = defaultButtonConfigs }) {
                        Text(stringResource(R.string.reset))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.primary)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(key(selected) { rememberScrollState() })
                ) {
                    SwitchSetting(
                        title = stringResource(R.string.button_visible),
                        description = stringResource(R.string.layout_row_visible_desc),
                        checked = config.visible,
                        onCheckedChange = { visible -> update { it.copy(visible = visible) } },
                    )
                    RowDivider()
                    ChoiceSetting(
                        title = stringResource(R.string.button_anchor_position),
                        options = ButtonAnchor.entries,
                        selected = config.anchor,
                        optionLabel = { stringResource(it.nameRes) },
                        onSelect = { anchor -> update { it.copy(anchor = anchor) } },
                    )
                    RowDivider()
                    SliderSetting(
                        title = stringResource(R.string.layout_row_size),
                        valueLabel = stringResource(R.string.value_scale, config.scale),
                        value = config.scale,
                        range = SCALE_VALUE_RANGE,
                        onValueChange = { scale -> update { it.copy(scale = scale) } },
                    )
                    RowDivider()
                    SliderSetting(
                        title = stringResource(R.string.layout_row_horizontal),
                        valueLabel = stringResource(R.string.value_offset, config.offsetX),
                        value = config.offsetX,
                        range = OFFSET_VALUE_RANGE,
                        onValueChange = { x -> update { it.copy(offsetX = x) } },
                    )
                    RowDivider()
                    SliderSetting(
                        title = stringResource(R.string.layout_row_vertical),
                        valueLabel = stringResource(R.string.value_offset, config.offsetY),
                        value = config.offsetY,
                        range = OFFSET_VALUE_RANGE,
                        onValueChange = { y -> update { it.copy(offsetY = y) } },
                    )
                    RowDivider()
                    SliderSetting(
                        title = stringResource(R.string.layout_row_rotation),
                        valueLabel = stringResource(R.string.value_degrees, config.rotation),
                        value = config.rotation,
                        range = 0f..359f,
                        onValueChange = { degrees -> update { it.copy(rotation = degrees.roundToInt().toFloat()) } },
                    )
                }
            }
        }
    }
}

/**
 * Dialog for exporting button configurations as JSON
 */
@Composable
fun ExportConfigDialog(
    buttonConfigs: Map<ButtonComponent, ButtonConfig>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val jsonString = remember(buttonConfigs) {
        Json.encodeToString(buttonConfigs)
    }
    val exportLabel = stringResource(R.string.customization_export_label)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.customization_export_title)) },
        text = {
            Log.i(logTag, "Exporting button configs: $jsonString")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.customization_export_desc),
                    style = MaterialTheme.typography.bodySmall
                )
                TextField(
                    value = jsonString,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    label = { Text(stringResource(R.string.customization_export_label)) },
                    readOnly = true,
                    singleLine = false,
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodySmall,
                    trailingIcon = {
                        Button(onClick = {
                            // Copy to clipboard
                            val clip = android.content.ClipData.newPlainText(
                                exportLabel, jsonString
                            )
                            clipboard.setPrimaryClip(clip)
                            Log.i(logTag, "Exported JSON copied to clipboard")
                        }) {
                            Text(stringResource(R.string.customization_copy))
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.customization_done))
            }
        }
    )
}

/**
 * Dialog for importing button configurations from JSON
 */
@Composable
fun ImportConfigDialog(
    importedJsonText: String,
    onJsonTextChange: (String) -> Unit,
    onImport: (Map<ButtonComponent, ButtonConfig>) -> Unit,
    onDismiss: () -> Unit
) {
    var hasError by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.customization_import_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.customization_import_desc),
                    style = MaterialTheme.typography.bodySmall
                )
                val errorMessage = if (hasError) {
                    stringResource(R.string.customization_import_error)
                } else {
                    ""
                }
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                TextField(
                    value = importedJsonText,
                    onValueChange = onJsonTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    placeholder = { Text(stringResource(R.string.customization_import_placeholder)) },
                    singleLine = false,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val configs: Map<ButtonComponent, ButtonConfig> =
                            Json.decodeFromString(importedJsonText)

                        // Sanitize the imported configs
                        val sanitizedConfigs = sanitizeButtonConfigs(configs)

                        hasError = false
                        Log.i(logTag, "Imported and sanitized button configs: $sanitizedConfigs")
                        onImport(sanitizedConfigs)
                    } catch (e: Exception) {
                        hasError = true
                        Log.e(logTag, "Error parsing JSON", e)
                    }
                }
            ) {
                Text(stringResource(R.string.customization_import))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Preview(
    showBackground = true,
    widthDp = 800,
    heightDp = 400
)
@Composable
fun GamepadCustomizationScreenPreview() {
    PreviewBase {
        GamepadCustomizationScreen(
            onNavigateBack = {},
            settingsRepository = SettingsRepository(LocalContext.current)
        )
    }
}
