# TextSentry

**Less noise. More signal.**

TextSentry is an Android message assistant powered by **TypeSafe Jev System One**. New users sign in with Google to use a monthly $1-capped OpenRouter key automatically; or they can skip registration and bring their own TypeSafe API key (BYOK). Both modes use the same single Jev yes/no decision, selected message categories and 50–100% threshold (default **95%**). The only automated reply is the fixed **STOP**.

## TextSentry account (Google sign-in)

The Play and sideload editions now offer Google sign-in / sign-up through AWS Cognito. First-time Google users create an account and returning users sign in through the same browser-based authorization-code + S256 PKCE flow. The app loads public Cognito configuration from `https://api.textsentry.org/v1/auth/config`, then keeps Cognito access/ID/refresh tokens encrypted locally with a separate Android Keystore AES-GCM key. Sign-out erases the local session, as does Delete all app data. After sign-in (or after reopening the Account screen with a saved session), the app refreshes Cognito tokens if necessary and retrieves the active-month OpenRouter key through `GET https://api.textsentry.org/v1/key`. The key remains **in process RAM only**; the Account screen reports readiness without displaying or storing the key. A Refresh managed key action is available; sign-out and data deletion clear memory.

**Required setup:** The launcher opens a minimal native setup screen. **Continue with Google** is the primary action; a small **Skip sign-up · Bring your own key** link expands a secure API key field. No Home, Settings, History, or developer screens are accessible until a Google session or BYOK key has been saved. This applies after sign-out and local-data removal too, even on older installs where the previous optional welcome was dismissed. Existing configured users skip setup. Play shared text is preserved in memory while completing setup. **Unified analysis:** If signed in, Play manual review, sideload sample tests and sideload monitoring all use Jev over **OpenRouter Decisions API** with the monthly managed key. If no account exists and a BYOK key has been saved, they use Jev over the direct **TypeSafe System One** endpoint. Both flows use the exact same rules, threshold, local safety checks and fixed STOP reply. An expired RAM-only key is automatically retrieved using the encrypted Cognito session from AWS; the managed key itself is never saved to device storage. If signed in but key fetch fails, no automatic fallback sends any credential to a different provider. Sign-in does not enable monitoring or auto-replies. No SMS contents reach TextSentry AWS.

Two distributions are maintained:

- **Play flavor:** A manual assistant for pasted/shared text. It requires explicit per-analysis consent, never monitors incoming SMS, never sends SMS automatically and holds no restricted SMS permissions. The Play flavor is **not yet approved or published**.
- **Sideload flavor:** An experimental tool to monitor eligible incoming SMS and optionally send STOP when local safeguards and the user-selected threshold pass. Monitoring and automatic replies remain **off by default**, with independent permission/consent checks and a one-tap pause.

## Guided first-run setup

Both editions begin with a branded Google sign-in or optional on-device TypeSafe BYOK. Until the wizard is completed, Home, Settings, History and developer tools are inaccessible (including on upgrades from earlier releases). The compact navy header uses the shared TextSentry shield and wordmark, with website-matched type, cyan accent, and the same "Stop sorting through spam" messaging.

**Experimental sideload:** (1) sign in or BYOK, (2) open Android app battery settings and choose **Unrestricted** when available, (3) explicitly grant incoming SMS, with outgoing SMS optional, and (4) **Start monitoring**. The final action starts **preview-only**, never sends automatic replies. Unrestricted battery is recommended but not required; it does not grant SMS permissions. When the installer hard-restricts SMS access, Android can refuse the grant; the wizard offers app-info and troubleshooting, and users may finish without monitoring. No SMS text goes to TextSentry AWS.

**Google Play:** (1) sign in or BYOK, (2) start manually reviewing shared/pasted texts. No SMS runtime permissions, background monitoring or auto-reply controls exist in Play flavor.

Existing users need to complete the guide once after this release; credentials, message history, categories, confidence and existing BYOK settings remain intact. Resetting app data also resets onboarding.

## App experience

Home keeps monitoring controls and sample analysis up front (sideload), or manual analysis and optional SMS compose (Play). Account, history, and settings are simple secondary actions; detailed rules and privacy controls remain available in Settings.

### Message categories

The Settings page provides three independent checkboxes:

- **Go vote reminders** — unsolicited nonpartisan get-out-the-vote reminders.
- **Political campaign messages** — unsolicited candidate, political party, fundraising and campaign appeals.
- **Marketing ads** — unsolicited commercial promotions, coupons and advertisements.

All three categories are checked by default for new installations and for users who have not saved a selection. Explicitly saved choices, including unchecked categories, remain unchanged. Selected categories are combined using an **OR** into ONE Jev yes/no classification request per eligible, nonduplicate SMS, with the optional extra restrictions appended to that same instruction. There are no separate requests for each checked category. With none selected, analysis is skipped entirely.

Jev now judges the message's purpose and category even if it does **not** contain a STOP or unsubscribe instruction. A message matching any enabled category can qualify at the configured confidence threshold. Official voter registration/polling place notices, personal texts, OTPs, and ambiguous texts remain excluded. This is opt-out filtering, not political messaging or voter outreach; no voter information is deleted and TextSentry never sends election content to others. A false positive may still occur, so use preview mode before allowing automatic replies. A separate **Settings** screen provides encrypted API-key entry/removal, category checkboxes, confidence slider and privacy/data controls. Advanced extra prompt editing is available only in hidden developer tools. Changing the policy invalidates queued SMS work, and upgrading from a legacy chat provider clears the old credential before any TypeSafe call.

### Hidden developer mode

In **Settings**, tap the **AI connection** heading **seven times**, then confirm the privacy warning. A new **Developer mode** card links to **Developer tools** in both app editions. The developer page lets you write, save or clear optional **Additional prompt** text; it is appended to the selected category instructions in the **same Jev request**. Developer text cannot enable unchecked categories, change the fixed STOP reply or override local safety checks. Existing saved extra instructions remain active if developer mode is switched off. Changes invalidate queued processing using older rules.

While enabled, developer mode captures the **five most recent TypeSafe request JSON bodies and raw response JSON bodies** for sideload SMS, sideload sample tests, and consented Play manual analysis. The trace is encrypted locally with the history Keystore key; it excludes the API key and Authorization header. Request JSON includes SMS text and filtering rules. The developer page offers refresh, a clear-traces action and a disable action. Disabling stops recording but does not delete existing traces; clearing history or app data also erases traces. No extra network requests or backend uploads are performed.

### Message history and Android permissions

The new **History** page is linked from Home and Settings. It includes each evaluated SMS's sender, full text, time, TypeSafe Jev yes probability, recommended fixed reply, and whether Android confirmed the SMS send, it was only previewed, it failed, or no reply was recommended. Results are kept in an **AES-256-GCM encrypted**, Keystore-backed, app-private archive of at most 100 entries (newest first). Clearing history wipes the archive and the previous short decision log. Full app data erasure also destroys the history encryption key. These are local records and cannot reconstruct messages evaluated before this update.

Sideload permissions: **RECEIVE_SMS** is requested for monitoring; **SEND_SMS** is requested for optional auto-replies. Home shows live permission statuses, request buttons, a link to Android's TextSentry app-info page, and a troubleshooting dialog. Granting SMS-send permission never enables auto-replies without separate consent. Neither the app's own processed-message history nor send-status callbacks require **READ_SMS**. The Play flavor does not request SMS/Call Log permissions; it tracks only user-consented manual analyses, clearly labeled as unverified suggestions, not sent SMS.

#### Set up SMS access (sideload APK)

1. Open **TextSentry > Home > Permissions** and tap **Allow incoming SMS**. Allow it if Android offers a dialog. To use automatic replies, separately tap **Request outgoing SMS access** and then **Enable auto-replies** after previewing first.
2. If Android does not grant access, tap **Open TextSentry app info**, then **Permissions > SMS > Allow** if that option is available. Return to the app; the statuses refresh automatically. Android menus vary across manufacturers.
3. If SMS is unavailable, permanently denied, or a request closes without a grant, tap **SMS permission troubleshooting**. Android classifies `RECEIVE_SMS` and `SEND_SMS` as **hard-restricted** permissions. The installer or an eligible system role must allowlist them; an ordinary runtime prompt cannot override this. The user cannot manually add an installer allowlist entry. A compatible/authorized installation method may be needed. TextSentry is **not** a full default SMS app, so it does not offer a bogus SMS-role switch as a shortcut.
4. For SMS received successfully but analyzed late, optionally open **Battery / App battery usage** from TextSentry's Android app-info page and select **Unrestricted** (if supported). The home screen also links to Android's battery optimization settings and reports whether Android considers TextSentry exempt. **Unrestricted is not an SMS permission**, does not fix a missing grant, and can consume more battery. Some manufacturers provide additional battery management controls.

On Android 13+, the overflow menu entry **Allow restricted settings** is intended for restricted special settings (such as accessibility or notification listener access); **it is not an installer allowlist for restricted SMS permissions**. Do not enable unrelated sensitive access as a workaround. If SMS grants are blocked by the installation path, TextSentry cannot programmatically bypass the restriction.

**Message deletion:** Each history row says **Not deleted by TextSentry** for monitored SMS, or **Not applicable** for manual analysis. These statuses describe the app's action, not whether another SMS app has subsequently deleted the original message. Clear History and full data reset erase TextSentry records, **not original SMS in Android Messages**. Android permits writing/deleting from the SMS provider only for the device's chosen **default SMS app**. TextSentry is not a complete SMS/MMS replacement and does not claim that role or request unnecessary inbox privileges. See issue #15 for the full default-SMS-handler work needed to enable optional original-message deletion.

**Privacy:** An eligible incoming SMS in the sideload flavor or text explicitly consented in Play is transmitted directly to **TypeSafe** with BYOK or **OpenRouter/TypeSafe** with a managed key, subject to those providers' privacy/retention terms. The app uses a TextSentry AWS account/key-management backend, but never sends SMS text to it. There is no advertising SDK or analytics. Encrypted API keys live on the phone. WorkManager can temporarily store queued SMS data on-device; the encrypted, bounded local history includes full processed message text, sender, match probability and reply status.

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

## RevenueCat Web Purchase Link (sideload preparation)

The independent sideload flavor has an identified-user checkout integration using the RevenueCat Web Purchase Link `https://pay.rev.cat/mimrlvpobvqnjikw/`. The checkout URL must **never** be opened directly as a generic shared link: the app gets the Cognito `sub` from its authenticated `GET /v1/me` response and URL-encodes it into `https://pay.rev.cat/mimrlvpobvqnjikw/<cognito-sub>`. Checkout opens in the device browser and does not receive Cognito bearer tokens or SMS. The product is `textsentry_monthly`, with entitlement `textsentry`.

**Not live yet.** To prevent taking money before the backend validates paid entitlements, the checkout button is omitted from ordinary release builds.

### Private sandbox checkout test build

The product owner has supplied a separate RevenueCat **sandbox** Purchase Link. Keep the token out of source control, public downloads, release notes and customer messages; it can grant test subscriptions without real payment. Store the **entire URL**, ending in `/`, in the GitHub Actions repository secret `TEXTSENTRY_REVENUECAT_SANDBOX_URL` (Settings → Secrets and variables → Actions). Also configure the existing `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` signing secrets.

Run **Actions → Private RevenueCat sandbox APK → Run workflow** on `main`. The manual workflow rejects non-sandbox checkout URLs, enables checkout only in its sideload build, runs offline tests and Android lint, builds and verifies a signed release APK, and uploads the `TextSentry-SANDBOX-checkout-DO-NOT-DISTRIBUTE` artifact for **one day**. It never creates a GitHub Release or modifies the regular production build.

Install the artifact only on a controlled test device. It shares the standard `org.textsentry.app` package ID and signing key, so it **replaces** the usual sideload edition rather than coexisting. Sign in with Google, open **Account → Subscribe to TextSentry**, complete checkout in your browser using Stripe's test card details, and return to the app. In the RevenueCat dashboard, search for the same Cognito `sub` displayed by TextSentry's authenticated `GET /v1/me`; verify product `textsentry_monthly` grants entitlement `textsentry`, and test cancellation/expiration.

**Billing verification caveat:** AWS currently has `EnforceSubscriptions: 'false'`, so managed-key readiness or successful analysis **cannot** prove purchase verification. Confirm the RevenueCat entitlement first and test the paid AWS check in a controlled environment before ever switching live enforcement on. The app does not currently confirm a successful payment on its own, and a real subscriber should not be charged on this test build.

 Set `TEXTSENTRY_ENABLE_WEB_CHECKOUT=true` **at build time** for controlled sideload testing only. To run a sandbox purchase, set `TEXTSENTRY_WEB_CHECKOUT_URL` to the exact separate RevenueCat sandbox URL during the test build. The build rejects URLs outside `pay.rev.cat`; the Play flavor always excludes this web-checkout button. Signed releases built by normal main CI remain disabled until checkout-to-entitlement verification is complete. Sandbox purchases require RevenueCat's **separate sandbox URL**, which must not be distributed publicly. The provided link has not been independently verified as production or sandbox.

Before activating customer checkout: verify RevenueCat/Stripe live setup and the separate sandbox checkout end-to-end, verify subscription `textsentry` with the same Cognito `sub`, verify AWS RevenueCat secret and key issuance, and then deliberately enable `EnforceSubscriptions: 'true'` in IaC. Until then the backend beta setting bypasses subscription checks. Do not interpret a successful key fetch while enforcement is disabled as proof of payment. BYOK remains independent and free. Do not expose a website-wide generic purchase link that lacks a signed-in user identity.

## Signing and release

Configure GitHub Actions repository secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` to enable optional signed release publishing on main. Until configured, the signed-release job skips publication while debug APK artifacts still work. Protect main with required quality and emulator checks; merely defining workflows is not branch protection.

## Launcher branding

Both Android editions use the official [TextSentry shared-brand icon](https://github.com/TextSentry/test-sentry/blob/main/assets/textsentry-icon.svg): a blue-to-purple security shield, white messaging bubble and red block symbol on dark navy. The adaptive launcher icon is recreated as Android vector drawables (foreground, gradient background and a monochrome version for themed launchers), so Android can mask and scale it cleanly. The icon source of truth remains the shared branding repository.
