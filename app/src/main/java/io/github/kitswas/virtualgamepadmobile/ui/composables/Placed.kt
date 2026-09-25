package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import io.github.kitswas.virtualgamepadmobile.data.ButtonAnchor
import io.github.kitswas.virtualgamepadmobile.data.Placement

private fun ButtonAnchor.toAlignment(): Alignment = when (this) {
    ButtonAnchor.TOP_LEFT -> Alignment.TopStart
    ButtonAnchor.TOP_CENTER -> Alignment.TopCenter
    ButtonAnchor.TOP_RIGHT -> Alignment.TopEnd
    ButtonAnchor.CENTER_LEFT -> Alignment.CenterStart
    ButtonAnchor.CENTER -> Alignment.Center
    ButtonAnchor.CENTER_RIGHT -> Alignment.CenterEnd
    ButtonAnchor.BOTTOM_LEFT -> Alignment.BottomStart
    ButtonAnchor.BOTTOM_CENTER -> Alignment.BottomCenter
    ButtonAnchor.BOTTOM_RIGHT -> Alignment.BottomEnd
}

/**
 * Puts one gamepad control where its [placement] says: aligned to its anchor, shifted
 * by its offset (a multiple of [baseDp]) and rotated. [content] receives the modifier
 * to apply to the control itself.
 *
 * @param showHidden draw a hidden control faded instead of leaving it out
 * @param itemModifier extra modifier after the placement (the layout editor's highlight)
 */
@Composable
internal fun Placed(
    placement: Placement,
    baseDp: Int,
    showHidden: Boolean,
    itemModifier: Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    if (!placement.visible && !showHidden) return
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = placement.anchor.toAlignment()
    ) {
        content(
            Modifier
                .offset(
                    x = (placement.offsetX * baseDp).dp,
                    y = (placement.offsetY * baseDp).dp
                )
                .alpha(if (placement.visible) 1f else 0.35f)
                .rotate(placement.rotation)
                .then(itemModifier)
        )
    }
}
