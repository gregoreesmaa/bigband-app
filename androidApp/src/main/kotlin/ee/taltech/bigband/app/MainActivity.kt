package ee.taltech.bigband.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ee.taltech.bigband.app.ui.MetronomeScreen
import ee.taltech.bigband.app.ui.TunerScreen

/** Android entry point: tuner + metronome tabs (Jetpack Compose). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var tab by remember { mutableIntStateOf(0) }
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    TabRow(selectedTabIndex = tab) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Tuner") })
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Metronome") })
                    }
                    when (tab) {
                        0 -> TunerScreen()
                        else -> MetronomeScreen()
                    }
                }
            }
        }
    }
}
