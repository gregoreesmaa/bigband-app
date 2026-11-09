package ee.taltech.bigband.app.audio

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClickSynthesizerTest {
    @Test
    fun rendersExpectedFrameCount() {
        val pcm = ClickSynthesizer.render(2000.0, 44_100)
        assertEquals((44_100 * 0.06).toInt(), pcm.size)
    }

    @Test
    fun samplesStayInUnitRangeAndDecay() {
        val pcm = ClickSynthesizer.render(1500.0, 44_100)
        assertTrue(pcm.all { abs(it) <= 1.0f })
        val earlyPeak = pcm.take(500).maxOf { abs(it) }
        val latePeak = pcm.takeLast(500).maxOf { abs(it) }
        assertTrue(earlyPeak > latePeak, "click must decay, $earlyPeak vs $latePeak")
    }
}
