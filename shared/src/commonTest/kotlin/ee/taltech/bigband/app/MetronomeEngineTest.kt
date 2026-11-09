package ee.taltech.bigband.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetronomeEngineTest {
    @Test
    fun intervalAt120BpmIsHalfSecond() {
        assertEquals(0.5, MetronomeEngine.intervalSeconds(120.0), 1e-9)
    }

    @Test
    fun fourBeatsAt60BpmFallOnWholeSeconds() {
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0), MetronomeEngine.beatTimestamps(60.0, 4))
    }

    @Test
    fun supportedTempoRange() {
        assertTrue(MetronomeEngine.isSupportedTempo(120.0))
        assertTrue(!MetronomeEngine.isSupportedTempo(10.0))
    }
}
