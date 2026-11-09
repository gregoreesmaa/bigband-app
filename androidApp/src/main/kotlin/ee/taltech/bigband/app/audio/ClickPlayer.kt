package ee.taltech.bigband.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import ee.taltech.bigband.app.MetronomeEngine
import ee.taltech.bigband.app.audio.ClickSynthesizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Basic metronome click player.
 *
 * Two pre-rendered PCM click buffers (accent + plain) are played through
 * `AudioTrack` in static mode for low latency. Beat times come from
 * [MetronomeEngine]; the scheduler sleeps until the next beat timestamp, so
 * drift does not accumulate. [onBeat] fires on the calling thread's scope for
 * UI flashing — keep it lightweight.
 *
 * Not sample-accurate (that would need AAudio + a lookahead scheduler); good
 * enough for sectionals and practice, and the engine math stays shared.
 */
class ClickPlayer(
    private val sampleRate: Int = 44_100,
    val onBeat: (beatIndex: Int, beatsPerBar: Int) -> Unit = { _, _ -> },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    @Volatile
    var playing: Boolean = false
        private set

    private val accentBuffer = renderClick(freqHz = 2000.0)
    private val plainBuffer = renderClick(freqHz = 1500.0)

    fun start(bpm: Double, beatsPerBar: Int) {
        require(MetronomeEngine.isSupportedTempo(bpm)) { "bpm $bpm out of range" }
        require(beatsPerBar >= 1) { "beatsPerBar must be >= 1" }
        stop()
        playing = true
        val intervalMs = (MetronomeEngine.intervalSeconds(bpm) * 1000).toLong()
        job = scope.launch {
            var beat = 0
            var nextTime = System.currentTimeMillis()
            while (isActive && playing) {
                val now = System.currentTimeMillis()
                if (now >= nextTime) {
                    playClick(accent = beat % beatsPerBar == 0)
                    onBeat(beat % beatsPerBar, beatsPerBar)
                    beat++
                    nextTime += intervalMs
                } else {
                    delay(nextTime - now)
                }
            }
        }
    }

    fun stop() {
        playing = false
        job?.cancel()
        job = null
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun playClick(accent: Boolean) {
        val data = if (accent) accentBuffer else plainBuffer
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(data.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(data, 0, data.size)
        track.setNotificationMarkerPosition(data.size)
        track.setPlaybackPositionUpdateListener(
            object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack) {
                    track.release()
                }

                override fun onPeriodicNotification(track: AudioTrack) = Unit
            },
        )
        track.play()
    }

    /** Click PCM comes from shared [ClickSynthesizer]; convert floats to 16-bit. */
    private fun renderClick(freqHz: Double): ShortArray {
        val floats = ClickSynthesizer.render(freqHz, sampleRate)
        return ShortArray(floats.size) { i ->
            (floats[i] * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }
}
