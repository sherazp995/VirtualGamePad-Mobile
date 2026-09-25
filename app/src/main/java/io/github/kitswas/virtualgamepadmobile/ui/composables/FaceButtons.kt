package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import io.github.kitswas.VGP_Data_Exchange.GameButtons
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.data.PreviewBase
import io.github.kitswas.virtualgamepadmobile.ui.theme.gamepadSurface
import io.github.kitswas.virtualgamepadmobile.ui.theme.gamepadStyle
import io.github.kitswas.virtualgamepadmobile.ui.theme.faceButtonTextStyle


enum class FaceButtonType {
    A, B, X, Y
}

private val FaceButtonType.gameButton: GameButtons
    get() = when (this) {
        FaceButtonType.A -> GameButtons.A
        FaceButtonType.B -> GameButtons.B
        FaceButtonType.X -> GameButtons.X
        FaceButtonType.Y -> GameButtons.Y
    }

private fun faceButtonAt(slot: ClusterSlot): FaceButtonType = when (slot) {
    ClusterSlot.TOP -> FaceButtonType.Y
    ClusterSlot.BOTTOM -> FaceButtonType.A
    ClusterSlot.LEFT -> FaceButtonType.X
    ClusterSlot.RIGHT -> FaceButtonType.B
}

/**
 * How one face button looks: an outlined, see-through circle with a coloured letter.
 * Touch handling lives in [ButtonCluster].
 */
@Composable
fun FaceButton(
    type: FaceButtonType,
    modifier: Modifier = Modifier,
    size: Dp,
    pressed: Boolean,
) {
    val style = gamepadStyle()
    val label = when (type) {
        FaceButtonType.A -> stringResource(R.string.button_a)
        FaceButtonType.B -> stringResource(R.string.button_b)
        FaceButtonType.X -> stringResource(R.string.button_x)
        FaceButtonType.Y -> stringResource(R.string.button_y)
    }
    val letterColour = when (type) {
        FaceButtonType.A -> style.faceA
        FaceButtonType.B -> style.faceB
        FaceButtonType.X -> style.faceX
        FaceButtonType.Y -> style.faceY
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { role = Role.Button }
            .gamepadSurface(CircleShape, pressed, style),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = letterColour,
            textAlign = TextAlign.Center,
            style = faceButtonTextStyle(size),
        )
    }
}

/**
 * The A, B, X, Y buttons on a gamepad, also known as the face buttons.
 * One finger can press several of them at once.
 */
@Composable
fun FaceButtons(
    modifier: Modifier = Modifier,
    size: Dp = 360.dp,
) {
    ButtonCluster(
        size = size,
        buttonFor = { faceButtonAt(it).gameButton },
        modifier = modifier,
    ) { held ->
        ClusterSlot.entries.forEach { slot ->
            FaceButton(
                type = faceButtonAt(slot),
                modifier = Modifier.align(slot.alignment),
                size = 2 * size / 5,
                pressed = slot in held,
            )
        }
    }
}

@Preview(showBackground = false)
@Composable
fun FaceButtonsPreview() {
    PreviewBase {
        FaceButtons(
        )
    }
}
