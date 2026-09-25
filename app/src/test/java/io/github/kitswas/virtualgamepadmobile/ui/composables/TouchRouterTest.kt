package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import io.github.kitswas.VGP_Data_Exchange.GameButtons
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.kitswas.virtualgamepadmobile.data.PadInput
import org.junit.Assert.assertEquals
import org.junit.Test

// Face buttons fill a 180px square at the gamepad's origin; custom button 1 is a 60px
// circle at (200, 60), just right of B.
class TouchRouterTest {

    private val faceButtons = mapOf(
        Offset(90f, 36f) to GameButtons.Y,
        Offset(90f, 144f) to GameButtons.A,
        Offset(36f, 90f) to GameButtons.X,
        Offset(144f, 90f) to GameButtons.B,
    )

    private fun router(reading: GamepadReading) = TouchRouter(reading, view = null).apply {
        register(
            key = "face",
            frame = { TargetFrame(IntSize(180, 180), toLocal = { it }, turn = 0f) },
            areas = {
                faceButtons.map { (center, button) ->
                    HitArea(HitShape.Circle(center, 36f), PadInput(buttons = button.value))
                }
            },
            resolve = { it },
        )
        register(
            key = "custom 1",
            frame = { TargetFrame(IntSize(60, 60), toLocal = { it - Offset(200f, 60f) }, turn = 0f) },
            areas = {
                listOf(HitArea(HitShape.Circle(Offset(30f, 30f), 30f), PadInput(buttons = GameButtons.Menu.value)))
            },
            resolve = { it },
        )
    }

    private fun down(reading: GamepadReading) =
        GameButtons.entries.filter { it != GameButtons.None && reading.ButtonsDown and it.value != 0 }.toSet()

    @Test
    fun `one thumb across B and a custom button presses both`() {
        val reading = GamepadReading()
        val flatThumb = Contact(Offset(170f, 90f), semiMajor = 48f, semiMinor = 16f)

        router(reading).onContacts(listOf(flatThumb))

        assertEquals(setOf(GameButtons.B, GameButtons.Menu), down(reading))
    }

    @Test
    fun `sliding off the face buttons onto the custom button swaps what is held`() {
        val reading = GamepadReading()
        val router = router(reading)
        router.onContacts(listOf(Contact(Offset(144f, 90f), 10f, 10f)))
        assertEquals(setOf(GameButtons.B), down(reading))

        router.onContacts(listOf(Contact(Offset(230f, 90f), 10f, 10f)))

        assertEquals(setOf(GameButtons.Menu), down(reading))
    }
}

class PointerClaimsTest {

    @Test
    fun `a finger a stick has claimed stays out of the router until it lifts`() {
        val claims = PointerClaims()
        assertEquals(false, claims.isFree(id = 1, pressed = true, consumed = true)) // stick takes the down

        // Holding still: the stick doesn't mark this event, but the finger is still its.
        assertEquals(false, claims.isFree(id = 1, pressed = true, consumed = false))

        claims.isFree(id = 1, pressed = false, consumed = false) // lifted
        assertEquals(true, claims.isFree(id = 1, pressed = true, consumed = false)) // new touch, same id
    }
}
