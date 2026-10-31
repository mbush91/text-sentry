# TextSentry

**Your messages. Your rules. AI-powered replies.**

TextSentry is an early-stage Android SMS triage agent. It evaluates incoming SMS against a personal prompt using an OpenAI-compatible **HTTPS chat-completions** model with your own API key (BYOK). It can preview a suggested response or automatically send it after separate opt-in.

**Initial distribution is sideload only.** Google Play tightly restricts RECEIVE_SMS / SEND_SMS for non-default SMS handlers; this app does not claim Play eligibility.

## What it currently does

- Configurable HTTPS endpoint, model, API key and natural-language reply prompt.
- API key encrypted at rest via Android Keystore. No backend, no telemetry, no cloud message history.
- SMS broadcast handled quickly; network requests delegated to Android WorkManager.
- Local safety checks for shortcodes, OTPs and oversize messages before any model call.
- Strict JSON output for chat completions; optional TypeSafe System One (Jev) typed yes/no decision with a fixed user-configured reply and conservative 0.95 approval threshold.
- TypeSafe uses `POST https://api.typesafe.ai/v1/systemone`, not `/chat/completions`. Its BYOK key is entered in the app; CI's `TYPESAFE_AI_API_KEY` is never embedded in APKs.
- **Monitoring off by default; auto-send off by default.** Preview decision button never sends SMS.
- Local outcome history without raw SMS content.
- Persistent 24-hour duplicate-message suppression and a 24-hour maximum of one auto-reply per sender to prevent reply loops.
- One-tap **Pause** invalidates any previously queued SMS work, even if monitoring is re-enabled later.
- Provider changes clear the previous BYOK key so it cannot accidentally be sent to a different AI service.
- JVM unit tests, Android lint, emulator instrumentation, and signed GitHub prereleases.

Start prompt: "Only reply STOP to unmistakably opt-in marketing messages that explicitly describe a STOP unsubscribe option. Ignore personal messages, scams, transactional notices, and uncertainty."

## Getting started

Install a prerelease APK (after signing secrets are configured), open TextSentry and configure your HTTPS chat-completions endpoint (for example https://api.openai.com/v1/chat/completions), model identifier, and API key. Test a sample message. Enable monitoring and grant RECEIVE_SMS for preview mode. Only enable auto-send after reviewing behavior and granting SEND_SMS. Your AI provider may charge for requests and your mobile carrier may charge for SMS.

**Provider selection:** Default OpenAI-compatible chat model generates a response. Toggle **Use TypeSafe System One** to classify using Jev (`jev-latest`) and send only your locally configured fixed response (default `STOP`) if the classification probability is at least 0.95. On provider changes, enter that provider's API key in the app before enabling monitoring. The CI GitHub secret does not provision keys to phones.

**Privacy:** When monitoring is enabled, incoming SMS bodies and senders eligible for evaluation are transmitted to your chosen AI provider under its privacy and retention terms. WorkManager temporarily persists SMS message bodies in an app-private on-device work queue until processing and eventual pruning. TextSentry does not maintain a cloud message archive or full local inbox. BYOK means users must provide their own provider account and key. On-device outcome history includes reply text and timestamps but no message bodies. This is an experimental tool; keep previews on while testing.

**Limitations:** SMS only (not RCS/MMS/third-party apps). Short codes are blocked by default; this may omit legitimate opt-outs. AI classification can be wrong. SMS delivery and multi-SIM behavior vary. A STOP reply to a scammer may confirm the phone number is active. WorkManager may delay evaluations. SMS permissions and Play Store compliance require separate consideration.

## Development

Requires JDK 17 and Android SDK API 36. The checked-in Gradle wrapper pins Gradle 8.13.

    ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
    ./gradlew :app:connectedDebugAndroidTest

An **installable debug APK** is attached to every successful GitHub Actions quality run as `TextSentry-debug-apk`; no signing secrets are needed for these test builds. Debug APKs use Android's ephemeral debug key (not suitable for stable public releases). The local build is at `app/build/outputs/apk/debug/app-debug.apk`. See AGENTS.md for required autonomous TDD/PR/CI procedure. CI is the source of truth and agents should iterate on failures.

## GitHub setup (one-time owner actions)

In Settings > Secrets and variables > Actions configure all four repository secrets:

| Secret | Meaning |
|---|---|
| ANDROID_KEYSTORE_BASE64 | Full release JKS encoded as one-line base64 |
| ANDROID_KEYSTORE_PASSWORD | JKS store password |
| ANDROID_KEY_ALIAS | Name of signing key |
| ANDROID_KEY_PASSWORD | Private key password |

Create the keystore locally and keep a safe backup. Example:

    keytool -genkeypair -v -keystore textsentry-release.jks -alias textsentry -keyalg RSA -keysize 3072 -validity 10000
    base64 -w0 textsentry-release.jks

Branch protection must be enabled in Settings > Rules > Rulesets (or Branches): require **Quality / tests and lint** and **Android emulator / tests**, block direct pushes to main, require up-to-date branches, and allow squash merge. Enable auto-merge in General if agents should merge after checks. Merely creating workflows does not protect main.

Every push to main runs both quality suites and a trusted Jev test; a passing quality job publishes an installable debug APK artifact. When all four signing secrets are present, CI additionally builds and verifies a signed release APK and publishes a GitHub **prerelease** tagged dev-<run number>. Without signing secrets it skips only the signed prerelease; it never publishes an unsigned artifact as a release. No secrets are exposed to PR checks.

### TypeSafe AI integration tests

All PRs run deterministic, offline `SystemOneClientTest` checks for request schema, untrusted SMS as data, fixed reply ownership, probability threshold, invalid/missing responses, and fail-closed configuration. They require no secret. The JUnit `SystemOneLiveTest` uses two **synthetic** messages to compare promotional vs personal classifications with Jev. It is skipped in ordinary PR jobs (no secret in PR contexts) and run as a separate `TypeSafe live API smoke test` job on trusted `main` pushes or a manually dispatched workflow.

Add `TYPESAFE_AI_API_KEY` under GitHub Actions secrets for this optional paid API contract test; the release job intentionally waits for the live smoke check. This CI key is used **only for tests**; users must enter their own TypeSafe API key into their app. No automated SMS is sent by tests. Run it manually from Actions → Android CI & prerelease → Run workflow, choosing the feature branch to verify live credentials before merging.

### Initial-version safety behavior

Monitoring and automatic sending are separate toggles. Preview mode evaluates eligible incoming SMS without sending. Explicit automatic sending is confirmed in a warning dialog and protected by an Android SEND_SMS permission check. Eligibility rejects short codes, one-time-password messages and messages that only say STOP or other common unsubscribe commands. Duplicate messages within 24 hours are ignored. Even different messages from the same sender can trigger at most one automated reply in 24 hours. Each settings save invalidates previously queued messages, and the Pause button immediately disables monitoring and sending. These checks do not make AI classification infallible: use preview mode first.

## Roadmap

Improve multi-SIM support, anti-loop rate limiting, persisted decision queue and audit UI, fake-provider integration tests, and policy-compliant store distribution if desired.
