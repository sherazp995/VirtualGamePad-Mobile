package io.github.kitswas.virtualgamepadmobile.ui.composables

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.kitswas.virtualgamepadmobile.data.HeldInputs
import io.github.kitswas.virtualgamepadmobile.data.PadInput
import io.github.kitswas.virtualgamepadmobile.data.press
import io.github.kitswas.virtualgamepadmobile.data.release
import io.github.kitswas.virtualgamepadmobile.ui.utils.HapticUtils
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * The part of a finger touching the screen, as Android reports it: an ellipse.
 *
 * @param semiMajor half the long side, in pixels
 * @param semiMinor half the short side, in pixels
 * @param angle direction of the long side, in radians from the x axis
 */
data class Contact(
    val center: Offset,
    val semiMajor: Float,
    val semiMinor: Float,
    val angle: Float = 0f,
)

/** Where a point lands in the contact's own frame, long side along x. */
private fun Contact.toEllipseFrame(point: Offset): Offset {
    val d = point - center
    val cos = cos(-angle)
    val sin = sin(-angle)
    return Offset(d.x * cos - d.y * sin, d.x * sin + d.y * cos)
}

/** The shape of a button's touch area, in the button's own pixels. */
sealed interface HitShape {
    fun overlaps(contact: Contact): Boolean

    data class Circle(val center: Offset, val radius: Float) : HitShape {
        // Grows the ellipse by the radius and checks the centre: exact along both axes,
        // close elsewhere.
        override fun overlaps(contact: Contact): Boolean {
            val p = contact.toEllipseFrame(center)
            val a = contact.semiMajor + radius
            val b = contact.semiMinor + radius
            return (p.x / a) * (p.x / a) + (p.y / b) * (p.y / b) <= 1f
        }
    }

    data class Box(val rect: Rect) : HitShape {
        // Touching when the finger's centre is inside, or the nearest point of the box
        // to it is inside the ellipse.
        override fun overlaps(contact: Contact): Boolean {
            if (rect.contains(contact.center)) return true
            val nearest = Offset(
                contact.center.x.coerceIn(rect.left, rect.right),
                contact.center.y.coerceIn(rect.top, rect.bottom),
            )
            val p = contact.toEllipseFrame(nearest)
            val a = contact.semiMajor
            val b = contact.semiMinor
            return (p.x / a) * (p.x / a) + (p.y / b) * (p.y / b) <= 1f
        }
    }
}

/** One pressable area of a control and what it presses. */
data class HitArea(val shape: HitShape, val input: PadInput)

/** How to reach a control from the gamepad's frame. */
class TargetFrame(
    /** Its size, in its own pixels. */
    val size: IntSize,
    /** Turns a point in the gamepad's frame into one in the control's own frame. */
    val toLocal: (Offset) -> Offset,
    /** How far the control is turned relative to the gamepad, in radians. */
    val turn: Float,
)

/**
 * Every button on the gamepad gets its touches from here, so one finger can press
 * buttons that belong to different controls: X, Y and custom button 1 together, or a
 * thumb sliding from the face buttons onto LB. Android would otherwise hand each
 * touch to the single control it first landed on.
 *
 * Controls register their areas with [touchTarget]; the gamepad passes every finger's
 * [Contact] to [onContacts]. Analog sticks keep their own drag handling.
 */
class TouchRouter(private val reading: GamepadReading, private val view: View?) {
    private class Target(
        val frame: () -> TargetFrame?,
        val areas: (IntSize) -> List<HitArea>,
        val resolve: (Set<Int>) -> Set<Int>,
    )

    private val targets = mutableMapOf<Any, Target>()

    /** What each target's held areas press, remembered so release matches press. */
    private val heldInputs = mutableMapOf<Any, Map<Int, PadInput>>()

    /** Held area indexes per target, observable so controls can draw themselves pressed. */
    private val heldAreas = mutableStateMapOf<Any, Set<Int>>()

    /** The gamepad's own layout, set by the gamepad. */
    var coordinates: LayoutCoordinates? = null

    fun held(key: Any): Set<Int> = heldAreas[key] ?: emptySet()

    fun register(
        key: Any,
        frame: () -> TargetFrame?,
        areas: (IntSize) -> List<HitArea>,
        resolve: (Set<Int>) -> Set<Int>,
    ) {
        targets[key] = Target(frame, areas, resolve)
    }

    /** Forgets a control, releasing whatever it was holding. */
    fun unregister(key: Any) {
        hold(key, emptyMap())
        targets.remove(key)
    }

    fun releaseAll() {
        targets.keys.toList().forEach { hold(it, emptyMap()) }
    }

    /** Works out what every finger presses. [contacts] are in the gamepad's frame. */
    fun onContacts(contacts: List<Contact>) {
        for ((key, target) in targets.toList()) {
            val frame = target.frame()
            if (frame == null) {
                hold(key, emptyMap())
                continue
            }
            val areas = target.areas(frame.size)
            val touched = contacts.flatMapTo(mutableSetOf()) { contact ->
                val local = contact.copy(
                    center = frame.toLocal(contact.center),
                    angle = contact.angle - frame.turn,
                )
                areas.indices.filter { areas[it].shape.overlaps(local) }
            }
            val resolved = target.resolve(touched).filter { it in areas.indices }
            hold(key, resolved.associateWith { areas[it].input })
        }
    }

    private fun hold(key: Any, next: Map<Int, PadInput>) {
        val before = heldInputs[key] ?: emptyMap()
        if (before.keys == next.keys) return
        val holds = HeldInputs.of(reading)
        (before.keys - next.keys).forEach { holds.release(before.getValue(it)) }
        (next.keys - before.keys).forEach { holds.press(next.getValue(it)) }
        view?.let {
            if (next.size > before.size) {
                HapticUtils.performButtonPressFeedback(it)
            } else {
                HapticUtils.performButtonReleaseFeedback(it)
            }
        }
        heldInputs[key] = next
        heldAreas[key] = next.keys
    }
}

/**
 * Keeps fingers that a control handles itself (an analog stick) away from the router
 * for their whole touch. A stick only marks some of its events as handled, e.g. none
 * while the thumb holds still, so checking each event alone would let a resting thumb
 * press nearby buttons.
 */
class PointerClaims {
    private val claimed = mutableSetOf<Long>()

    /** Whether the router may use this pointer now. Call once per pointer per event. */
    fun isFree(id: Long, pressed: Boolean, consumed: Boolean): Boolean {
        if (!pressed) {
            claimed -= id
            return false
        }
        if (consumed) claimed += id
        return id !in claimed
    }
}

/** The gamepad's router, or null outside a gamepad (e.g. in previews). */
val LocalTouchRouter = staticCompositionLocalOf<TouchRouter?> { null }

private fun LayoutCoordinates.turnOnScreen(): Float {
    val xAxis = localToRoot(Offset(1f, 0f)) - localToRoot(Offset.Zero)
    return atan2(xAxis.y, xAxis.x)
}

/**
 * Registers this control's touch areas with the gamepad's [TouchRouter].
 *
 * @param key identifies the control; read its held areas with [TouchRouter.held]
 * @param areas the control's areas for its current size, in its own pixels
 * @param resolve adjusts the set of touched area indexes, e.g. to cancel opposite
 * D-pad directions
 */
@Composable
fun Modifier.touchTarget(
    key: Any,
    areas: (IntSize) -> List<HitArea>,
    resolve: (Set<Int>) -> Set<Int> = { it },
): Modifier {
    val router = LocalTouchRouter.current ?: return this
    val currentAreas by rememberUpdatedState(areas)
    val currentResolve by rememberUpdatedState(resolve)
    val layout = remember { object { var coordinates: LayoutCoordinates? = null } }

    DisposableEffect(router, key) {
        router.register(
            key = key,
            frame = {
                val own = layout.coordinates?.takeIf { it.isAttached }
                val pad = router.coordinates?.takeIf { it.isAttached }
                if (own == null || pad == null) {
                    null
                } else {
                    TargetFrame(
                        size = own.size,
                        toLocal = { own.localPositionOf(pad, it) },
                        turn = own.turnOnScreen() - pad.turnOnScreen(),
                    )
                }
            },
            areas = { currentAreas(it) },
            resolve = { currentResolve(it) },
        )
        onDispose { router.unregister(key) }
    }
    return onGloballyPositioned { layout.coordinates = it }
}

/**
 * The contact of the finger at [position] (gamepad pixels), read from the Android
 * touch event. Pointers are matched by position, since Compose and Android number
 * them differently. Returns null when the screen doesn't report a contact size.
 */
fun contactAt(
    position: Offset,
    event: PointerEvent,
    pad: LayoutCoordinates?,
    maxSemiAxis: Float,
): Contact? {
    val motion = event.motionEvent ?: return null
    val coordinates = pad?.takeIf { it.isAttached } ?: return null
    val inView = coordinates.localToRoot(position)
    val index = (0 until motion.pointerCount).minByOrNull {
        (Offset(motion.getX(it), motion.getY(it)) - inView).getDistanceSquared()
    } ?: return null

    val major = motion.getTouchMajor(index)
    if (major <= 0f) return null
    val minor = motion.getTouchMinor(index).takeIf { it > 0f } ?: major

    // Android: 0 means the long side points up, positive turns clockwise.
    val orientation = motion.getOrientation(index)
    val angleOnScreen = atan2(-cos(orientation), sin(orientation))

    return Contact(
        center = position,
        semiMajor = (major / 2).coerceAtMost(maxSemiAxis),
        semiMinor = (minor / 2).coerceAtMost(maxSemiAxis),
        angle = angleOnScreen - coordinates.turnOnScreen(),
    )
}

/**
 * A control that is one button all over: pressing anywhere on it presses [input].
 * Read whether it's held with [isHeld].
 */
@Composable
fun Modifier.buttonTarget(key: Any, input: PadInput, round: Boolean): Modifier =
    touchTarget(key, areas = { size ->
        val width = size.width.toFloat()
        val height = size.height.toFloat()
        val shape = if (round) {
            HitShape.Circle(Offset(width / 2, height / 2), minOf(width, height) / 2)
        } else {
            HitShape.Box(Rect(0f, 0f, width, height))
        }
        listOf(HitArea(shape, input))
    })

@Composable
fun isHeld(key: Any): Boolean = LocalTouchRouter.current?.held(key)?.isNotEmpty() == true
