package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import io.github.kitswas.VGP_Data_Exchange.GameButtons
import io.github.kitswas.virtualgamepadmobile.data.PadInput
import kotlin.math.roundToInt

/**
 * The face buttons and the D-pad share one shape: four round buttons, each 2/5 of the
 * cluster wide, sitting in the middle of each side of a square.
 *
 * Positions are absolute (not start/end) so the buttons don't swap sides in RTL
 * locales, which keeps them in line with the touch hit test below.
 */
enum class ClusterSlot(val alignment: Alignment) {
    TOP(Alignment.TopCenter),
    BOTTOM(Alignment.BottomCenter),
    LEFT(AbsoluteAlignment.CenterLeft),
    RIGHT(AbsoluteAlignment.CenterRight),
}

private fun ClusterSlot.center(clusterSize: Float): Offset = when (this) {
    ClusterSlot.TOP -> Offset(clusterSize / 2, clusterSize / 5)
    ClusterSlot.BOTTOM -> Offset(clusterSize / 2, clusterSize * 4 / 5)
    ClusterSlot.LEFT -> Offset(clusterSize / 5, clusterSize / 2)
    ClusterSlot.RIGHT -> Offset(clusterSize * 4 / 5, clusterSize / 2)
}

/**
 * The slots a [contact] overlaps, in the cluster's own pixels. A thumb laid flat
 * across three buttons presses those three; a fingertip in a gap presses nothing.
 */
fun slotsUnderContact(contact: Contact, clusterSize: Float): Set<ClusterSlot> =
    ClusterSlot.entries.filterTo(mutableSetOf()) { slot ->
        slot.hitShape(clusterSize).overlaps(contact)
    }

private fun ClusterSlot.hitShape(clusterSize: Float) = HitShape.Circle(center(clusterSize), clusterSize / 5)

/**
 * Which way this slot points on screen once the cluster is turned [degrees] clockwise,
 * rounded to the nearest side. A D-pad turned a quarter turn sends Right from its top arrow.
 */
fun ClusterSlot.rotatedBy(degrees: Float): ClusterSlot {
    val clockwise = listOf(ClusterSlot.TOP, ClusterSlot.RIGHT, ClusterSlot.BOTTOM, ClusterSlot.LEFT)
    val quarterTurns = (degrees / 90f).roundToInt()
    return clockwise[(clockwise.indexOf(this) + quarterTurns).mod(clockwise.size)]
}

/**
 * A D-pad can't point up and down at once, so opposite directions cancel out
 * (a thumb in the middle presses nothing, a thumb just above it presses up).
 */
fun withoutOpposites(slots: Set<ClusterSlot>): Set<ClusterSlot> {
    var result = slots
    if (ClusterSlot.TOP in slots && ClusterSlot.BOTTOM in slots) {
        result = result - ClusterSlot.TOP - ClusterSlot.BOTTOM
    }
    if (ClusterSlot.LEFT in slots && ClusterSlot.RIGHT in slots) {
        result = result - ClusterSlot.LEFT - ClusterSlot.RIGHT
    }
    return result
}

/**
 * A square of four buttons (the face buttons or the D-pad). Touches come from the
 * gamepad's [TouchRouter], so one finger can press several of these, and buttons of
 * other controls too.
 *
 * @param resolveHeld adjusts the touched slots, e.g. to cancel opposite directions
 */
@Composable
fun ButtonCluster(
    size: Dp,
    buttonFor: (ClusterSlot) -> GameButtons,
    modifier: Modifier = Modifier,
    resolveHeld: (Set<ClusterSlot>) -> Set<ClusterSlot> = { it },
    content: @Composable BoxScope.(held: Set<ClusterSlot>) -> Unit,
) {
    val key = remember { Any() }
    val slots = ClusterSlot.entries
    val held = LocalTouchRouter.current?.held(key).orEmpty().mapTo(mutableSetOf()) { slots[it] }

    Box(
        modifier = modifier
            .size(size)
            .touchTarget(
                key = key,
                areas = { measured ->
                    val side = measured.width.toFloat()
                    slots.map { HitArea(it.hitShape(side), PadInput(buttons = buttonFor(it).value)) }
                },
                resolve = { touched ->
                    resolveHeld(touched.mapTo(mutableSetOf()) { slots[it] })
                        .mapTo(mutableSetOf()) { slots.indexOf(it) }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content(held)
    }
}
