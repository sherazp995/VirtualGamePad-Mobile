package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.kitswas.virtualgamepadmobile.data.CustomButton
import io.github.kitswas.virtualgamepadmobile.data.toPadInput
import io.github.kitswas.virtualgamepadmobile.ui.theme.gamepadStyle
import io.github.kitswas.virtualgamepadmobile.ui.theme.gamepadSurface
import io.github.kitswas.virtualgamepadmobile.ui.theme.faceButtonTextStyle

/**
 * A user-made button shown as its number. Held down, it presses all of its inputs.
 * Touches come from the gamepad's [TouchRouter], so one finger can press it together
 * with buttons of other controls.
 */
@Composable
fun ComboButton(
    button: CustomButton,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val key = remember { Any() }
    val held = isHeld(key)

    val style = gamepadStyle()
    Box(
        modifier = modifier
            .size(size)
            .buttonTarget(key, button.inputs.toPadInput(), round = true)
            .semantics { role = Role.Button }
            .gamepadSurface(CircleShape, held, style),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            button.number.toString(),
            color = style.glyph,
            style = faceButtonTextStyle(size),
        )
    }
}
