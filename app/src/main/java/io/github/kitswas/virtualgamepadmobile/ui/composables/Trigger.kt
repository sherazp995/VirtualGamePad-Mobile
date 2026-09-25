package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.data.PadInput
import io.github.kitswas.virtualgamepadmobile.ui.theme.gamepadStyle
import io.github.kitswas.virtualgamepadmobile.ui.theme.gamepadSurface

enum class TriggerType {
    LEFT, RIGHT
}

@Composable
fun Trigger(
    type: TriggerType,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    val key = remember { Any() }
    val isPressed = isHeld(key)
    val input = when (type) {
        TriggerType.LEFT -> PadInput(leftTrigger = true)
        TriggerType.RIGHT -> PadInput(rightTrigger = true)
    }

    val label = when (type) {
        TriggerType.LEFT -> stringResource(R.string.button_lt)
        TriggerType.RIGHT -> stringResource(R.string.button_rt)
    }

    // Wide and low, with the outer side slanting in towards the top.
    val triggerShape = GenericShape { bounds, _ ->
        val slant = bounds.height * 0.45f
        when (type) {
            TriggerType.LEFT -> {
                moveTo(slant, 0f)
                lineTo(bounds.width, 0f)
                lineTo(bounds.width, bounds.height)
                lineTo(0f, bounds.height)
            }

            TriggerType.RIGHT -> {
                moveTo(0f, 0f)
                lineTo(bounds.width - slant, 0f)
                lineTo(bounds.width, bounds.height)
                lineTo(0f, bounds.height)
            }
        }
        close()
    }

    val style = gamepadStyle()
    Box(
        modifier = modifier
            .width(size * 1.9f)
            .height(size * 0.8f)
            .buttonTarget(key, input, round = false)
            .semantics { role = Role.Button }
            .gamepadSurface(triggerShape, isPressed, style),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = style.glyph, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview
@Composable
fun TriggerPreview() {
    Row {
        Trigger(
            type = TriggerType.LEFT,
            size = 64.dp,
        )
        Trigger(
            type = TriggerType.RIGHT,
            size = 64.dp,
        )
    }
}
