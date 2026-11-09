package ee.taltech.bigband.app

/**
 * Pure metronome timing math shared by every platform.
 *
 * The platform layers own click audio scheduling (AVAudioEngine on Apple,
 * AAudio/Oboe on Android, haptics via WatchKit on watchOS) and call
 * [intervalSeconds] / [beatTimestamps] to know *when* clicks fall. Keeping the
 * math here keeps tempo behavior identical everywhere and unit-testable without
 * audio hardware.
 */
object MetronomeEngine {
    /** Seconds between beats at [bpm] (must be > 0). */
    fun intervalSeconds(bpm: Double): Double {
        require(bpm > 0) { "bpm must be > 0" }
        return 60.0 / bpm
    }

    /**
     * Timestamps (seconds, relative to start) of the first [count] beats at
     * [bpm] with an optional [startDelaySeconds] offset.
     */
    fun beatTimestamps(bpm: Double, count: Int, startDelaySeconds: Double = 0.0): List<Double> {
        require(count >= 0) { "count must be >= 0" }
        val interval = intervalSeconds(bpm)
        return List(count) { i -> startDelaySeconds + i * interval }
    }

    /** True when [bpm] is within the supported metronome range. */
    fun isSupportedTempo(bpm: Double, minBpm: Double = 30.0, maxBpm: Double = 300.0): Boolean =
        bpm in minBpm..maxBpm
}
