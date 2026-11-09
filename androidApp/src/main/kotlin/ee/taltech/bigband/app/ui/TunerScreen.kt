package ee.taltech.bigband.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ee.taltech.bigband.app.TunerEngine
import ee.taltech.bigband.app.TuningResult
import ee.taltech.bigband.app.audio.PitchDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private const val SAMPLE_RATE = 44_100
private const val FRAME_SIZE = 4096

/**
 * Basic working tuner: mic input via [AudioRecord], pitch estimation via
 * [PitchDetector], note mapping via shared [TunerEngine].
 */
@Composable
fun TunerScreen() {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> permissionGranted = granted }

    var reading by remember { mutableStateOf<TuningResult?>(null) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) return@LaunchedEffect
        running = true
        withContext(Dispatchers.IO) {
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(FRAME_SIZE * 2)
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf,
            )
            val shorts = ShortArray(FRAME_SIZE)
            val floats = FloatArray(FRAME_SIZE)
            try {
                record.startRecording()
                while (isActive) {
                    val n = record.read(shorts, 0, FRAME_SIZE)
                    if (n > 0) {
                        for (i in 0 until n) floats[i] = shorts[i] / 32768f
                        val freq = PitchDetector.detectFrequency(
                            floats.copyOf(n),
                            SAMPLE_RATE,
                        )
                        reading = freq?.let { TunerEngine.tune(it) }
                    }
                }
            } finally {
                runCatching { record.stop() }
                record.release()
                running = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { running = false }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        if (!permissionGranted) {
            Text("The tuner needs microphone access.")
            Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text("Allow microphone")
            }
            return@Column
        }
        val current = reading
        if (current == null) {
            Text(if (running) "Listening… play a note" else "Starting mic…")
        } else {
            Text(current.noteName, fontSize = 72.sp)
            Text("%.1f Hz · %+.0f cents".format(current.frequencyHz, current.centsSigned))
            val inTune = TunerEngine.isInTune(current.centsSigned)
            // Cents needle: map [-50, +50] to [0, 1].
            LinearProgressIndicator(
                progress = { ((current.centsSigned + 50) / 100).toFloat().coerceIn(0f, 1f) },
            )
            Text(
                if (inTune) "In tune ✓" else if (current.centsSigned < 0) "♭ Flat — tune up" else "♯ Sharp — tune down",
                color = if (inTune) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}
