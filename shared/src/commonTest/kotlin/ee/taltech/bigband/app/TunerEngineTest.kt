package ee.taltech.bigband.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TunerEngineTest {
    @Test
    fun a440MapsToA4WithZeroCents() {
        val r = TunerEngine.tune(440.0)
        assertEquals("A4", r.noteName)
        assertEquals(69, r.midiNumber)
        assertTrue(kotlin.math.abs(r.centsSigned) < 1e-9)
    }

    @Test
    fun sharpNoteIsPositiveCents() {
        val r = TunerEngine.tune(445.0)
        assertEquals("A4", r.noteName)
        assertTrue(r.centsSigned > 0)
        assertTrue(TunerEngine.isInTune(3.0))
    }
}
