package io.github.kitswas.virtualgamepadmobile.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.roundToInt

// Pure math behind the visual layout editor. Every edit starts from the control's
// placement and on-screen bounds at the start of the swipe, plus how far the finger has
// travelled since, so the result never drifts however many touch events arrive.

/**
 * Moves the control by exactly [drag] pixels, as far as it can go without leaving
 * [screen]. A control that is already partly off screen can only move back in.
 *
 * @param pxPerOffsetUnit pixels per unit of [Placement.offsetX] / [Placement.offsetY]
 */
fun Placement.movedBy(
    drag: Offset,
    bounds: Rect,
    screen: Rect,
    pxPerOffsetUnit: Float,
): Placement {
    val dx = drag.x.coerceIn(
        minOf(screen.left - bounds.left, 0f),
        maxOf(screen.right - bounds.right, 0f),
    )
    val dy = drag.y.coerceIn(
        minOf(screen.top - bounds.top, 0f),
        maxOf(screen.bottom - bounds.bottom, 0f),
    )
    return copy(
        offsetX = offsetX + dx / pxPerOffsetUnit,
        offsetY = offsetY + dy / pxPerOffsetUnit,
    )
}

/**
 * Resizes the control so its height changes by the finger's vertical travel: swiping
 * up grows it, swiping down shrinks it. Growth stops before the control leaves
 * [screen], and the scale stays inside [SCALE_VALUE_RANGE].
 */
fun Placement.resizedBy(
    dragY: Float,
    bounds: Rect,
    screen: Rect,
    isRtl: Boolean,
): Placement {
    if (bounds.height <= 0f) return this
    val wanted = scale * (bounds.height - dragY) / bounds.height
    val largestFitting = scale * maxOf(largestFitFactor(bounds, screen, isRtl), 1f)
    return copy(scale = wanted.coerceAtMost(largestFitting).coerceIn(SCALE_VALUE_RANGE))
}

/** Angles within this many degrees of a multiple of 45 snap to it. */
private const val ROTATION_SNAP_DEGREES = 4f

/**
 * Turns the control clockwise as the finger moves right, [pxPerDegree] pixels per
 * degree. Lands exactly on 0, 45, 90... when close, so straight is easy to hit.
 */
fun Placement.rotatedBy(dragX: Float, pxPerDegree: Float): Placement {
    val angle = (rotation + dragX / pxPerDegree).mod(360f)
    val nearest45 = (angle / 45f).roundToInt() * 45f
    val snapped = if (abs(angle - nearest45) <= ROTATION_SNAP_DEGREES) nearest45 else angle
    return copy(rotation = snapped.mod(360f))
}

/**
 * How many times bigger the control can get before it leaves [screen]. A control grows
 * away from its anchor (a top left control grows right and down), so the anchor point
 * stays put and only the other edges move.
 */
private fun Placement.largestFitFactor(bounds: Rect, screen: Rect, isRtl: Boolean): Float {
    val (anchorX, anchorY) = anchor.fraction(isRtl)
    val fixedX = bounds.left + bounds.width * anchorX
    val fixedY = bounds.top + bounds.height * anchorY
    return listOfNotNull(
        if (anchorX > 0f) (fixedX - screen.left) / (bounds.width * anchorX) else null,
        if (anchorX < 1f) (screen.right - fixedX) / (bounds.width * (1 - anchorX)) else null,
        if (anchorY > 0f) (fixedY - screen.top) / (bounds.height * anchorY) else null,
        if (anchorY < 1f) (screen.bottom - fixedY) / (bounds.height * (1 - anchorY)) else null,
    ).min()
}

/** Where the anchor sits inside the control: 0 is the left/top edge, 1 the right/bottom. */
private fun ButtonAnchor.fraction(isRtl: Boolean): Pair<Float, Float> {
    // Anchors are start/end aligned, so "left" anchors sit on the right in RTL.
    val start = if (isRtl) 1f else 0f
    val end = 1f - start
    return when (this) {
        ButtonAnchor.TOP_LEFT -> start to 0f
        ButtonAnchor.TOP_CENTER -> 0.5f to 0f
        ButtonAnchor.TOP_RIGHT -> end to 0f
        ButtonAnchor.CENTER_LEFT -> start to 0.5f
        ButtonAnchor.CENTER -> 0.5f to 0.5f
        ButtonAnchor.CENTER_RIGHT -> end to 0.5f
        ButtonAnchor.BOTTOM_LEFT -> start to 1f
        ButtonAnchor.BOTTOM_CENTER -> 0.5f to 1f
        ButtonAnchor.BOTTOM_RIGHT -> end to 1f
    }
}
