# TextSentry Privacy Policy

**Last updated:** November 1, 2026

**App:** TextSentry (`org.textsentry.app`). This policy applies to both the Google Play edition and the separately distributed sideload edition.

**Contact and privacy inquiries:** Please open a confidential inquiry through the repository's [security reporting](https://github.com/mbush91/text-sentry/security/advisories/new), or use [project support](https://github.com/mbush91/text-sentry/issues) without including any personal messages, phone numbers, or API credentials.

## What TextSentry does

TextSentry is a message-assistance app with optional Google sign-in through AWS Cognito. Its current message analysis remains bring-your-own-key (BYOK); the managed-key subscription service is being developed and is **not yet integrated into Android message processing**. TextSentry operates a limited AWS account/key-management backend, but **does not send SMS messages or their contents to that backend**. It has no advertising or analytics SDK.

**Google Play edition:** You decide what text to paste or explicitly share with the app. The Play edition does not read incoming messages automatically, cannot send messages automatically, does not request SMS/call-log permissions, and cannot inspect your SMS inbox. You may review and edit an AI suggestion, then manually open your existing SMS app to compose a reply. The SMS app, not TextSentry, sends it after your approval.

**Sideload edition:** After you explicitly enable monitoring and grant Android SMS permissions, eligible incoming SMS content and sender numbers can be evaluated through your configured AI provider. Auto-send is separately optional. WorkManager may hold unprocessed SMS and sender data in app-private on-device storage until processing and cleanup. Once evaluated, messages are saved into an app-private, AES-GCM-encrypted history (most recent 100 decisions). History includes the SMS sender, full processed text, evaluation time, TypeSafe model confidence, reply suggestion, Android send result and an explicit indicator that TextSentry did not delete the original SMS. The existing shorter decision outcomes, hashed deduplication records and timestamps are also kept. The full inbox is never read or copied.

## Information handled

- If you choose Google sign-in/sign-up, Google and AWS Cognito receive the OAuth authentication information needed to identify your account (including email and Cognito user ID). The app requests only basic Google identity scopes (`openid`, `email`, `profile`) and receives access, ID and refresh tokens from Cognito. These tokens are encrypted with a separate Android Keystore-backed AES-GCM key in app-private storage so the account can remain signed in between launches. The OAuth browser handles your password; the app does not read it. Sign-out erases the local tokens, not the remotely created Cognito account. The TextSentry backend stores account identity and may associate future subscription/key metadata; it never receives SMS content.
- Your TypeSafe API key, confidence threshold and instructions are stored locally on your device. The API key is encrypted using Android Keystore. The local message-history archive uses a separate Android Keystore-backed AES-256-GCM encryption key. The provider and model are not configurable.
- The text you explicitly submit in the Play edition, or eligible SMS body and sender in the sideload edition when monitoring is on, is transmitted **to TypeSafe AI (Jev System One)** to obtain an evaluation. The provider and model are fixed in this version. The generic chat interface uses a placeholder sender number for manually submitted Play text.
- The AI provider may process, store or retain submitted content in accordance with **its own privacy policy and terms**. Check those terms before sending private or sensitive text. Do not submit one-time codes, passwords or other sensitive content.
- In the Play edition, text submitted with your per-message consent and its probability/suggestion are stored in TextSentry's encrypted local history. The number you enter to compose an SMS is passed only to the installed SMS app selected by Android. TextSentry cannot verify whether the SMS app actually sent the message. TextSentry does not send that number to the AI provider.

## Your choices and consent

Creating an account or signing in with Google is optional. The first-time and returning-user login experiences both use Google OAuth; no TextSentry password account is offered. Account sign-in does not enable SMS monitoring or authorize paid key issuance. When you sign out, the local OAuth session is removed; browser/Google session cookies may remain in the system browser. Google and AWS may retain sign-in records under their own policies. For account deletion inquiries, use the support contact above; the in-app local deletion control is not a remote account-deletion request.

In the Play edition, no message is submitted to AI without checking the consent box and selecting **Analyze**. That choice is reset after an analysis. In the sideload edition, you must separately enable SMS monitoring and automatic sending. You can pause sideload monitoring or delete local TextSentry data in either edition.

## Retention and deletion

TextSentry now supports optional cloud-backed Google/Cognito accounts. Account identity and login processing may be retained by Google and AWS. Signing out or clearing local app data does not delete a Cognito account or erase external provider records. The History page keeps up to 100 encrypted results and offers a **Clear history** control; use **Clear local decision history** to remove TextSentry's encrypted history and short outcome log, **Delete all local TextSentry data** to remove all local app records, or Android Settings > Apps > TextSentry > Storage > Clear storage, to erase the locally stored configuration, API key, encrypted account tokens, decision history and deduplication records. TextSentry also cancels scheduled work when local data is deleted. These controls do **not** delete SMS messages from the device's default messaging app; inbox deletion requires default SMS handler capabilities that TextSentry does not currently provide. Deleting local data **cannot delete information already transmitted** to your independent AI provider; contact that provider for its deletion procedure.

## Sharing, security and children

TextSentry does not sell your data or share data for advertising. Data transmitted to your chosen provider uses HTTPS. Access to SMS in the sideload edition is controlled by Android permissions. TextSentry is not designed for children, and SMS classification is never guaranteed to be correct.

Sideload requests RECEIVE_SMS for opt-in monitoring and SEND_SMS for separately consented auto-replies, with in-app permission status and requests. SMS deletion would require TextSentry to be your actual default SMS app, which is not offered; a history row's "Not deleted by TextSentry" indicator does not assert whether another app subsequently deleted the message. Android send confirmations are used to distinguish a submitted send from a confirmed sent attempt. The Play edition and sideload edition have different permissions and functionality. This policy may be updated as the product changes; the dated version is published in this repository.
