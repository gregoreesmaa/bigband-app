package ee.taltech.bigband.app.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Metronome click synthesis, shared by every platform.
 *
 * Renders a short sine burst with exponential decay as mono floats in
 * [-1, 1]. Android converts to 16-bit PCM for `AudioTrack`; Apple copies
 * the floats straight into an `AVAudioPCMBuffer`. One implementation, one
 * test, identical click timbre on phone, tablet, and watch.
 */
object ClickSynthesizer {
    fun render(
        freqHz: Double,
        sampleRate: Int,
        durationSeconds: Double = 0.06,
    ): FloatArray {
        require(freqHz > 0) { "freqHz must be > 0" }
        require(sampleRate > 0) { "sampleRate must be > 0" }
        require(durationSeconds > 0) { "durationSeconds must be > 0" }
        val frames = (sampleRate * durationSeconds).toInt().coerceAtLeast(1)
        return FloatArray(frames) { i ->
            val t = i.toDouble() / sampleRate
            (exp(-t * 60.0) * sin(2 * PI * freqHz * t)).toFloat()
        }
    }

    /** Standard two-voice metronome: piercing accent, softer plain beat. */
    fun renderAccent(sampleRate: Int): FloatArray = render(2000.0, sampleRate)

    fun renderPlain(sampleRate: Int): FloatArray = render(1500.0, sampleRate)
}
