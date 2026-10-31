# TextSentry

**Your messages. Your rules. AI-powered replies.**

TextSentry is an early-stage Android SMS triage agent. It evaluates incoming SMS against a personal prompt using an OpenAI-compatible **HTTPS chat-completions** model with your own API key (BYOK). It can preview a suggested response or automatically send it after separate opt-in.

**Initial distribution is sideload only.** Google Play tightly restricts RECEIVE_SMS / SEND_SMS for non-default SMS handlers; this app does not claim Play eligibility.

## What it currently does

- Configurable HTTPS endpoint, model, API key and natural-language reply prompt.
- API key encrypted at rest via Android Keystore. No backend, no telemetry, no cloud message history.
- SMS broadcast handled quickly; network requests delegated to Android WorkManager.
- Local safety checks for shortcodes, OTPs and oversize messages before any model call.
- Strict JSON output: should_reply boolean and reply string of at most 120 chars.
- **Monitoring off by default; auto-send off by default.** Preview decision button never sends SMS.
- Local outcome history without raw SMS content.
- JVM unit tests, Android lint, emulator instrumentation, and signed GitHub prereleases.

Start prompt: "Only reply STOP to unmistakably opt-in marketing messages that explicitly describe a STOP unsubscribe option. Ignore personal messages, scams, transactional notices, and uncertainty."

## Getting started

Install a prerelease APK (after signing secrets are configured), open TextSentry and configure your HTTPS chat-completions endpoint (for example https://api.openai.com/v1/chat/completions), model identifier, and API key. Test a sample message. Enable monitoring and grant RECEIVE_SMS for preview mode. Only enable auto-send after reviewing behavior and granting SEND_SMS. Your AI provider may charge for requests and your mobile carrier may charge for SMS.

**Privacy:** When monitoring is enabled, incoming SMS bodies and senders eligible for evaluation are transmitted to your chosen AI provider under its privacy and retention terms. Received messages are not permanently stored by TextSentry. BYOK means users must provide their own provider account and key. On-device outcome history includes reply text and timestamps but no message bodies. This is an experimental tool; keep previews on while testing.

**Limitations:** SMS only (not RCS/MMS/third-party apps). Short codes are blocked by default; this may omit legitimate opt-outs. AI classification can be wrong. SMS delivery and multi-SIM behavior vary. A STOP reply to a scammer may confirm the phone number is active. WorkManager may delay evaluations. SMS permissions and Play Store compliance require separate consideration.

## Development

Requires JDK 17 and Android SDK API 36. The checked-in Gradle wrapper pins Gradle 8.13.

    ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
    ./gradlew :app:connectedDebugAndroidTest

The unsigned debug build is at app/build/outputs/apk/debug/app-debug.apk. See AGENTS.md for required autonomous TDD/PR/CI procedure. CI is the source of truth and agents should iterate on failures.

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

Every push to main runs both quality suites, then uses the four secrets to build and verify a signed release APK and publish a GitHub **prerelease** with tag dev-<run number>. Missing secrets fail release intentionally rather than publishing unsigned artifacts. No secrets are exposed to PR checks.

## Roadmap

Improve multi-SIM support, anti-loop rate limiting, persisted decision queue and audit UI, fake-provider integration tests, and policy-compliant store distribution if desired.
