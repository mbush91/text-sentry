package org.textsentry.app;

import org.json.JSONObject;
import java.io.IOException;

/** Subscription state from the Cognito-protected account API, not a checkout redirect. */
final class AccountStatusClient {
    private AccountStatusClient() {}

    static final class Status {
        final String userId;
        final String subscription;
        final boolean managedAccess;
        final String reason;
        final String expiresAt;
        final boolean isSandbox;
        Status(String userId, String subscription, boolean managedAccess,
               String reason, String expiresAt, boolean isSandbox) {
            this.userId = userId;
            this.subscription = subscription;
            this.managedAccess = managedAccess;
            this.reason = reason;
            this.expiresAt = expiresAt;
            this.isSandbox = isSandbox;
        }
        boolean subscribed() { return "active".equals(subscription); }
        String label(boolean sandboxBuild) {
            if (subscribed()) return sandboxBuild
                    ? "TextSentry subscription active (sandbox)"
                    : "TextSentry subscription active";
            if ("unavailable".equals(subscription)) return "Subscription status unavailable";
            if ("expired".equals(reason)) return isSandbox || sandboxBuild
                    ? "Sandbox subscription expired" : "Subscription expired";
            if ("missing_entitlement".equals(reason))
                return "Purchase is missing the textsentry entitlement";
            if ("no_purchase".equals(reason))
                return "No purchase found for this account";
            return "Not subscribed";
        }
    }

    static Status parse(String body) {
        try {
            JSONObject data = new JSONObject(body);
            String userId = data.getString("user_id");
            String subscription = data.optString("subscription_status", "unavailable");
            if (userId.isBlank() || userId.length() > 256
                    || !("active".equals(subscription) || "inactive".equals(subscription)
                            || "unavailable".equals(subscription))) {
                throw new IllegalArgumentException("Invalid account status");
            }
            return new Status(userId, subscription, data.optBoolean("managed_access", false),
                    data.optString("subscription_reason", ""),
                    data.optString("subscription_expires_at", ""),
                    data.optBoolean("subscription_sandbox", false));
        } catch (Exception ignored) {
            throw new IllegalArgumentException("Invalid account status");
        }
    }

    static Status fetch(String accessToken) throws IOException {
        try {
            return parse(WebCheckout.fetchAccount(accessToken));
        } catch (IllegalArgumentException ignored) {
            throw new IOException("Unable to verify your subscription.");
        }
    }
}
