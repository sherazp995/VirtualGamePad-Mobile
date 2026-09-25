package io.github.kitswas.virtualgamepadmobile.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One look for every on-screen control: a faint see-through fill, a thin light
 * outline and a clear glyph.
 * Pressing brightens the fill and outline and shrinks the control slightly.
 *
 * Built from the theme's on-surface colour, so it reads on light and dark backgrounds.
 */
@Immutable
data class GamepadStyle(
    val fill: Color,
    val pressedFill: Color,
    val outline: Color,
    val pressedOutline: Color,
    val glyph: Color,
    val outlineWidth: Dp = 1.5.dp,
) {
    /** Face button letters keep their familiar colours, tuned to read on either theme. */
    val faceA = Color(0xFF4CC24A)
    val faceB = Color(0xFFE5484D)
    val faceX = Color(0xFF3B8BF0)
    val faceY = Color(0xFFF2C12E)
}

@Composable
fun gamepadStyle(): GamepadStyle {
    val ink = MaterialTheme.colorScheme.onSurface
    return GamepadStyle(
        fill = ink.copy(alpha = 0.07f),
        pressedFill = ink.copy(alpha = 0.28f),
        outline = ink.copy(alpha = 0.55f),
        pressedOutline = ink.copy(alpha = 0.95f),
        glyph = ink.copy(alpha = 0.9f),
    )
}

/** The see-through, outlined body of a control, in [shape]. */
fun Modifier.gamepadSurface(shape: Shape, pressed: Boolean, style: GamepadStyle): Modifier = this
    .graphicsLayer {
        val scale = if (pressed) 0.94f else 1f
        scaleX = scale
        scaleY = scale
    }
    .clip(shape)
    .background(if (pressed) style.pressedFill else style.fill)
    .border(style.outlineWidth, if (pressed) style.pressedOutline else style.outline, shape)
