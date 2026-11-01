package org.textsentry.app;

import static org.junit.Assert.*;
import org.junit.Test;

public class AccountStatusClientTest {
    @Test public void verifiedEntitlementIsDistinctFromBetaManagedAccess() {
        AccountStatusClient.Status beta = AccountStatusClient.parse(
                "{\"user_id\":\"cognito-sub\",\"managed_access\":true,\"subscription_required\":false,\"subscription_status\":\"inactive\"}");
        assertEquals("Not subscribed", beta.label(false));
        assertFalse(beta.subscribed());

        AccountStatusClient.Status subscribed = AccountStatusClient.parse(
                "{\"user_id\":\"cognito-sub\",\"managed_access\":true,\"subscription_required\":false,\"subscription_status\":\"active\"}");
        assertTrue(subscribed.subscribed());
        assertEquals("TextSentry subscription active (sandbox)", subscribed.label(true));
    }

    @Test public void providerOutageAndLegacyBackendNeverClaimPaid() {
        AccountStatusClient.Status unreachable = AccountStatusClient.parse(
                "{\"user_id\":\"cognito-sub\",\"managed_access\":true,\"subscription_status\":\"unavailable\"}");
        assertFalse(unreachable.subscribed());
        assertEquals("Subscription status unavailable", unreachable.label(false));

        AccountStatusClient.Status legacy = AccountStatusClient.parse(
                "{\"user_id\":\"cognito-sub\",\"managed_access\":true}");
        assertFalse(legacy.subscribed());
        assertEquals("Subscription status unavailable", legacy.label(false));
    }

    @Test public void unsupportedResponseIsRejected() {
        for (String data : new String[] {"{}", "not json",
                "{\"subscription_status\":\"active\"}",
                "{\"user_id\":\"\",\"subscription_status\":\"active\"}",
                "{\"user_id\":\"abc\",\"subscription_status\":\"unexpected\"}"}) {
            try {
                AccountStatusClient.parse(data);
                fail("Must reject malformed account status");
            } catch (IllegalArgumentException expected) {}
        }
    }
}
