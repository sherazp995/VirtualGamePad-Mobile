package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.ui.geometry.Offset
import io.github.kitswas.virtualgamepadmobile.ui.composables.ClusterSlot.BOTTOM
import io.github.kitswas.virtualgamepadmobile.ui.composables.ClusterSlot.LEFT
import io.github.kitswas.virtualgamepadmobile.ui.composables.ClusterSlot.RIGHT
import io.github.kitswas.virtualgamepadmobile.ui.composables.ClusterSlot.TOP
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI

// A 180px cluster has 72px buttons centred 36px in from each side.
class ButtonClusterTest {

    private fun fingertip(x: Float, y: Float) = slotsUnderContact(Contact(Offset(x, y), 20f, 20f), 180f)

    @Test
    fun `a fingertip on one button presses only that button`() {
        assertEquals(setOf(BOTTOM), fingertip(90f, 144f))
    }

    @Test
    fun `a fingertip in the gap between two buttons presses both`() {
        assertEquals(setOf(TOP, RIGHT), fingertip(126f, 54f))
    }

    @Test
    fun `a thumb laid flat across the bottom row presses the three buttons under it`() {
        // Long side horizontal, centred just above A: reaches X, A and B but not Y.
        val flatThumb = Contact(Offset(90f, 118f), semiMajor = 48f, semiMinor = 16f, angle = 0f)

        assertEquals(setOf(LEFT, BOTTOM, RIGHT), slotsUnderContact(flatThumb, 180f))
    }

    @Test
    fun `the same thumb turned upright presses only the column under it`() {
        val uprightThumb = Contact(Offset(90f, 118f), semiMajor = 48f, semiMinor = 16f, angle = (PI / 2).toFloat())

        // Spans y 70 to 166, so it touches A and the bottom edge of Y (y 72), not X or B.
        assertEquals(setOf(TOP, BOTTOM), slotsUnderContact(uprightThumb, 180f))
    }

    @Test
    fun `opposite d-pad directions cancel and leave the rest`() {
        assertEquals(setOf(TOP), withoutOpposites(setOf(TOP, LEFT, RIGHT)))
    }

    @Test
    fun `a d-pad turned a quarter turn sends right from its top arrow`() {
        assertEquals(RIGHT, TOP.rotatedBy(90f))
    }

    @Test
    fun `a stick turned a quarter turn reports a push along its own x axis as down`() {
        val onScreen = screenDirection(1f, 0f, 90f)

        assertEquals(0f, onScreen.x, 0.0001f)
        assertEquals(1f, onScreen.y, 0.0001f)
    }
}
