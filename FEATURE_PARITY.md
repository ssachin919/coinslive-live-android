# Feature parity — Android ↔ iOS

| Area | Android | iOS |
|------|---------|-----|
| Clerk sign-in / sign-out / delete account | Yes | Yes |
| List / create Workshop | Yes | Yes |
| List / create Adhyayan | Yes | Yes |
| Join by slug / deep link | Yes | Yes |
| LiveKit Workshop room (host / attendee) | Yes | Yes |
| LiveKit Adhyayan room | Yes | Yes |
| Mic / camera controls | Yes | Yes |
| Screen share (publish) | TODO | ReplayKit extension |
| Q&A / messages / broadcasts | Yes (host/attendee as wired) | Yes |
| Play / TestFlight deploy | `deploy-play.yml` on `v*` | `ios-testflight.yml` on `ios-v*` |

Version (Android): see `versionName` / `versionCode` in `app/build.gradle.kts`.
