package org.textsentry.app;

import static org.junit.Assert.*;
import org.junit.Test;

public class ManagedKeyClientTest {
    @Test public void parsesIssuedKeyAndMonthExpiryWithoutExposingItInStatus() throws Exception {
        String json = "{\"key\":\"synthetic-key-not-real\",\"period\":\"2099-11\","
                + "\"expires_at\":\"2099-12-01T00:00:00Z\",\"monthly_limit_usd\":1}";
        ManagedKeyClient.KeyResponse key = ManagedKeyClient.parse(json);
        assertEquals("synthetic-key-not-real", key.key);
        assertEquals("2099-11", key.period);
        assertTrue(key.expiresAtMillis > System.currentTimeMillis());
        assertFalse(key.status().contains("synthetic-key-not-real"));
    }

    @Test public void revokedKeyErrorIsNotConfusedWithSubscriptionOrMonthlySpend() {
        assertEquals("This month's managed key was revoked and cannot be reissued until next month.",
                ManagedKeyClient.failureMessage(429,
                        "{\"error\":\"key_revoked_until_next_month\"}"));
        assertEquals("Too many requests. Try again shortly.",
                ManagedKeyClient.failureMessage(429, "{}"));
        assertEquals("An active subscription is required for managed access.",
                ManagedKeyClient.failureMessage(402,
                        "{\"error\":\"active_subscription_required\"}"));
        assertEquals("Provisioning in progress. Try again shortly.",
                ManagedKeyClient.failureMessage(409, ""));
        assertEquals("Managed key unavailable (HTTP 503).",
                ManagedKeyClient.failureMessage(503, ""));
    }

    @Test public void refusesExpiredMissingOrOverBudgetKeys() throws Exception {
        for (String json : new String[] {
            "{\"key\":\"x\",\"period\":\"2020-01\",\"expires_at\":\"2020-02-01T00:00:00Z\",\"monthly_limit_usd\":1}",
            "{\"period\":\"2099-11\",\"expires_at\":\"2099-12-01T00:00:00Z\",\"monthly_limit_usd\":1}",
            "{\"key\":\"x\",\"period\":\"2099-11\",\"expires_at\":\"2099-12-01T00:00:00Z\",\"monthly_limit_usd\":5}"
        }) {
            try {
                ManagedKeyClient.parse(json);
                fail("Invalid/overbudget or expired key must be rejected");
            } catch (IllegalArgumentException expected) {
                // Never echo response bodies or key strings in failures.
            }
        }
    }
}
