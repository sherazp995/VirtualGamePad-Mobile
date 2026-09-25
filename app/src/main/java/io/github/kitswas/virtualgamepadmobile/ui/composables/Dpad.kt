package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import io.github.kitswas.VGP_Data_Exchange.GameButtons
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.data.PreviewBase
import io.github.kitswas.virtualgamepadmobile.ui.theme.GamepadStyle
import io.github.kitswas.virtualgamepadmobile.ui.theme.gamepadStyle

enum class DpadButtonType {
    UP, DOWN, LEFT, RIGHT
}

private val DpadButtonType.gameButton: GameButtons
    get() = when (this) {
        DpadButtonType.UP -> GameButtons.DPadUp
        DpadButtonType.DOWN -> GameButtons.DPadDown
        DpadButtonType.LEFT -> GameButtons.DPadLeft
        DpadButtonType.RIGHT -> GameButtons.DPadRight
    }

private fun dpadButtonAt(slot: ClusterSlot): DpadButtonType = when (slot) {
    ClusterSlot.TOP -> DpadButtonType.UP
    ClusterSlot.BOTTOM -> DpadButtonType.DOWN
    ClusterSlot.LEFT -> DpadButtonType.LEFT
    ClusterSlot.RIGHT -> DpadButtonType.RIGHT
}

/** How far each slot's arm is turned from the up arm, clockwise. */
private val ClusterSlot.armDegrees: Float
    get() = when (this) {
        ClusterSlot.TOP -> 0f
        ClusterSlot.RIGHT -> 90f
        ClusterSlot.BOTTOM -> 180f
        ClusterSlot.LEFT -> 270f
    }

/**
 * The up arm of the D-pad in a [size] square: straight from the outer edge,
 * then tapering at 45 degrees to a point short of the middle. With every arm the same,
 * neighbouring edges run parallel with an even V-shaped gap between them.
 */
private fun upArm(size: Float): Path {
    val middle = size / 2
    val halfWidth = size * 0.155f
    val shoulder = size * 0.26f
    return Path().apply {
        moveTo(middle - halfWidth, 0f)
        lineTo(middle + halfWidth, 0f)
        lineTo(middle + halfWidth, shoulder)
        lineTo(middle, shoulder + halfWidth)
        lineTo(middle - halfWidth, shoulder)
        close()
    }
}

/** A small outlined triangle in the straight part of the up arm, pointing out. */
private fun upChevron(size: Float): Path {
    val middle = size / 2
    return Path().apply {
        moveTo(middle, size * 0.09f)
        lineTo(middle + size * 0.055f, size * 0.17f)
        lineTo(middle - size * 0.055f, size * 0.17f)
        close()
    }
}

private fun DrawScope.drawArm(slot: ClusterSlot, pressed: Boolean, style: GamepadStyle) {
    val stroke = Stroke(width = style.outlineWidth.toPx(), join = StrokeJoin.Round)
    rotate(slot.armDegrees, pivot = Offset(size.width / 2, size.height / 2)) {
        val arm = upArm(size.width)
        // Outline only; the arm fills in while pressed.
        if (pressed) drawPath(arm, style.pressedFill)
        drawPath(arm, if (pressed) style.pressedOutline else style.outline, style = stroke)
        val chevron = upChevron(size.width)
        if (pressed) drawPath(chevron, style.glyph)
        drawPath(chevron, style.glyph, style = stroke)
    }
}

/**
 * A directional pad with up, down, left, and right arrow-shaped arms.
 * A thumb across two arms presses the diagonal.
 *
 * @param rotation how far the pad is turned clockwise. Directions follow the screen,
 * so an arm pointing right always sends Right.
 */
@Composable
fun Dpad(
    modifier: Modifier = Modifier,
    size: Dp = 360.dp,
    rotation: Float = 0f,
) {
    val style = gamepadStyle()
    ButtonCluster(
        size = size,
        buttonFor = { dpadButtonAt(it.rotatedBy(rotation)).gameButton },
        modifier = modifier,
        resolveHeld = ::withoutOpposites,
    ) { held ->
        Canvas(modifier = Modifier.fillMaxSize()) {
            ClusterSlot.entries.forEach { slot -> drawArm(slot, slot in held, style) }
        }
        // Invisible, one per arm, so screen readers and tests can find each direction.
        ClusterSlot.entries.forEach { slot ->
            val description = stringResource(R.string.content_desc_dpad_button, dpadButtonAt(slot).name)
            Box(
                modifier = Modifier
                    .align(slot.alignment)
                    .size(2 * size / 5)
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                    }
            )
        }
    }
}

@Preview(showBackground = false)
@Composable
fun DpadPreview() {
    PreviewBase {
        Dpad(
        )
    }
}
