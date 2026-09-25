package io.github.kitswas.virtualgamepadmobile.data

import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import java.lang.ref.WeakReference

/**
 * Several controls can hold the same input: a combo button and the A button both hold
 * A. This counts the holders of every button and trigger, so releasing one control
 * only releases what no other control still holds.
 *
 * Every control writes to the reading through here. Main thread only.
 */
class HeldInputs private constructor(reading: GamepadReading) {
    private val readingRef = WeakReference(reading)
    private val buttonHolders = IntArray(Int.SIZE_BITS)
    private var leftTriggerHolders = 0
    private var rightTriggerHolders = 0

    fun press(buttons: Int) {
        var newlyDown = 0
        forEachBit(buttons) { bit, index ->
            if (buttonHolders[index]++ == 0) newlyDown = newlyDown or bit
        }
        val reading = readingRef.get() ?: return
        reading.ButtonsDown = reading.ButtonsDown or newlyDown
    }

    fun release(buttons: Int) {
        var nowUp = 0
        forEachBit(buttons) { bit, index ->
            if (buttonHolders[index] > 0 && --buttonHolders[index] == 0) nowUp = nowUp or bit
        }
        val reading = readingRef.get() ?: return
        reading.ButtonsDown = reading.ButtonsDown and nowUp.inv()
        reading.ButtonsUp = reading.ButtonsUp or nowUp
    }

    fun pressTrigger(left: Boolean) {
        val reading = readingRef.get() ?: return
        if (left) {
            leftTriggerHolders++
            reading.LeftTrigger = 1f
        } else {
            rightTriggerHolders++
            reading.RightTrigger = 1f
        }
    }

    fun releaseTrigger(left: Boolean) {
        val reading = readingRef.get() ?: return
        if (left) {
            leftTriggerHolders = (leftTriggerHolders - 1).coerceAtLeast(0)
            if (leftTriggerHolders == 0) reading.LeftTrigger = 0f
        } else {
            rightTriggerHolders = (rightTriggerHolders - 1).coerceAtLeast(0)
            if (rightTriggerHolders == 0) reading.RightTrigger = 0f
        }
    }

    private inline fun forEachBit(mask: Int, action: (bit: Int, index: Int) -> Unit) {
        for (index in 0 until Int.SIZE_BITS) {
            val bit = 1 shl index
            if (mask and bit != 0) action(bit, index)
        }
    }

    companion object {
        // Matched by identity: GamepadReading's equals compares field values, which change
        // with every press, so a hash map would lose track of it.
        private val all = mutableListOf<HeldInputs>()

        /** The shared counter for [reading], so every control on one gamepad agrees. */
        fun of(reading: GamepadReading): HeldInputs {
            all.removeAll { it.readingRef.get() == null }
            return all.firstOrNull { it.readingRef.get() === reading }
                ?: HeldInputs(reading).also { all += it }
        }
    }
}

/** What one button presses: gamepad buttons (a GameButtons bit mask) and triggers. */
data class PadInput(
    val buttons: Int = 0,
    val leftTrigger: Boolean = false,
    val rightTrigger: Boolean = false,
)

fun HeldInputs.press(input: PadInput) {
    press(input.buttons)
    if (input.leftTrigger) pressTrigger(left = true)
    if (input.rightTrigger) pressTrigger(left = false)
}

fun HeldInputs.release(input: PadInput) {
    release(input.buttons)
    if (input.leftTrigger) releaseTrigger(left = true)
    if (input.rightTrigger) releaseTrigger(left = false)
}
