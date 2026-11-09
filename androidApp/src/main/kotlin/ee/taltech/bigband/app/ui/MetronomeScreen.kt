package ee.taltech.bigband.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.taltech.bigband.app.MetronomeEngine
import ee.taltech.bigband.app.audio.ClickPlayer

private val BEAT_CHOICES = listOf(2, 3, 4, 6)

/**
 * Basic working metronome: tempo + meter controls, click audio via
 * [ClickPlayer], beat dots flashing on each beat callback.
 */
@Composable
fun MetronomeScreen() {
    var bpm by remember { mutableDoubleStateOf(120.0) }
    var beatsPerBar by remember { mutableIntStateOf(4) }
    var playing by remember { mutableStateOf(false) }
    var currentBeat by remember { mutableIntStateOf(0) }

    val player = remember {
        ClickPlayer { beat, _ -> currentBeat = beat }
    }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Text("${bpm.toInt()} BPM", fontSize = 56.sp)
        Slider(
            value = bpm.toFloat(),
            onValueChange = { bpm = it.toDouble() },
            valueRange = 30f..300f,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { bpm = (bpm - 1).coerceAtLeast(30.0) }) { Text("−1") }
            Button(onClick = { bpm = (bpm + 1).coerceAtMost(300.0) }) { Text("+1") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BEAT_CHOICES.forEach { beats ->
                Button(
                    onClick = {
                        beatsPerBar = beats
                        if (playing) player.start(bpm, beatsPerBar)
                    },
                ) {
                    Text(
                        "$beats/4",
                        color = if (beats == beatsPerBar) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(beatsPerBar) { i ->
                val lit = playing && currentBeat == i
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (lit) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                )
            }
        }
        Button(
            onClick = {
                if (playing) {
                    player.stop()
                    playing = false
                } else if (MetronomeEngine.isSupportedTempo(bpm)) {
                    player.start(bpm, beatsPerBar)
                    playing = true
                }
            },
        ) {
            Text(if (playing) "Stop" else "Start")
        }
    }
}
