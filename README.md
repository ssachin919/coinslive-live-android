# CoinsLive Live — Android

Native **video calling** for CoinsLive:

- **Workshop** — broadcast live sessions (`/workshop`)
- **Adhyayan** — peer study rooms (`/adhyayan`)

No mentor sessions, feed, or other product surfaces. Backend: [`coinslive-mvp`](https://github.com/ssachin919/coinslive-mvp). iOS sibling: [`coinslive-live-ios`](https://github.com/ssachin919/coinslive-live-ios).

## Brand

Navy `#0d1b2a`, brand blue `#1e88e5`, tan `#f4a261`, paper `#f3f0ea` — same tokens as the website / iOS app.

## Stack

- Jetpack Compose + Material3
- Clerk (`clerk-android-ui`) for sign-in
- LiveKit Android + Compose components for A/V
- Package: `in.coinslive.live`

## Deploy (Google Play)

**Default track: Closed testing** (`alpha`), status `completed`.

- Push to `main` (app/code changes) → builds AAB and uploads to **Closed testing**
- Tag `v*` → same (Closed testing)
- Manual: Actions → **Deploy to Google Play** (defaults to `alpha` / `completed`)
- Add `[skip play]` to a commit message to skip auto-upload

```bash
# optional: bump versionName in app/build.gradle.kts for a clearer store label, then:
git push origin main
# or: git tag v0.1.8 && git push origin v0.1.8
```

CI assigns a unique `versionCode` on each upload so Play never rejects a duplicate.

Promote Closed/Internal → production with Actions → **Promote Google Play track**.

### Required repository secrets

| Secret | Purpose |
|--------|---------|
| `KEYSTORE_BASE64` | Base64-encoded upload keystore (`.jks`) |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |
| `PLAY_SERVICE_ACCOUNT_JSON` | Play Console API service account JSON |

Create the Play Console app with package **`in.coinslive.live`** before the first upload. Grant the service account access to that app.

## Local build

```bash
./gradlew :app:assembleDebug
```

Optional: copy `keystore.properties.example` → `keystore.properties` for local release signing.

## Deep links

- `coinslive://workshop/{slug}/live`
- `coinslive://adhyayan/{slug}/live`
