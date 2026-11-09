import AVFoundation
import SwiftUI
import shared

/// Basic working tuner: mic input via AVAudioEngine, pitch estimation with
/// the shared `PitchDetector`, note mapping with the shared `TunerEngine`.
/// The view is presentation-only: no DSP lives here.
struct TunerView: View {
    @State private var result: TuningResult?
    @State private var permissionDenied = false
    @State private var engine: AVAudioEngine?
    @State private var lastUpdate = Date.distantPast

    var body: some View {
        VStack(spacing: 16) {
            if permissionDenied {
                Text("The tuner needs microphone access. Allow it in Settings.")
            } else if let current = result {
                Text(current.noteName)
                    .font(.system(size: 72, weight: .bold))
                Text(
                    String(
                        format: "%.1f Hz · %+.0f cents",
                        current.frequencyHz,
                        current.centsSigned
                    )
                )
                ProgressView(value: (current.centsSigned + 50) / 100)
                    .frame(maxWidth: 240)
                Text(label(for: current))
                    .foregroundStyle(
                        TunerEngine.shared.isInTune(
                            centsSigned: current.centsSigned,
                            toleranceCents: 5
                        ) ? .green : .red
                    )
            } else {
                Text("Listening… play a note")
            }
        }
        .padding(24)
        .onAppear(perform: start)
        .onDisappear(perform: stop)
    }

    private func label(for current: TuningResult) -> String {
        if TunerEngine.shared.isInTune(centsSigned: current.centsSigned, toleranceCents: 5) {
            return "In tune ✓"
        }
        return current.centsSigned < 0 ? "♭ Flat — tune up" : "♯ Sharp — tune down"
    }

    private func start() {
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
            guard granted else {
                permissionDenied = true
                return
            }
            do {
                let session = AVAudioSession.sharedInstance()
                try session.setCategory(.playAndRecord, mode: .measurement, options: [.duckOthers])
                try session.setActive(true)
                let engine = AVAudioEngine()
                let input = engine.inputNode
                let format = input.outputFormat(forBus: 0)
                input.installTap(onBus: 0, bufferSize: 4096, format: format) { buffer, _ in
                    guard let channel = buffer.floatChannelData?[0] else { return }
                    let count = Int(buffer.frameLength)
                    let frames = Array(UnsafeBufferPointer(start: channel, count: count))
                    let kotlinSamples = KotlinFloatArray(size: Int32(count)) { i in
                        frames[Int(i)]
                    }
                    if let hz = PitchDetector.shared.detectFrequency(
                        samples: kotlinSamples,
                        sampleRate: Int32(format.sampleRate)
                    )?.doubleValue {
                        let tuned = TunerEngine.shared.tune(
                            frequencyHz: hz,
                            referenceA4Hz: 440
                        )
                        let now = Date()
                        // Throttle UI updates to ~10 Hz.
                        if now.timeIntervalSince(lastUpdate) > 0.1 {
                            lastUpdate = now
                            result = tuned
                        }
                    }
                }
                engine.prepare()
                try engine.start()
                self.engine = engine
            } catch {
                stop()
            }
        }
    }

    private func stop() {
        engine?.inputNode.removeTap(onBus: 0)
        engine?.stop()
        engine = nil
    }
}
