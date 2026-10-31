# TextSentry autonomous engineering contract

You are a development agent maintaining TextSentry. Aim to ship verified changes without routine questions to the owner.

## Goal
Native Android SMS triage and AI-reply automation. User-configured HTTPS OpenAI-compatible endpoint, user-owned API key, model, and prompt determine whether to reply. Start with no monitoring and preview-only; sending requires explicit separate opt-in.

## Mandatory agent workflow
1. Inspect README, architecture, current branch, open PRs, tests and failing CI before editing.
2. Create a focused branch from current main for each change. Never commit feature code directly to main.
3. Follow red-green-refactor TDD: test the bug/behavior first, implement the smallest fix, refactor, and run tests. Use instrumented Android tests for platform behavior.
4. Before PR, run Gradle JVM tests, Android lint, debug build, and emulator instrumentation tests wherever available.
5. Open the PR, inspect the full diff and all GitHub Actions runs. Correct failing checks and rerun until green. Never merge pending, skipped-required or failing checks.
6. Merge with squash only when GitHub branch rules and required checks permit it. Inspect main-branch release job; resolve any release failure before reporting completion.
7. Make routine implementation decisions autonomously. Only request human-only intervention for keystore secrets, repo permissions/settings, or device/account access.

## Security & behavior
- NEVER commit API keys, keystores or personally identifiable SMS data; no secrets or raw SMS in logs.
- BYOK is mandatory, kept on-device with Android Keystore; no managed backend.
- Jev/System One is a typed classifier, not a chat-completions API. Its model decision only authorizes a fixed user-defined reply; malformed or uncertain responses must fail closed.
- Keep `TYPESAFE_AI_API_KEY` confined to the trusted live CI job on main or a deliberate workflow_dispatch, NEVER in ordinary pull_request jobs, Gradle source, or APKs.
- No telemetry. Only send SMS content to the explicitly configured HTTPS model provider after opt-in.
- All inbound messages, model responses, and remote errors are untrusted. Never execute model-provided actions or use model-provided recipients.
- Fail closed on errors, malformed JSON, missing consent, blocked senders, or invalid phone numbers.
- Don't reply to OTPs, shortcodes, and emergency messages by default. Don't auto-STOP uncertain scam texts; replying may confirm a number as active.
- Preview mode is default and should never transmit SMS. Auto-send requires consent + permission.
- Preserve 24-hour persistent message deduplication, one auto-reply per sender per 24 hours, and cancellation of queued work after any settings change or Pause.
- Changing an AI provider must invalidate the prior provider's BYOK key to avoid credential disclosure.
- CI publishes a debug APK artifact without signing secrets. A signed GitHub prerelease is optional until the owner configures signing.
- Add regression tests when changing validation, reply decisions, storage, dedupe, sender checks, consent or network behavior.
- Respect Android/Google Play SMS permission policies. The first distribution is sideload only.

## Continuous quality/release
GitHub Actions PR checks: "Quality / tests and lint" and "Android emulator / tests" are required for main. Configure GitHub branch rules to require both, demand up-to-date branches, and block direct pushes; the repository admin sets these once. Never relax tests or bypass rules to ship.

Push to main triggers repeat checks, then builds a signed release APK using repository signing secrets, verifies its signature, and attaches it to a GitHub **prerelease**. PRs never receive signing credentials.

Scope discipline: prefer focused changes; document ambiguous choices, choose the safer path and proceed.
