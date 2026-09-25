package io.github.kitswas.virtualgamepadmobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomButtonTest {

    private fun buttons(vararg numbers: Int) = numbers.map { CustomButton(it, setOf(ComboInput.A)) }

    @Test
    fun `a new button reuses the lowest freed number and leaves the others alone`() {
        // Added 1 to 4, then deleted 1 to 3.
        assertEquals(1, buttons(4).nextFreeNumber())
    }

    @Test
    fun `no number is free once all nine exist`() {
        assertNull(buttons(1, 2, 3, 4, 5, 6, 7, 8, 9).nextFreeNumber())
    }
}
