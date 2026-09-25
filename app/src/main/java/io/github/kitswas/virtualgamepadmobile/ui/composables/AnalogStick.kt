package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.kitswas.VGP_Data_Exchange.GameButtons
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.kitswas.virtualgamepadmobile.data.HeldInputs
import io.github.kitswas.virtualgamepadmobile.ui.theme.gamepadStyle
import io.github.kitswas.virtualgamepadmobile.ui.theme.gamepadSurface
import io.github.kitswas.virtualgamepadmobile.ui.utils.HapticUtils
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class AnalogStickType {
    LEFT, RIGHT
}

private class AnalogStickUiState {
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    var visualX by mutableFloatStateOf(0f)
    var visualY by mutableFloatStateOf(0f)
    var isPressed by mutableStateOf(false)
}

private fun updateThumbstickValues(
    type: AnalogStickType,
    gamepadState: GamepadReading,
    x: Float,
    y: Float,
) {
    when (type) {
        AnalogStickType.LEFT -> {
            gamepadState.LeftThumbstickX = x
            gamepadState.LeftThumbstickY = y
        }

        AnalogStickType.RIGHT -> {
            gamepadState.RightThumbstickX = x
            gamepadState.RightThumbstickY = y
        }
    }
}

private fun thumbstickButtonMask(type: AnalogStickType): Int = when (type) {
    AnalogStickType.LEFT -> GameButtons.LeftThumbstick.value
    AnalogStickType.RIGHT -> GameButtons.RightThumbstick.value
}

private fun resetAnalogStick(
    state: AnalogStickUiState,
    type: AnalogStickType,
    gamepadState: GamepadReading,
) {
    state.offsetX = 0f
    state.offsetY = 0f
    state.visualX = 0f
    state.visualY = 0f
    updateThumbstickValues(type, gamepadState, 0f, 0f)
}

/**
 * Turns a direction measured inside a stick rotated [degrees] clockwise into the
 * direction on screen, so pushing right always sends right whatever the rotation.
 */
internal fun screenDirection(x: Float, y: Float, degrees: Float): Offset {
    val radians = Math.toRadians(degrees.toDouble())
    val cos = cos(radians).toFloat()
    val sin = sin(radians).toFloat()
    return Offset(x * cos - y * sin, x * sin + y * cos)
}

private fun handleAnalogDrag(
    state: AnalogStickUiState,
    dragX: Float,
    dragY: Float,
    maxOffset: Float,
    rotation: Float,
    type: AnalogStickType,
    gamepadState: GamepadReading,
    view: android.view.View,
) {
    state.offsetX += dragX
    state.offsetY += dragY

    val magnitude = sqrt(state.offsetX * state.offsetX + state.offsetY * state.offsetY)
    val normalizedDistance = (magnitude / maxOffset).coerceIn(0f, 1f)
    if (normalizedDistance > 0.3f) {
        HapticUtils.performAnalogMovementFeedback(view, normalizedDistance)
    }

    if (magnitude <= 0f) {
        return
    }

    val scaleFactor = if (magnitude > maxOffset) maxOffset / magnitude else 1f
    state.visualX = state.offsetX * scaleFactor
    state.visualY = state.offsetY * scaleFactor
    // The knob moves in the stick's own (rotated) frame; the PC gets the screen direction.
    val onScreen = screenDirection(state.visualX, state.visualY, rotation)
    updateThumbstickValues(type, gamepadState, onScreen.x / maxOffset, onScreen.y / maxOffset)
}

private fun toggleThumbstickButton(
    state: AnalogStickUiState,
    type: AnalogStickType,
    gamepadState: GamepadReading,
    view: android.view.View,
) {
    val mask = thumbstickButtonMask(type)
    if (!state.isPressed) {
        state.isPressed = true
        HeldInputs.of(gamepadState).press(mask)
        HapticUtils.performButtonPressFeedback(view)
        return
    }

    state.isPressed = false
    HeldInputs.of(gamepadState).release(mask)
    HapticUtils.performGestureEndFeedback(view)
}

@Composable
fun AnalogStick(
    modifier: Modifier = Modifier,
    outerCircleWidth: Dp = 4.dp,
    innerCircleRadius: Dp = 32.dp,
    rotation: Float = 0f,
    gamepadState: GamepadReading,
    type: AnalogStickType,
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val style = gamepadStyle()
    val state = remember { AnalogStickUiState() }
    var dragging by remember { mutableStateOf(false) }

    // Calculate maximum offset once
    val maxOffset = with(density) {
        (innerCircleRadius + outerCircleWidth).toPx()
    }

    // The base: an outlined, see-through disc the knob travels inside.
    Box(
        modifier = modifier
            .testTag("AnalogStick_${type.name}")
            .size((innerCircleRadius + outerCircleWidth) * 2)
            .gamepadSurface(CircleShape, pressed = false, style = style),
        contentAlignment = Alignment.Center
    ) {
        // The knob: solid enough to find at a glance, brighter while held.
        val knobActive = dragging || state.isPressed
        Box(
            modifier = Modifier
                .testTag("AnalogStick_${type.name}_Handle")
                .size(innerCircleRadius * 2)
                .offset {
                    IntOffset(
                        state.visualX.roundToInt(),
                        state.visualY.roundToInt()
                    )
                }
                .clip(CircleShape)
                .background(style.glyph.copy(alpha = if (knobActive) 0.45f else 0.22f))
                .border(
                    style.outlineWidth,
                    if (knobActive) style.pressedOutline else style.outline,
                    CircleShape
                )
                .pointerInput(rotation) {
                    detectDragGestures(
                        onDragStart = { _ ->
                            dragging = true
                            HapticUtils.performGestureStartFeedback(view)
                        },
                        onDragEnd = {
                            dragging = false
                            resetAnalogStick(state, type, gamepadState)
                            HapticUtils.performGestureEndFeedback(view)
                        },
                        onDragCancel = {
                            dragging = false
                            resetAnalogStick(state, type, gamepadState)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            handleAnalogDrag(
                                state = state,
                                dragX = dragAmount.x,
                                dragY = dragAmount.y,
                                maxOffset = maxOffset,
                                rotation = rotation,
                                type = type,
                                gamepadState = gamepadState,
                                view = view,
                            )
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { _ ->
                            toggleThumbstickButton(state, type, gamepadState, view)
                        }
                    )
                }
        )
    }
}

@Preview(showBackground = false)
@Composable
fun AnalogStickPreview() {
    AnalogStick(
        gamepadState = GamepadReading(),
        type = AnalogStickType.LEFT,
    )
}
