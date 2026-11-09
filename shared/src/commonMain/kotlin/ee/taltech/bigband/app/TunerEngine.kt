package ee.taltech.bigband.app

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Result of mapping a detected frequency to the nearest chromatic note.
 *
 * @property frequencyHz Input frequency in Hz (must be > 0).
 * @property noteName Nearest note in sharp spelling, e.g. "A4".
 * @property centsSigned Deviation from the nearest note in cents, in [-50, 50).
 * @property midiNumber Nearest MIDI note number (69 = A4 = 440 Hz).
 */
data class TuningResult(
    val frequencyHz: Double,
    val noteName: String,
    val centsSigned: Double,
    val midiNumber: Int,
)

private val NOTE_NAMES_SHARP = listOf(
    "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B",
)

/**
 * Pure pitch-to-note math shared by every platform.
 *
 * Platform layers own mic capture and frequency estimation (FFT/autocorrelation
 * via AVAudioEngine on Apple, AAudio/Oboe on Android) and call [tune] with the
 * estimated frequency. Keeping this mapping here means tuner display behavior
 * is unit-tested once and identical on phone, tablet, and watch.
 */
object TunerEngine {
    fun tune(frequencyHz: Double, referenceA4Hz: Double = 440.0): TuningResult {
        require(frequencyHz > 0) { "frequencyHz must be > 0" }
        require(referenceA4Hz > 0) { "referenceA4Hz must be > 0" }
        val midi = (69 + 12 * ln(frequencyHz / referenceA4Hz) / ln(2.0)).roundToInt()
        val nearestHz = referenceA4Hz * 2.0.pow((midi - 69) / 12.0)
        val cents = 1200 * ln(frequencyHz / nearestHz) / ln(2.0)
        val octave = midi / 12 - 1
        val name = "${NOTE_NAMES_SHARP[((midi % 12) + 12) % 12]}$octave"
        return TuningResult(frequencyHz, name, cents, midi)
    }

    /** True when [centsSigned] is within [toleranceCents] of perfect tune. */
    fun isInTune(centsSigned: Double, toleranceCents: Double = 5.0): Boolean =
        abs(centsSigned) <= abs(toleranceCents)
}
