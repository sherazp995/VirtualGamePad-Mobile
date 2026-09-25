package io.github.kitswas.virtualgamepadmobile.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

// 1000 x 400 px screen; 100 px of finger travel equals 1 offset unit.
class LayoutEditsTest {

    private val screen = Rect(0f, 0f, 1000f, 400f)

    private fun faceButtons() = Placement(anchor = ButtonAnchor.TOP_LEFT)

    @Test
    fun `moving follows the finger exactly`() {
        val moved = faceButtons().movedBy(Offset(30f, -20f), Rect(200f, 100f, 300f, 200f), screen, 100f)

        assertEquals(0.3f, moved.offsetX, 0.0001f)
        assertEquals(-0.2f, moved.offsetY, 0.0001f)
    }

    @Test
    fun `moving stops at the screen edge`() {
        val moved = faceButtons().movedBy(Offset(-500f, 500f), Rect(200f, 100f, 300f, 200f), screen, 100f)

        assertEquals(-2f, moved.offsetX, 0.0001f)
        assertEquals(2f, moved.offsetY, 0.0001f)
    }

    @Test
    fun `swiping up by the control's height doubles it`() {
        val resized = faceButtons().resizedBy(-100f, Rect(0f, 0f, 100f, 100f), screen, isRtl = false)

        assertEquals(2f, resized.scale, 0.0001f)
    }

    @Test
    fun `growing stops before the control leaves the screen`() {
        // Anchored top left at (0, 250), so it can grow 150 px downwards at most.
        val resized = faceButtons().resizedBy(-100f, Rect(0f, 250f, 100f, 350f), screen, isRtl = false)

        assertEquals(1.5f, resized.scale, 0.0001f)
    }

    @Test
    fun `rotating follows the finger and wraps past a full turn`() {
        val rotated = Placement(rotation = 350f).rotatedBy(dragX = 30f, pxPerDegree = 1f)

        assertEquals(20f, rotated.rotation, 0.0001f)
    }

    @Test
    fun `rotating snaps to the nearest 45 degrees when close`() {
        val rotated = Placement().rotatedBy(dragX = 87f, pxPerDegree = 1f)

        assertEquals(90f, rotated.rotation, 0.0001f)
    }
}
