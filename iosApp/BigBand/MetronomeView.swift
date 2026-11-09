import AVFoundation
import SwiftUI
import shared

/// Basic working metronome: tempo + meter controls, click audio scheduled on
/// AVAudioEngine, beat times from the shared `MetronomeEngine`.
struct MetronomeView: View {
    @State private var bpm = 120.0
    @State private var beatsPerBar = 4
    @State private var playing = false
    @State private var currentBeat = 0
    @State private var engine: MetronomeAudioEngine?

    private let beatChoices = [2, 3, 4, 6]

    var body: some View {
        VStack(spacing: 20) {
            Text("\(Int(bpm)) BPM")
                .font(.system(size: 56, weight: .bold))
            Slider(value: $bpm, in: 30...300, step: 1) {
                if playing { restart() }
            }
            HStack(spacing: 8) {
                Button("−1") { bpm = max(30, bpm - 1); if playing { restart() } }
                Button("+1") { bpm = min(300, bpm + 1); if playing { restart() } }
            }
            .buttonStyle(.bordered)
            HStack(spacing: 8) {
                ForEach(beatChoices, id: \.self) { beats in
                    Button("\(beats)/4") {
                        beatsPerBar = beats
                        if playing { restart() }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(beats == beatsPerBar ? .blue : .gray)
                }
            }
            HStack(spacing: 12) {
                ForEach(0..<beatsPerBar, id: \.self) { i in
                    Circle()
                        .fill(playing && currentBeat == i ? Color.blue : Color.gray.opacity(0.3))
                        .frame(width: 24, height: 24)
                }
            }
            Button(playing ? "Stop" : "Start") {
                playing ? stop() : start()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(24)
        .onDisappear(perform: stop)
    }

    private func start() {
        guard MetronomeEngine.shared.isSupportedTempo(
            bpm: bpm,
            minBpm: 30,
            maxBpm: 300
        ) else { return }
        let audio = MetronomeAudioEngine { beat in
            currentBeat = Int(beat)
        }
        audio.start(bpm: bpm, beatsPerBar: Int32(beatsPerBar))
        engine = audio
        playing = true
    }

    private func restart() {
        stop()
        start()
    }

    private func stop() {
        engine?.stop()
        engine = nil
        playing = false
    }
}

/// Thin AVAudioEngine wrapper: schedules accent/plain clicks ahead of time.
final class MetronomeAudioEngine {
    private let engine = AVAudioEngine()
    private let player = AVAudioPlayerNode()
    private var timer: Timer?
    private let onBeat: (Int) -> Void

    init(onBeat: @escaping (Int) -> Void) {
        self.onBeat = onBeat
        engine.attach(player)
        engine.connect(player, to: engine.mainMixerNode, format: nil)
    }

    func start(bpm: Double, beatsPerBar: Int32) {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, options: [.duckOthers])
            try AVAudioSession.sharedInstance().setActive(true)
            try engine.start()
        } catch {
            return
        }
        player.play()
        var beat = 0
        let interval = MetronomeEngine.shared.intervalSeconds(bpm: bpm)
        var nextTime = AVAudioTime(hostTime: mach_absolute_time())
        timer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] _ in
            guard let self else { return }
            // Schedule ~200 ms ahead.
            while nextTime.hostTime < mach_absolute_time() + 200_000_000 {
                let accent = beat % Int(beatsPerBar) == 0
                let buffer = Self.clickBuffer(accent: accent, format: self.player.outputFormat(forBus: 0))
                self.player.scheduleBuffer(buffer, at: nextTime, options: []) {
                    DispatchQueue.main.async { self.onBeat(beat % Int(beatsPerBar)) }
                }
                nextTime = AVAudioTime(
                    hostTime: nextTime.hostTime + UInt64(interval * 1_000_000_000)
                )
                beat += 1
            }
        }
    }

    func stop() {
        timer?.invalidate()
        timer = nil
        player.stop()
        engine.stop()
    }

    /// Click PCM comes from the shared `ClickSynthesizer`; this only copies
    /// the floats into the output buffer. No DSP lives here.
    private static func clickBuffer(accent: Bool, format: AVAudioFormat) -> AVAudioPCMBuffer {
        let rate = format.sampleRate
        let pcm = ClickSynthesizer.shared.render(
            freqHz: accent ? 2000 : 1500,
            sampleRate: Int32(rate),
            durationSeconds: 0.06
        )
        let frames = AVAudioFrameCount(pcm.size)
        let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: frames)!
        buffer.frameLength = frames
        let channel = buffer.floatChannelData![0]
        for i in 0..<Int(frames) {
            channel[i] = pcm.get(index: Int32(i))
        }
        return buffer
    }
}
