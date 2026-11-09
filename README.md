# BigBand App

Musician's toolkit for a big-band context: chromatic tuner, metronome, and room to grow
(setlists, section notes, transposition helpers).

## Stack decision (short version)

**Kotlin Multiplatform (shared logic) + SwiftUI on Apple + Jetpack Compose on Android.**

- Shared `shared/` Kotlin module holds portable logic: pitch-to-note math, tempo engine,
  music-theory helpers. Tested once with `kotlin.test`, used on every platform.
- Apple UI (iOS / iPadOS / watchOS) is **native SwiftUI**. watchOS has no production-grade
  cross-platform UI framework, so the Watch app is Swift-only and calls into the shared
  Kotlin framework for logic.
- Android UI is **native Jetpack Compose**.
- Audio I/O stays platform-native and thin: `AVAudioEngine` on Apple, `AAudio`/`Oboe` on
  Android. Low-latency tuner input and metronome click scheduling bypass any shared UI layer.

Why not Flutter / React Native / MAUI / pure-native-duplicate-everything? See
[ADR-001](docs/ADR-001-framework-choice.md).

## Repo layout

```text
bigband-app/
  shared/                  All heavy lifting (commonMain): TunerEngine,
                           MetronomeEngine, audio/PitchDetector,
                           audio/ClickSynthesizer + commonTest suite
  androidApp/              Android UI only: TunerScreen, MetronomeScreen,
                           thin AudioRecord/AudioTrack glue
  iosApp/BigBand/          iOS/iPadOS UI only: TunerView, MetronomeView,
                           thin AVAudioEngine glue (see iosApp/README.md)
  watchApp/                watchOS UI only: haptic metronome (see watchApp/README.md)
  docs/                    Architecture decision records
  .github/workflows/ci.yml CI: shared tests + Android build + Swift syntax check
```

Rule: DSP and music math go in `shared/` (tested once, runs everywhere).
Native code owns mic/speaker I/O, permissions, haptics, and pixels — nothing else.

## Prerequisites

- JDK 17 (Gradle + Android builds)
- Android Studio (Ladybug or newer) with Android SDK 34
- Xcode 16+ on a Mac (iOS / watchOS builds only)
- Kotlin 2.1.x via the Gradle plugin (no separate install needed)

Windows/Linux can build and test `shared/` plus `androidApp/`.
Apple targets require macOS + Xcode.

## Run

```bash
# Shared logic tests (any OS with JDK 17)
./gradlew :shared:allTests

# Android (device, emulator, or Android Studio Run)
./gradlew :androidApp:assembleDebug

# iOS (on a Mac)
open iosApp/BigBandApp.xcodeproj
# select BigBandApp scheme > Run (shared.framework is built via the
# "Embed Shared Framework" build phase calling ../gradlew)

# watchOS (on a Mac)
# open the same Xcode project/workspace, select the WatchApp scheme > Run
```

Audio notes: the tuner and metronome engines in `shared/` are pure logic
(no mic/speaker access). Each app wires them to its platform audio stack —
see `shared/src/commonMain/kotlin/ee/taltech/bigband/app/` KDoc on each class.

## Status

Scaffold (week 0). Engines are stubbed with correct APIs and unit tests;
platform audio wiring is the first real milestone.
