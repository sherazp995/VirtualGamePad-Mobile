package io.github.kitswas.virtualgamepadmobile.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.data.ButtonComponent
import io.github.kitswas.virtualgamepadmobile.data.ButtonConfig
import io.github.kitswas.virtualgamepadmobile.data.ComboInput
import io.github.kitswas.virtualgamepadmobile.data.CustomButton
import io.github.kitswas.virtualgamepadmobile.data.LayoutItem
import io.github.kitswas.virtualgamepadmobile.data.Placement
import io.github.kitswas.virtualgamepadmobile.data.SettingsRepository
import io.github.kitswas.virtualgamepadmobile.data.defaultButtonConfigs
import io.github.kitswas.virtualgamepadmobile.data.movedBy
import io.github.kitswas.virtualgamepadmobile.data.nextFreeNumber
import io.github.kitswas.virtualgamepadmobile.data.resizedBy
import io.github.kitswas.virtualgamepadmobile.data.rotatedBy
import io.github.kitswas.virtualgamepadmobile.ui.composables.DrawGamepad
import io.github.kitswas.virtualgamepadmobile.ui.composables.SidebarItem
import io.github.kitswas.virtualgamepadmobile.ui.utils.jsonSaver
import kotlinx.coroutines.runBlocking

private const val logTag = "LayoutEditor"

private enum class EditMode(@StringRes val labelRes: Int, @StringRes val hintRes: Int) {
    MOVE(R.string.layout_editor_move, R.string.layout_editor_hint_move),
    RESIZE(R.string.layout_editor_resize, R.string.layout_editor_hint_resize),
    ROTATE(R.string.layout_editor_rotate, R.string.layout_editor_hint_rotate),
}

/**
 * Edit the gamepad layout on a preview of the gamepad itself.
 *
 * Tap a control to select it, then swipe anywhere on the preview: Move follows the
 * swipe, Resize grows on up and shrinks on down, Rotate turns on left and right. The
 * control never follows the finger's position, only its movement, so the finger
 * doesn't cover what it is editing. Custom combo buttons are added, edited and
 * deleted here too.
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun LayoutEditorScreen(
    onNavigateBack: () -> Unit,
    settingsRepository: SettingsRepository,
) {
    val savedConfigs by settingsRepository.buttonConfigs
        .collectAsState<Map<ButtonComponent, ButtonConfig>, Map<ButtonComponent, ButtonConfig>?>(initial = null)
    val savedCustomButtons by settingsRepository.customButtons
        .collectAsState<List<CustomButton>, List<CustomButton>?>(initial = null)
    // Wait for the saved layout so the first swipe doesn't start from the defaults.
    val initialConfigs = savedConfigs ?: return
    val initialCustomButtons = savedCustomButtons ?: return

    // Saveable so a dark mode or language change mid-edit keeps the unsaved changes.
    var editedConfigs by rememberSaveable(stateSaver = jsonSaver<Map<ButtonComponent, ButtonConfig>>()) {
        mutableStateOf(null)
    }
    var editedCustomButtons by rememberSaveable(stateSaver = jsonSaver<List<CustomButton>>()) {
        mutableStateOf(null)
    }

    // Functions, not vals, so the long-lived gesture handler always sees the latest edit.
    fun configs() = editedConfigs ?: initialConfigs
    fun customButtons() = editedCustomButtons ?: initialCustomButtons

    var mode by rememberSaveable { mutableStateOf(EditMode.MOVE) }
    var selected by rememberSaveable { mutableStateOf<LayoutItem?>(null) }
    // The button the combo dialog is open for: an existing one, or a new one not added yet.
    var comboBeingEdited by remember { mutableStateOf<CustomButton?>(null) }

    val itemBounds = remember { mutableStateMapOf<LayoutItem, Rect>() }
    // The scaled-down gamepad (controls stay inside it) and the whole area right of the sidebar
    // (swipes work anywhere in it), both in window pixels.
    var frameBounds by remember { mutableStateOf(Rect.Zero) }
    var editAreaBounds by remember { mutableStateOf(Rect.Zero) }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val highlight = MaterialTheme.colorScheme.tertiary
    val gamepadState = remember { GamepadReading() }

    fun configOf(component: ButtonComponent) = configs()[component] ?: ButtonConfig.default(component)
    fun customButtonOf(id: String) = customButtons().firstOrNull { it.id == id }

    fun placementOf(item: LayoutItem): Placement? = when (item) {
        is LayoutItem.BuiltIn -> configOf(item.component).placement
        is LayoutItem.Custom -> customButtonOf(item.id)?.placement
    }

    fun setPlacement(item: LayoutItem, placement: Placement) {
        when (item) {
            is LayoutItem.BuiltIn -> editedConfigs =
                configs() + (item.component to configOf(item.component).with(placement))

            is LayoutItem.Custom -> editedCustomButtons = customButtons().map {
                if (it.id == item.id) it.copy(placement = placement) else it
            }
        }
    }

    // Built-in controls first, then custom buttons by number, hidden ones included.
    fun allItems(): List<LayoutItem> =
        ButtonComponent.entries.map { LayoutItem.BuiltIn(it) } +
            customButtons().sortedBy { it.number }.map { LayoutItem.Custom(it.id) }

    fun selectNext(step: Int) {
        val all = allItems()
        val index = all.indexOf(selected)
        selected = when {
            index >= 0 -> all[(index + step).mod(all.size)]
            step > 0 -> all.first()
            else -> all.last()
        }
    }

    // Smallest control under the tap, so a small button on top of a big one stays reachable.
    fun itemAt(point: Offset): LayoutItem? = itemBounds
        .filter { (item, bounds) -> placementOf(item) != null && bounds.contains(point) }
        .minByOrNull { (_, bounds) -> bounds.width * bounds.height }
        ?.key

    fun saveCombo(button: CustomButton) {
        val existing = customButtonOf(button.id)
        editedCustomButtons = if (existing != null) {
            customButtons().map { if (it.id == button.id) button else it }
        } else {
            customButtons() + button
        }
        selected = LayoutItem.Custom(button.id)
    }

    fun deleteSelectedCustomButton() {
        val item = selected as? LayoutItem.Custom ?: return
        editedCustomButtons = customButtons().filterNot { it.id == item.id }
        itemBounds.remove(item)
        selected = null
    }

    comboBeingEdited?.let { button ->
        ComboDialog(
            button = button,
            onConfirm = {
                saveCombo(it)
                comboBeingEdited = null
            },
            onDismiss = { comboBeingEdited = null },
        )
    }

    val current = selected
    val selectedCustom = (current as? LayoutItem.Custom)?.let { customButtonOf(it.id) }
    val selectedName = when (current) {
        is LayoutItem.BuiltIn -> stringResource(current.component.nameRes)
        is LayoutItem.Custom -> selectedCustom?.let {
            stringResource(R.string.layout_editor_custom_button, it.number)
        }

        null -> null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        EditorTopBar(
            selectedName = selectedName,
            hint = stringResource(if (selectedName == null) R.string.layout_editor_hint_select else mode.hintRes),
            onPrevious = { selectNext(-1) },
            onNext = { selectNext(1) },
            onResetLayout = { editedConfigs = defaultButtonConfigs },
            onCancel = onNavigateBack,
            onSave = {
                // Blocking like the other settings screens: leaving the screen would cancel a coroutine.
                runBlocking {
                    editedConfigs?.let { settingsRepository.setAllButtonConfigs(it) }
                    editedCustomButtons?.let { settingsRepository.setCustomButtons(it) }
                }
                Log.i(logTag, "Layout saved")
                onNavigateBack()
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.primary)

        Row(modifier = Modifier.weight(1f)) {
            EditorTools(
                mode = mode,
                onModeChange = { mode = it },
                hasSelection = selectedName != null,
                selectedVisible = current?.let { placementOf(it)?.visible } ?: true,
                customSelected = selectedCustom != null,
                canAddButton = customButtons().nextFreeNumber() != null,
                onToggleVisibility = {
                    current?.let { item ->
                        placementOf(item)?.let { setPlacement(item, it.copy(visible = !it.visible)) }
                    }
                },
                onAddButton = {
                    customButtons().nextFreeNumber()?.let {
                        comboBeingEdited = CustomButton(number = it, inputs = emptySet())
                    }
                },
                onEditButton = { comboBeingEdited = selectedCustom },
                onDeleteButton = ::deleteSelectedCustomButton,
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .onGloballyPositioned { editAreaBounds = it.boundsInWindow() },
                contentAlignment = Alignment.Center,
            ) {
                // The real gamepad is full screen, so draw it at full size and shrink it to fit.
                // Positions and sizes keep their true proportions.
                val scale = minOf(maxWidth / screenWidthDp.dp, maxHeight / screenHeightDp.dp) * 0.96f
                Box(
                    modifier = Modifier
                        .requiredSize(screenWidthDp.dp, screenHeightDp.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .border(2.dp, MaterialTheme.colorScheme.outline)
                        .onGloballyPositioned { frameBounds = it.boundsInWindow() }
                ) {
                    DrawGamepad(
                        widthDp = screenWidthDp,
                        heightDp = screenHeightDp,
                        gamepadState = gamepadState,
                        buttonConfigs = configs(),
                        customButtons = customButtons(),
                        itemModifier = { item ->
                            val tracked = Modifier.onGloballyPositioned {
                                itemBounds[item] = it.boundsInWindow()
                            }
                            if (item == selected) {
                                val shape = RoundedCornerShape(12.dp)
                                tracked
                                    .border(3.dp, highlight, shape)
                                    .background(highlight.copy(alpha = 0.25f), shape)
                            } else {
                                tracked
                            }
                        },
                        showHidden = true,
                    )
                }

                // Covers the whole edit area so editing never presses a button.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val item = selected
                                val start = item?.let { placementOf(it) }
                                val startBounds = item?.let { itemBounds[it] }
                                val gestureMode = mode
                                var travelled = Offset.Zero
                                var swiping = false

                                while (true) {
                                    val change = awaitPointerEvent().changes
                                        .firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    travelled += change.positionChange()
                                    change.consume()
                                    if (!swiping && travelled.getDistance() > viewConfiguration.touchSlop) {
                                        swiping = true
                                    }
                                    if (!swiping || item == null || start == null || startBounds == null) continue

                                    // Bounds are measured on the shrunk preview, so the control moves
                                    // exactly as far as the finger does on screen.
                                    val edited = when (gestureMode) {
                                        EditMode.MOVE -> start.movedBy(
                                            travelled, startBounds, frameBounds,
                                            pxPerOffsetUnit = frameBounds.height,
                                        )

                                        EditMode.RESIZE -> start.resizedBy(
                                            travelled.y, startBounds, frameBounds, isRtl
                                        )

                                        // Swiping across the preview's height turns it half a circle.
                                        EditMode.ROTATE -> start.rotatedBy(
                                            travelled.x, pxPerDegree = frameBounds.height / 180f
                                        )
                                    }
                                    setPlacement(item, edited)
                                }

                                if (!swiping) {
                                    itemAt(down.position + editAreaBounds.topLeft)?.let { selected = it }
                                }
                            }
                        }
                )
            }
        }
    }
}

/** Picks what a custom button presses. */
@Composable
private fun ComboDialog(
    button: CustomButton,
    onConfirm: (CustomButton) -> Unit,
    onDismiss: () -> Unit,
) {
    var inputs by remember(button.id) { mutableStateOf(button.inputs) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.layout_editor_combo_title, button.number)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.layout_editor_combo_desc),
                    style = MaterialTheme.typography.bodySmall,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ComboInput.entries.forEach { input ->
                        val ticked = input in inputs
                        FilterChip(
                            selected = ticked,
                            onClick = { inputs = if (ticked) inputs - input else inputs + input },
                            label = { Text(stringResource(input.nameRes)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(button.copy(inputs = inputs)) },
                enabled = inputs.isNotEmpty(),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Across the top: which control is selected (with arrows to step through them), what a
 * swipe does now, and the screen's actions.
 */
@Composable
private fun EditorTopBar(
    selectedName: String?,
    hint: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onResetLayout: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    // A Surface, not a plain background, so text and icons get the theme's content colour.
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.layout_editor_previous),
                )
            }
            Text(
                selectedName ?: stringResource(R.string.layout_editor_none_selected),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
            )
            IconButton(onClick = onNext) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.layout_editor_next),
                )
            }
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            TextButton(onClick = onResetLayout) {
                Text(stringResource(R.string.reset))
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(stringResource(R.string.cancel))
            }
            Button(onClick = onSave, modifier = Modifier.padding(end = 4.dp)) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

/** Tool column on the left: the edit modes and actions on the selection. */
@Composable
private fun EditorTools(
    mode: EditMode,
    onModeChange: (EditMode) -> Unit,
    hasSelection: Boolean,
    selectedVisible: Boolean,
    customSelected: Boolean,
    canAddButton: Boolean,
    onToggleVisibility: () -> Unit,
    onAddButton: () -> Unit,
    onEditButton: () -> Unit,
    onDeleteButton: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(190.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            EditMode.entries.forEach { entry ->
                SidebarItem(
                    label = stringResource(entry.labelRes),
                    selected = mode == entry,
                    onClick = { onModeChange(entry) },
                )
            }
            SidebarItem(
                label = stringResource(
                    if (selectedVisible) R.string.layout_editor_hide else R.string.layout_editor_show
                ),
                enabled = hasSelection,
                onClick = onToggleVisibility,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SidebarItem(
                label = stringResource(R.string.layout_editor_add_button),
                enabled = canAddButton,
                onClick = onAddButton,
            )
            SidebarItem(
                label = stringResource(R.string.layout_editor_edit_button),
                enabled = customSelected,
                onClick = onEditButton,
            )
            SidebarItem(
                label = stringResource(R.string.layout_editor_delete_button),
                enabled = customSelected,
                onClick = onDeleteButton,
            )
        }
    }
}
