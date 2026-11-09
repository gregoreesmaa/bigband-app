import SwiftUI

/// iOS + iPadOS entry point. iPad support comes free: same views,
/// multitasking-capable, no extra code.
struct ContentView: View {
    var body: some View {
        TabView {
            TunerView()
                .tabItem { Label("Tuner", systemImage: "tuningfork") }
            MetronomeView()
                .tabItem { Label("Metronome", systemImage: "metronome") }
        }
    }
}
