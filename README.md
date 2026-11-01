# TextSentry

**Less noise. More signal.**

TextSentry is an Android message assistant powered by **TypeSafe AI / Jev System One (`jev-latest`)**. The AI provider, endpoint and model are fixed by the app and **cannot be changed**. Each user enters their own TypeSafe API key, checks message categories, adds optional natural-language restrictions, and selects a minimum match probability (50–100%, default **95%**). Jev returns a yes probability, not free-form text. The fixed opt-out reply is **STOP**.

Two distributions are maintained:

- **Play flavor:** A manual assistant for pasted/shared text. It requires explicit per-analysis consent, never monitors incoming SMS, never sends SMS automatically and holds no restricted SMS permissions. The Play flavor is **not yet approved or published**.
- **Sideload flavor:** An experimental tool to monitor eligible incoming SMS and optionally send STOP when local safeguards and the user-selected threshold pass. Monitoring and automatic replies remain **off by default**, with independent permission/consent checks and a one-tap pause.

## App experience

The redesigned home screen shows TypeSafe connection readiness, monitoring status (sideload), selected message categories, active confidence setting and test/decision history.

### Message categories

The Settings page provides three independent checkboxes:

- **Go vote reminders** — unsolicited nonpartisan get-out-the-vote reminders.
- **Political campaign messages** — unsolicited candidate, political party, fundraising and campaign appeals.
- **Marketing ads** — unsolicited commercial promotions, coupons and advertisements.

All three categories are checked by default for new installations and for users who have not saved a selection. Explicitly saved choices, including unchecked categories, remain unchanged. Selected categories are combined using an **OR** into ONE Jev yes/no classification request per eligible, nonduplicate SMS, with the optional extra restrictions appended to that same instruction. There are no separate requests for each checked category. With none selected, analysis is skipped entirely.

To qualify for a STOP reply, each selected category still requires a clear SMS **STOP** unsubscribe instruction. Official voter registration/polling place notices, personal texts, OTPs, and ambiguous texts remain excluded. This is opt-out filtering, not political messaging or voter outreach; no voter information is deleted and TextSentry never sends election content to others. A false positive may still occur, so use preview mode before allowing automatic replies. A separate **Settings** screen provides encrypted API-key entry/removal, category checkboxes, extra prompt editing, confidence slider and privacy/data controls. Changing the policy invalidates queued SMS work, and upgrading from a legacy chat provider clears the old credential before any TypeSafe call.

**Message deletion:** Settings provides an immediate, confirmed **Clear local decision history** action and a full local data reset. These erase TextSentry records, **not original SMS in Android Messages**. Android only permits apps with appropriate default SMS handler privileges to delete inbox messages. TextSentry is not currently a full default SMS/MMS app, so automatic original-message deletion is intentionally unavailable.

**Privacy:** An eligible incoming SMS in the sideload flavor or text you explicitly submit in the Play flavor is transmitted over HTTPS to TypeSafe for analysis under TypeSafe's privacy/retention terms. The app does not run a TextSentry backend, advertising SDK or analytics. Encrypted API keys live on the phone. WorkManager can temporarily store queued SMS data on-device; decision history may contain suggested reply text.

**Safety:** Local rules reject OTPs, short codes, common automated stop/quit responses and dangerous or oversized messages before an AI request. Duplicate messages are suppressed for 24 hours and auto-replies are limited to one per sender per day. False positive STOP replies can confirm a number to malicious senders, so keep preview mode on while evaluating.

## Development

Requires Java 17, Android SDK API 36, Gradle wrapper 9.8.2 and Android Gradle Plugin 9.5.0.

```bash
./gradlew :app:testSideloadDebugUnitTest :app:testPlayDebugUnitTest :app:lintSideloadDebug :app:lintPlayDebug :app:assembleSideloadDebug :app:assemblePlayDebug :app:bundlePlayRelease
./gradlew :app:connectedSideloadDebugAndroidTest :app:connectedPlayDebugAndroidTest
```

GitHub Actions uploads the **TextSentry-sideload-debug-apk** and **TextSentry-play-preview-apk** artifacts after successful quality checks. Automated CI runs offline unit tests, Android lint, instrumented tests on both flavors, a Play restricted-permission audit and a trusted live TypeSafe test on main.

CI's `TYPESAFE_AI_API_KEY` is for synthetic integration tests only. It is never embedded in the app. Users must enter their own key.

For Google Play release preparation, see [PLAY_RELEASE.md](PLAY_RELEASE.md), [PRIVACY.md](PRIVACY.md), and [AGENTS.md](AGENTS.md).

## Signing and release

Configure GitHub Actions repository secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` to enable optional signed release publishing on main. Until configured, the signed-release job skips publication while debug APK artifacts still work. Protect main with required quality and emulator checks; merely defining workflows is not branch protection.
