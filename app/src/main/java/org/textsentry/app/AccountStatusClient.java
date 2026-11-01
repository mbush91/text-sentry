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
        Status(String userId, String subscription, boolean managedAccess) {
            this.userId = userId;
            this.subscription = subscription;
            this.managedAccess = managedAccess;
        }
        boolean subscribed() { return "active".equals(subscription); }
        String label(boolean sandbox) {
            if (subscribed()) return sandbox
                    ? "TextSentry subscription active (sandbox)"
                    : "TextSentry subscription active";
            if ("inactive".equals(subscription)) return "Not subscribed";
            return "Subscription status unavailable";
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
            return new Status(userId, subscription, data.optBoolean("managed_access", false));
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
