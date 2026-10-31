package org.textsentry.app;

import static org.junit.Assert.*;
import org.junit.Assume;
import org.junit.Test;

/**
 * Live contract test: synthetic SMS only, no carrier SMS sent.
 * Secret is injected by a trusted GitHub Actions job, never PR CI.
 */
public class SystemOneLiveTest {
    @Test public void modelClassifiesClearOptOutBetterThanPersonalConversation() throws Exception {
        String key = System.getenv("TYPESAFE_AI_API_KEY");
        Assume.assumeTrue("Requires TYPESAFE_AI_API_KEY", key != null && !key.trim().isEmpty());
        SystemOneClient client = new SystemOneClient();
        String rule = "Only opt-in promotional messages that explicitly say reply STOP to unsubscribe. Never personal messages.";
        double advertising = client.score(key, "jev-latest", rule,
                "20% off this week at Example Store! Reply STOP to unsubscribe.");
        double personal = client.score(key, "jev-latest", rule,
                "Hey! Are we still on for dinner next Thursday?");
        assertTrue("Jev should rank unambiguous opt-out higher than personal chat",
                advertising > personal);
    }
}
