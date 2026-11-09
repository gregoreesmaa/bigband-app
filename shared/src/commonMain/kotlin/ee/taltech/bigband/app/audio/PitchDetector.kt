package ee.taltech.bigband.app.audio

import kotlin.math.sqrt

/**
 * Monophonic pitch estimation via normalized autocorrelation.
 *
 * Pure shared Kotlin: every platform feeds it mic frames captured with its
 * native stack (`AudioRecord` on Android, `AVAudioEngine` tap on Apple) and
 * gets back an estimated frequency in Hz, or null when the frame is silent
 * or unpitched. Covers roughly C2–C7, the practical tuning range of a
 * big band. Note mapping of the result stays in [TunerEngine].
 */
object PitchDetector {
    private const val MIN_HZ = 50.0
    private const val MAX_HZ = 1200.0
    private const val MIN_RMS = 0.02
    private const val MIN_PERIODICITY = 0.5

    fun detectFrequency(samples: FloatArray, sampleRate: Int): Double? {
        if (samples.isEmpty() || sampleRate <= 0) return null

        var sumSquares = 0.0
        var mean = 0.0
        for (s in samples) {
            sumSquares += s * s
            mean += s
        }
        mean /= samples.size
        val rms = sqrt(sumSquares / samples.size)
        if (rms < MIN_RMS) return null

        val minLag = (sampleRate / MAX_HZ).toInt().coerceAtLeast(1)
        val maxLag = (sampleRate / MIN_HZ).toInt().coerceAtMost(samples.size / 2)
        if (minLag >= maxLag) return null

        // One correlation pass, then peak-pick on the curve. A plain global
        // maximum prefers subharmonics (2x/4x the true period correlate just
        // as well for pure tones), so instead take the *earliest* local
        // maximum near the global peak — the fundamental period.
        val corr = DoubleArray(maxLag - minLag + 1) { i ->
            correlation(samples, mean, minLag + i)
        }
        val globalMax = corr.max()
        val threshold = maxOf(MIN_PERIODICITY, 0.8 * globalMax)
        var bestLag = -1
        for (i in 1 until corr.size - 1) {
            if (corr[i] >= threshold && corr[i] >= corr[i - 1] && corr[i] >= corr[i + 1]) {
                bestLag = minLag + i
                break
            }
        }
        if (bestLag < 0) return null

        // Parabolic interpolation around the peak for sub-sample accuracy.
        return sampleRate / refineAbsolute(corr, minLag, bestLag)
    }

    private fun refineAbsolute(corr: DoubleArray, minLag: Int, peakLag: Int): Double {
        val i = peakLag - minLag
        if (i <= 0 || i >= corr.size - 1) return peakLag.toDouble()
        val y0 = corr[i - 1]
        val y1 = corr[i]
        val y2 = corr[i + 1]
        val denom = y0 - 2 * y1 + y2
        if (denom >= 0) return peakLag.toDouble()
        return peakLag + 0.5 * (y0 - y2) / denom
    }

    private fun correlation(samples: FloatArray, mean: Double, lag: Int): Double {
        var num = 0.0
        var denA = 0.0
        var denB = 0.0
        var i = 0
        while (i + lag < samples.size) {
            val a = samples[i] - mean
            val b = samples[i + lag] - mean
            num += a * b
            denA += a * a
            denB += b * b
            i++
        }
        return if (denA > 0 && denB > 0) num / sqrt(denA * denB) else 0.0
    }

}
