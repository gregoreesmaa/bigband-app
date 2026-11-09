package ee.taltech.bigband.app.audio

import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PitchDetectorTest {
    private fun sine(freqHz: Double, sampleRate: Int = 44_100, seconds: Double = 0.1): FloatArray {
        val n = (sampleRate * seconds).toInt()
        return FloatArray(n) { i -> (0.5 * sin(2 * PI * freqHz * i / sampleRate)).toFloat() }
    }

    @Test
    fun detectsA440() {
        val detected = PitchDetector.detectFrequency(sine(440.0), 44_100)
        assertNotNull(detected)
        assertTrue(abs(detected - 440.0) < 5.0, "expected ~440 Hz, got $detected")
    }

    @Test
    fun detectsLowE() {
        val detected = PitchDetector.detectFrequency(sine(82.4), 44_100)
        assertNotNull(detected)
        assertTrue(abs(detected - 82.4) < 3.0, "expected ~82.4 Hz, got $detected")
    }

    @Test
    fun silenceGivesNull() {
        assertTrue(PitchDetector.detectFrequency(FloatArray(4096), 44_100) == null)
    }
}
