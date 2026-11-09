import SwiftUI
import WatchKit
import shared

@main
struct WatchApp: App {
    var body: some Scene {
        WindowGroup {
            TabView {
                WatchMetronomeView()
                WatchTunerView()
            }
            .tabViewStyle(.page)
        }
    }
}

/// Haptic metronome: beat times from shared `MetronomeEngine`, pulse via
/// the Taptic Engine. Accent = .notification, plain beat = .click.
struct WatchMetronomeView: View {
    @State private var bpm = 120.0
    @State private var playing = false
    @State private var currentBeat = 0
    @State private var task: Task<Void, Never>?

    var body: some View {
        VStack(spacing: 8) {
            Text("\(Int(bpm))")
                .font(.system(size: 44, weight: .bold))
            HStack {
                Button("-") { bpm = max(30, bpm - 5) }
                Button("+") { bpm = min(300, bpm + 5) }
            }
            .buttonStyle(.bordered)
            HStack(spacing: 6) {
                ForEach(0..<4, id: \.self) { i in
                    Circle()
                        .fill(playing && currentBeat == i ? Color.green : Color.gray.opacity(0.4))
                        .frame(width: 12, height: 12)
                }
            }
            Button(playing ? "Stop" : "Start") {
                playing ? stop() : start()
            }
            .tint(playing ? .red : .green)
        }
        .onDisappear(perform: stop)
    }

    private func start() {
        guard MetronomeEngine.shared.isSupportedTempo(bpm: bpm, minBpm: 30, maxBpm: 300) else {
            return
        }
        playing = true
        let intervalNs = UInt64(MetronomeEngine.shared.intervalSeconds(bpm: bpm) * 1_000_000_000)
        task = Task {
            var beat = 0
            while !Task.isCancelled {
                WKInterfaceDevice.current().play(beat % 4 == 0 ? .notification : .click)
                await MainActor.run { currentBeat = beat % 4 }
                beat += 1
                try? await Task.sleep(nanoseconds: intervalNs)
            }
        }
    }

    private func stop() {
        task?.cancel()
        task = nil
        playing = false
    }
}

/// Tuner lite: same shared `TunerEngine` mapping as the phone. Mic capture on
/// watchOS is restricted — if the engine cannot start, the view says so
/// instead of pretending to listen.
struct WatchTunerView: View {
    @State private var noteName = "–"
    @State private var message = "Tap Start and play a note"
    @State private var listening = false

    var body: some View {
        VStack(spacing: 8) {
            Text(noteName)
                .font(.system(size: 52, weight: .bold))
            Text(message)
                .font(.footnote)
                .multilineTextAlignment(.center)
            Button(listening ? "Stop" : "Start") {
                listening ? stopListening() : startListening()
            }
            .tint(listening ? .red : .green)
        }
        .onDisappear(perform: stopListening)
    }

    private func startListening() {
        // Watch mic capture needs extended-runtime-session wiring that is
        // not done yet. Fail openly rather than showing a dead needle.
        message = "Watch mic capture is not wired up yet — use the phone tuner."
        listening = false
    }

    private func stopListening() {
        listening = false
        noteName = "–"
    }
}
