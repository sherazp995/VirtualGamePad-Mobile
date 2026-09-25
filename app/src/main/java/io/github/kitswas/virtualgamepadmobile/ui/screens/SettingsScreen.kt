package io.github.kitswas.virtualgamepadmobile.ui.screens

import android.os.Parcelable
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.data.BaseColor
import io.github.kitswas.virtualgamepadmobile.data.ColorScheme
import io.github.kitswas.virtualgamepadmobile.data.PreviewBase
import io.github.kitswas.virtualgamepadmobile.data.PreviewHeightDp
import io.github.kitswas.virtualgamepadmobile.data.PreviewWidthDp
import io.github.kitswas.virtualgamepadmobile.data.SettingsRepository
import io.github.kitswas.virtualgamepadmobile.data.defaultBaseColor
import io.github.kitswas.virtualgamepadmobile.data.defaultColorScheme
import io.github.kitswas.virtualgamepadmobile.data.defaultFullScreenEnabled
import io.github.kitswas.virtualgamepadmobile.data.defaultHapticFeedbackEnabled
import io.github.kitswas.virtualgamepadmobile.data.defaultPollingDelay
import io.github.kitswas.virtualgamepadmobile.data.defaultSaveConnectionCredentials
import io.github.kitswas.virtualgamepadmobile.ui.composables.ChoiceSetting
import io.github.kitswas.virtualgamepadmobile.ui.composables.LinkSetting
import io.github.kitswas.virtualgamepadmobile.ui.composables.RowDivider
import io.github.kitswas.virtualgamepadmobile.ui.composables.SectionHeader
import io.github.kitswas.virtualgamepadmobile.ui.composables.SidebarItem
import io.github.kitswas.virtualgamepadmobile.ui.composables.SliderSetting
import io.github.kitswas.virtualgamepadmobile.ui.composables.SwitchSetting
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt

private const val logTag = "SettingsScreen"

private val pollingDelayRange = 20..200

@Parcelize
private data class SettingsChanges(
    val colorScheme: ColorScheme? = null,
    val baseColor: BaseColor? = null,
    val pollingDelay: Int? = null,
    val hapticFeedbackEnabled: Boolean? = null,
    val saveConnectionCredentials: Boolean? = null,
    val fullScreenEnabled: Boolean? = null,
) : Parcelable

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGamepadCustomization: () -> Unit,
    onNavigateToLayoutEditor: () -> Unit,
    settingsRepository: SettingsRepository
) {
    var settingsChanges by rememberSaveable { mutableStateOf(SettingsChanges()) }
    var confirmingReset by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf(SettingsCategory.CONTROLS) }

    val colorScheme by settingsRepository.colorScheme.collectAsState(initial = defaultColorScheme)
    val baseColor by settingsRepository.baseColor.collectAsState(initial = defaultBaseColor)
    val pollingDelay by settingsRepository.pollingDelay.collectAsState(initial = defaultPollingDelay)
    val hapticEnabled by settingsRepository.hapticFeedbackEnabled.collectAsState(initial = defaultHapticFeedbackEnabled)
    val saveCredentials by settingsRepository.saveConnectionCredentials.collectAsState(initial = defaultSaveConnectionCredentials)
    val fullScreenEnabled by settingsRepository.fullScreenEnabled.collectAsState(initial = defaultFullScreenEnabled)

    fun save() {
        var changesSaved = 0
        runBlocking {
            try {
                settingsChanges.colorScheme?.let { settingsRepository.setColorScheme(it); ++changesSaved }
                settingsChanges.baseColor?.let { settingsRepository.setBaseColor(it); ++changesSaved }
                settingsChanges.pollingDelay?.let { settingsRepository.setPollingDelay(it); ++changesSaved }
                settingsChanges.hapticFeedbackEnabled?.let {
                    settingsRepository.setHapticFeedbackEnabled(it); ++changesSaved
                }
                settingsChanges.saveConnectionCredentials?.let {
                    settingsRepository.setSaveConnectionCredentials(it); ++changesSaved
                }
                settingsChanges.fullScreenEnabled?.let {
                    settingsRepository.setFullScreenEnabled(it); ++changesSaved
                }
            } catch (e: Exception) {
                Log.e(logTag, "Error saving settings", e)
            }
        }
        Log.i(logTag, "Saved settings: $changesSaved")
        onNavigateBack()
    }

    if (confirmingReset) {
        AlertDialog(
            onDismissRequest = { confirmingReset = false },
            title = { Text(stringResource(R.string.settings_reset_title)) },
            text = { Text(stringResource(R.string.settings_reset_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmingReset = false
                        settingsChanges = SettingsChanges()
                        runBlocking { settingsRepository.resetAllSettings() }
                        Log.i(logTag, "Settings reset to defaults")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(stringResource(R.string.settings_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingReset = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SettingsSidebar(
                selected = category,
                onSelect = { category = it },
                onRestoreDefaults = { confirmingReset = true },
                onCancel = onNavigateBack,
                onSave = ::save,
            )
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(key(category) { rememberScrollState() })
            ) {
                SectionHeader(stringResource(category.titleRes))
                when (category) {
                    SettingsCategory.CONTROLS -> {
                        SwitchSetting(
                            title = stringResource(R.string.settings_haptic_feedback),
                            description = stringResource(R.string.settings_haptic_feedback_desc),
                            checked = settingsChanges.hapticFeedbackEnabled ?: hapticEnabled,
                            onCheckedChange = {
                                settingsChanges = settingsChanges.copy(hapticFeedbackEnabled = it)
                            },
                        )
                    }

                    SettingsCategory.LAYOUT -> {
                        LinkSetting(
                            title = stringResource(R.string.settings_edit_layout_visually),
                            description = stringResource(R.string.settings_edit_layout_visually_desc),
                            onClick = onNavigateToLayoutEditor,
                        )
                        RowDivider()
                        LinkSetting(
                            title = stringResource(R.string.settings_customize_layout),
                            description = stringResource(R.string.settings_customize_layout_desc),
                            onClick = onNavigateToGamepadCustomization,
                        )
                    }

                    SettingsCategory.DISPLAY -> {
                        ChoiceSetting(
                            title = stringResource(R.string.settings_color_scheme),
                            options = ColorScheme.entries,
                            selected = settingsChanges.colorScheme ?: colorScheme,
                            optionLabel = { stringResource(it.nameRes) },
                            onSelect = { settingsChanges = settingsChanges.copy(colorScheme = it) },
                        )
                        RowDivider()
                        ChoiceSetting(
                            title = stringResource(R.string.settings_theme_color),
                            options = BaseColor.entries,
                            selected = settingsChanges.baseColor ?: baseColor,
                            optionLabel = { stringResource(it.nameRes) },
                            onSelect = { settingsChanges = settingsChanges.copy(baseColor = it) },
                        )
                        RowDivider()
                        SwitchSetting(
                            title = stringResource(R.string.settings_full_screen),
                            description = stringResource(R.string.settings_full_screen_desc),
                            checked = settingsChanges.fullScreenEnabled ?: fullScreenEnabled,
                            onCheckedChange = { settingsChanges = settingsChanges.copy(fullScreenEnabled = it) },
                        )
                    }

                    SettingsCategory.CONNECTION -> {
                        val delay = settingsChanges.pollingDelay ?: pollingDelay
                        SliderSetting(
                            title = stringResource(R.string.settings_polling_interval),
                            description = stringResource(R.string.settings_polling_interval_desc),
                            valueLabel = stringResource(R.string.value_ms, delay),
                            value = delay,
                            range = pollingDelayRange,
                            step = 10,
                            onValueChange = { settingsChanges = settingsChanges.copy(pollingDelay = it) },
                        )
                        RowDivider()
                        SwitchSetting(
                            title = stringResource(R.string.settings_save_connection_credentials),
                            description = stringResource(R.string.settings_save_connection_credentials_desc),
                            checked = settingsChanges.saveConnectionCredentials ?: saveCredentials,
                            onCheckedChange = {
                                settingsChanges = settingsChanges.copy(saveConnectionCredentials = it)
                            },
                        )
                    }
                }
            }
        }
    }
}

private enum class SettingsCategory(@StringRes val titleRes: Int) {
    CONTROLS(R.string.settings_section_controls),
    LAYOUT(R.string.settings_section_layout),
    DISPLAY(R.string.settings_section_display),
    CONNECTION(R.string.settings_section_connection),
}

/** Category list on the left, with the screen's actions pinned to the bottom. */
@Composable
private fun SettingsSidebar(
    selected: SettingsCategory,
    onSelect: (SettingsCategory) -> Unit,
    onRestoreDefaults: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = 8.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsCategory.entries.forEach { entry ->
                SidebarItem(
                    label = stringResource(entry.titleRes),
                    selected = entry == selected,
                    onClick = { onSelect(entry) },
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(onClick = onRestoreDefaults, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.reset))
            }
        }
    }
}

@Preview(
    widthDp = PreviewWidthDp,
    heightDp = PreviewHeightDp,
)
@Composable
fun SettingsScreenPreview() {
    PreviewBase {
        SettingsScreen(
            onNavigateBack = {},
            onNavigateToGamepadCustomization = {},
            onNavigateToLayoutEditor = {},
            settingsRepository = SettingsRepository(LocalContext.current)
        )
    }
}
