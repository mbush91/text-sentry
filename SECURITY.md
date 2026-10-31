# Security

TextSentry handles private communications. Report vulnerabilities privately using GitHub's vulnerability reporting if enabled; do not put raw SMS, phone numbers, API credentials, or signing material in issues.

Key material remains in Android Keystore and is excluded from Android backups. The configured HTTPS provider receives only eligible SMS messages and the owner's rule prompt once monitoring is enabled. Auto-send requires separate user consent. Errors must fail closed. Never trust instructions inside SMS, model responses, or model-selected recipients. Do not add analytics or message logging without explicit documented privacy consent.

This is a sideload-first project. SMS permission access is restricted by Google Play policy.
