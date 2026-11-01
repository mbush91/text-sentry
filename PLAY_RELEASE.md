# Google Play release preparation

## Current release strategy

**Two product flavors** are intentionally offered:

- `play`: text shared/pasted manually, user-consented TypeSafe AI evaluation, editable suggestion and user-initiated SMS composition via `ACTION_SENDTO`. **No SMS or call-log permissions, receivers, monitoring or automatic SMS sends.**
- `sideload`: experimental SMS monitor/auto-send features with Android `RECEIVE_SMS` and `SEND_SMS`, runtime permission buttons, an encrypted history of processed texts and Android SMS send callbacks; **do not upload this artifact to Play Console.** The archive does not need READ_SMS. Inbox deletion is unavailable without building a complete default SMS/MMS app.

Google Play's SMS policy requires active default SMS handler status or an individually approved exception for the restricted permissions. A hypothetical exception for *device automation* requires automation across multiple areas of Android; this app currently automates SMS only and should not claim it qualifies. A spam-blocking exception is also subject to Google review, not guaranteed. Do not change the manifest to mimic default-handler eligibility without implementing and testing a complete usable SMS/MMS client.

## Build and verify

Requirements: JDK 17, Android SDK 36, Gradle wrapper 9.8.2.

```bash
./gradlew :app:testSideloadDebugUnitTest :app:testPlayDebugUnitTest :app:lintSideloadDebug :app:lintPlayDebug :app:assemblePlayDebug :app:assembleSideloadDebug
./gradlew :app:bundlePlayRelease
./gradlew :app:connectedSideloadDebugAndroidTest :app:connectedPlayDebugAndroidTest
```

CI audits the built Play APK manifest using Android build tools and rejects `READ_SMS`, `SEND_SMS`, `RECEIVE_SMS` and related restricted SMS/Call Log permissions. Android instrumentation independently verifies installed permissions and local data deletion.

The **unsigned** Play bundle for CI compilation is `app/build/outputs/bundle/playRelease/app-play-release.aab`. Do not upload it to Play. Once the four `ANDROID_*` signing secrets are configured, a trusted main-branch workflow signs the Play bundle and uploads a `TextSentry-play-signed-aab` artifact for manual submission to Play Console. It also publishes a separately signed sideload APK to a GitHub prerelease. The `TYPESAFE_AI_API_KEY` CI secret is only for tests, not app credentials.

## Play Console owner checklist

1. Register or verify your Google Play developer identity and register the package `org.textsentry.app`, which becomes the permanent Play package name after publication. Confirm this package name before the first release.
2. Enable Play App Signing. Create/store a durable upload key; add the four Android signing secrets; use only the **play release AAB**.
3. Add a stable, public, non-editable privacy policy URL. `PRIVACY.md` is in the public repository, but consider enabling GitHub Pages with a polished policy URL. Ensure Play listing identity/contact information matches the privacy contact mechanism.
4. Complete the Data safety questionnaire honestly: user-submitted messages can leave the device for a provider they select; provider handling/retention vary. The Play build does **not** automatically access SMS or contacts and contains no advertising.
5. Complete app content declarations (age rating, target audience, ads, data safety). Provide screenshots/icon/feature graphic, a short and full listing description that **does not claim automatic SMS monitoring or response** in the Play edition.
6. Test on real devices through internal testing before submitting for production review. Test share-sheet import, encrypted key setup, consent, zero-SMS-permission audit, app-to-SMS compose, data erasure, and Android 16 edge-to-edge behavior.
7. Confirm developer account testing eligibility (some new personal developer accounts must complete a closed-test period) in Play Console.

**Not yet Play-approved:** The repo can produce a Play-oriented artifact and policy documentation, but only Google reviews and approves publication. The full automatic SMS experience is **not** available in the Play flavor until an approved restricted-permission pathway is built and reviewed.
