# iOS / iPadOS app (SwiftUI)

Sources live in `BigBand/`. There is deliberately no `.xcodeproj` in the
repo — it is generated on a Mac:

1. In Xcode: File → New → Project → iOS → App, name `BigBand`,
   bundle ID `ee.taltech.bigband.app`, interface SwiftUI.
2. Drag the `.swift` files from `BigBand/` into the project (or move the
   Xcode project next to them and add the files).
3. Add the shared Kotlin framework (standard KMP integration):
   add a Build Phase → New Run Script Phase before Compile Sources:
   `cd "$SRCROOT/../.." && ./gradlew :shared:embedAndSignAppleFrameworkForXcode`,
   then link the produced `shared.framework` and `import shared`.
4. Target → Info → add `NSMicrophoneUsageDescription`
   ("Tuner needs microphone access to detect pitch.").
5. iPad needs no extra work: same views, all orientations enabled.

`MetronomeEngine` / `TunerEngine` come from `import shared` (Kotlin
objects are visible as `MetronomeEngine.shared`, `TunerEngine.shared`).
`PitchDetector.swift` is intentionally platform-local: audio I/O stays
native, only the tempo/pitch math is shared.
