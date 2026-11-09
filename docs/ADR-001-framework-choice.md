# ADR-001: Language and framework for iOS / iPadOS / watchOS / Android tuner + metronome

Date: 2026-11-09
Status: Accepted

## Context

One codebase should serve iOS phones, iPads, Apple Watch, and Android.
The first two features are a chromatic tuner (live mic input, pitch detection,
sub-20 ms display latency target) and a metronome (sample-accurate click
scheduling, background operation, haptic pulse on Watch). More musician
features may follow.

## Decision

**Kotlin Multiplatform for shared logic, native SwiftUI for all Apple UI
(including watchOS), native Jetpack Compose for Android UI.**

Languages: Kotlin (shared + Android) and Swift (Apple UI + Apple audio glue).
Audio I/O is platform-native: `AVAudioEngine` on Apple, `AAudio`/`Oboe` on Android.

## Comparison

| Framework | watchOS | Audio fit | Sharing |
|---|---|---|---|
| KMP + native UI (chosen) | Native SwiftUI | Thin native audio | Logic shared, UI native |
| Pure native (Swift + Kotlin) | Native SwiftUI | Thin native audio | Theory/DSP duplicated |
| Flutter | No official support | Plugin + channel lag | UI shared, Watch native anyway |
| React Native | Community only | Bridge jitter | UI shared, Watch native anyway |

MAUI was also rejected: no production watchOS story.

## Consequences

- Watch app is Swift-only UI calling the shared Kotlin framework for tempo/pitch logic.
- DSP stays testable in `commonTest`; platform layers stay thin.
- Cost: two UI codebases (SwiftUI + Compose) and Xcode still required for Apple builds.
