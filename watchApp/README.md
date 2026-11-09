# watchOS app (SwiftUI)

Sources live in `WatchApp/`. Like `iosApp/`, the `.xcodeproj` is generated
on a Mac:

1. In the same Xcode workspace as the iOS app: File → New → Target →
   watchOS → App, name `WatchApp`, bundle ID `ee.taltech.bigband.app.watchapp`.
2. Add the `.swift` files from `WatchApp/`.
3. Link the same `shared.framework` as the iOS target (`import shared`).

What works today: haptic metronome (Taptic Engine pulse, beat times from
shared `MetronomeEngine`). The tuner tab is an honest stub — watchOS mic
capture needs extended-runtime-session work, so it says so instead of
showing a dead needle.
