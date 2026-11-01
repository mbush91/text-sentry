package org.textsentry.app;

import static org.junit.Assert.*;
import org.junit.Test;

public class WebCheckoutTest {
    @Test public void checkoutAppendsCognitoIdentityAsEncodedPathNotQuery() {
        assertEquals("https://pay.rev.cat/mimrlvpobvqnjikw/123e4567-e89b-12d3-a456-426614174000",
                WebCheckout.purchaseUrl("123e4567-e89b-12d3-a456-426614174000"));
        assertEquals("https://pay.rev.cat/mimrlvpobvqnjikw/abc%2Fdef%20ghi",
                WebCheckout.purchaseUrl("abc/def ghi"));
    }

    @Test public void sandboxCheckoutCanBeInjectedWithoutChangingIdentity() {
        assertEquals("https://pay.rev.cat/sandbox/test_token/user-id",
                WebCheckout.purchaseUrl("user-id", "https://pay.rev.cat/sandbox/test_token/"));
        try {
            WebCheckout.purchaseUrl("user-id", "https://evil.example/checkout/");
            fail("Only RevenueCat checkout host is allowed");
        } catch (IllegalArgumentException expected) {}
    }

    @Test public void refusesAnonymousCheckout() {
        for (String id : new String[] {null, "", "  "}) {
            try {
                WebCheckout.purchaseUrl(id);
                fail("Checkout must require an account ID");
            } catch (IllegalArgumentException expected) {}
        }
    }

    @Test public void onlyUsesBackendProvidedCognitoSub() {
        assertEquals("cognito-sub-123",
                WebCheckout.userIdFromAccount("{\"user_id\":\"cognito-sub-123\",\"managed_access\":false}"));
        for (String response : new String[] {"{}", "{\"email\":\"test@example.com\"}",
                "{\"user_id\":\"\"}", "not-json"}) {
            try {
                WebCheckout.userIdFromAccount(response);
                fail("No usable Cognito ID");
            } catch (IllegalArgumentException expected) {}
        }
    }
}
