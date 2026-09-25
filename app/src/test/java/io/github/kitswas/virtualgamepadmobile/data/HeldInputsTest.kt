package io.github.kitswas.virtualgamepadmobile.data

import io.github.kitswas.VGP_Data_Exchange.GameButtons
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import org.junit.Assert.assertEquals
import org.junit.Test

class HeldInputsTest {

    private fun isDown(reading: GamepadReading, button: GameButtons) =
        reading.ButtonsDown and button.value != 0

    @Test
    fun `releasing a combo keeps a button another control still holds`() {
        val reading = GamepadReading()
        val holds = HeldInputs.of(reading)
        val combo = setOf(ComboInput.A, ComboInput.B).toPadInput()
        holds.press(GameButtons.A.value) // thumb on the A button
        holds.press(combo)
        assertEquals(true, isDown(reading, GameButtons.B))

        holds.release(combo)

        assertEquals(true, isDown(reading, GameButtons.A))
        assertEquals(false, isDown(reading, GameButtons.B))
    }

    @Test
    fun `a trigger stays pulled until its last holder lets go`() {
        val reading = GamepadReading()
        val holds = HeldInputs.of(reading)
        val combo = setOf(ComboInput.LEFT_TRIGGER).toPadInput()
        holds.pressTrigger(left = true)
        holds.press(combo)

        holds.release(combo)
        assertEquals(1f, reading.LeftTrigger)

        holds.releaseTrigger(left = true)
        assertEquals(0f, reading.LeftTrigger)
    }
}
