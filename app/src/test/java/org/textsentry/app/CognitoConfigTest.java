package org.textsentry.app;

import static org.junit.Assert.*;
import org.junit.Test;

/** Public auth discovery contract; no network or user data in unit tests. */
public class CognitoConfigTest {
    private static final String DOMAIN =
            "https://textsentry-prod-123456789012.auth.us-east-1.amazoncognito.com";

    @Test public void acceptsCognitoConfigurationForExpectedAndroidCallback() throws Exception {
        CognitoConfig config = CognitoConfig.parse("{\"client_id\":\"publicclient123\","
                + "\"authorization_server\":\"" + DOMAIN + "\","
                + "\"redirect_uri\":\"textsentry://auth/callback\"}");
        assertEquals("publicclient123", config.clientId);
        assertEquals(DOMAIN + "/oauth2/authorize", config.authorizationEndpoint());
        assertEquals(DOMAIN + "/oauth2/token", config.tokenEndpoint());
        assertEquals("textsentry://auth/callback", config.redirectUri);
    }

    @Test public void rejectsUntrustedHostsSchemesAndCallbacks() throws Exception {
        for (String host : new String[] {
                "http://textsentry-prod-123456789012.auth.us-east-1.amazoncognito.com",
                "https://textsentry.org.evil.com",
                "https://evil.amazoncognito.com",
                "https://textsentry-prod-123456789012.auth.us-east-1.amazoncognito.com.evil.com",
                "https://textsentry-prod-123456789012.auth.us-east-1.amazoncognito.com/path",
                "https://user@textsentry-prod-123456789012.auth.us-east-1.amazoncognito.com"
        }) {
            try {
                CognitoConfig.parse("{\"client_id\":\"public\","
                        + "\"authorization_server\":\"" + host + "\","
                        + "\"redirect_uri\":\"textsentry://auth/callback\"}");
                fail("Must reject " + host);
            } catch (IllegalArgumentException expected) {}
        }
    }

    @Test public void rejectsForeignCallbackAndMissingClient() {
        for (String client : new String[] {"", "bad client", "x".repeat(200)}) {
            try {
                CognitoConfig.parse("{\"client_id\":\"" + client + "\","
                        + "\"authorization_server\":\"" + DOMAIN + "\","
                        + "\"redirect_uri\":\"textsentry://auth/callback\"}");
                fail("Bad client ID");
            } catch (Exception expected) {}
        }
        try {
            CognitoConfig.parse("{\"client_id\":\"public\","
                    + "\"authorization_server\":\"" + DOMAIN + "\","
                    + "\"redirect_uri\":\"https://evil.com/callback\"}");
            fail("Redirect must be exact");
        } catch (Exception expected) {}
    }
}
